package com.timeyourself.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

/**
 * Applies the SQLite schema shipped as {@code db/schema.sql} while the application starts and upgrades
 * databases created by older versions by adding the {@code deleted} flag to the tables that lack it.
 *
 * The script runs twice: the first pass creates missing tables and views for a fresh installation, the second
 * pass recreates the views after they were dropped, because the views select all columns of their base tables
 * and therefore only expose a column that existed when the view was created.
 */
@Component
public class SchemaInitializer implements InitializingBean {

    /**
     * Tables that the startup migration is allowed to alter. SQLite does not accept bound parameters for
     * identifiers in DDL or {@code PRAGMA} statements, so the only identifier interpolation in this class is
     * restricted to this fixed allow-list instead of accepting arbitrary table names.
     */
    private static final Set<String> MIGRATION_TABLES = Set.of("task", "event");

    /**
     * Data source whose connection is used to execute the schema script and the migration statements.
     */
    private final DataSource dataSource;

    /**
     * Creates the initializer with the data source that provides the application database connection.
     *
     * @param dataSource data source of the application database
     */
    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Initializes the database schema on a single connection. The schema script runs first, then the
     * {@code deleted} column is added to the {@code task} and {@code event} tables when an older database does
     * not have it, both views are dropped and the script runs again so that the views are recreated with the
     * new column. Existing data is preserved because every statement is written to tolerate an existing state.
     *
     * @throws Exception when the connection cannot be obtained or a script or migration statement fails, which
     *                   aborts application startup
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/schema.sql"));

            ensureDeletedColumn(connection, "task");
            ensureDeletedColumn(connection, "event");

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP VIEW IF EXISTS v_task");
                statement.execute("DROP VIEW IF EXISTS v_event");
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/schema.sql"));
        }
    }

    /**
     * Adds the {@code deleted} column, a non-null integer defaulting to zero, to the given table unless the
     * column already exists. The table name must belong to the fixed migration allow-list because it is
     * interpolated into identifier position in DDL and {@code PRAGMA} statements.
     *
     * @param connection open database connection used to inspect and alter the table
     * @param table      table that must carry the soft-delete flag, one of the tables in
     *                   {@link #MIGRATION_TABLES}
     * @throws SQLException             when the table metadata cannot be read or the column cannot be added
     * @throws IllegalArgumentException when the table is not part of the fixed migration allow-list
     */
    private void ensureDeletedColumn(Connection connection, String table) throws SQLException {
        if (!MIGRATION_TABLES.contains(table)) {
            throw new IllegalArgumentException("Table is not part of the startup migration: " + table);
        }
        if (hasColumn(connection, table, "deleted")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
        }
    }

    /**
     * Determines whether a table already contains a column with the given name, comparing the names without
     * regard to case. An unknown table simply reports no matching column.
     *
     * @param connection open database connection used to read the table metadata
     * @param table      table whose columns are inspected
     * @param column     column name to look for
     * @return {@code true} when the table has a column with that name
     * @throws SQLException when the table information cannot be queried
     */
    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
