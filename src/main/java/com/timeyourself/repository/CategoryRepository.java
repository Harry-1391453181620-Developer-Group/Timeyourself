package com.timeyourself.repository;

import com.timeyourself.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository providing persistence operations for task categories.
 *
 * Reads return each category together with the number of non-deleted tasks attached to it,
 * computed by a correlated subquery, and writes run directly against the category table
 * through a JdbcTemplate. Every call issues a single statement and relies on the template
 * for connection handling and exception translation.
 */
@Repository
public class CategoryRepository {

    /**
     * Formatter used to write and parse the text timestamps stored by SQLite, shared with the
     * other repositories so that all persisted date-time values use the same layout.
     */
    public static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** SQL selecting every category with the number of non-deleted tasks attached to each. */
    private static final String SELECT = """
            select c.id, c.name, c.created_at,
                   (select count(*) from task t where t.category_id = c.id and t.deleted = 0) as task_count
            from category c
            """;

    /**
     * Row mapper converting one category row into a populated Category model instance, parsing
     * the stored creation timestamp text and leaving it null when the column is SQL null.
     */
    private static final RowMapper<Category> MAPPER = (rs, rowNum) -> {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setTaskCount(rs.getLong("task_count"));
        String createdAt = rs.getString("created_at");
        c.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt, TS));
        return c;
    };

    /** Template used to execute every query and update of this repository. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the repository with the application JdbcTemplate.
     *
     * @param jdbcTemplate template used to execute SQL statements
     */
    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Loads every category ordered by name, ignoring case.
     *
     * The task count of each returned category is computed by a correlated subquery and
     * already excludes soft-deleted tasks.
     *
     * @return all categories ordered by name, an empty list when the table is empty
     */
    public List<Category> findAll() {
        return jdbcTemplate.query(SELECT + " order by c.name collate nocase", MAPPER);
    }

    /**
     * Finds a single category by its primary key.
     *
     * @param id database identifier of the category
     * @return the matching category, or an empty optional when no row exists
     */
    public Optional<Category> findById(long id) {
        return jdbcTemplate.query(SELECT + " where c.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    /**
     * Checks whether a category with the given name already exists.
     *
     * The comparison ignores case, matching the collation used when categories are listed,
     * so names that differ only in letter case are reported as duplicates.
     *
     * @param name category name to look up
     * @return true when at least one category carries the name, false otherwise
     */
    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from category where name = ? collate nocase",
                Integer.class, name);
        return count != null && count > 0;
    }

    /**
     * Inserts a new category with the supplied name and the current local timestamp.
     *
     * The generated key is requested from the driver so that the caller can address the new
     * row immediately; uniqueness of the name is enforced by the database.
     *
     * @param name name of the new category
     * @return the generated identifier, or -1 when the driver did not report one
     */
    public long insert(String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into category (name, created_at) values (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, LocalDateTime.now().format(TS));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    /**
     * Renames an existing category.
     *
     * The name is stored exactly as supplied and is unique in the table, so a rename that
     * collides with another row under the column collation is rejected by the database and
     * surfaces as a data access exception from the template.
     *
     * @param id database identifier of the category
     * @param name new name of the category
     * @return the number of rows updated, one on success and zero when the identifier is unknown
     */
    public int rename(long id, String name) {
        return jdbcTemplate.update("update category set name = ? where id = ?", name, id);
    }

    /**
     * Deletes a category by its primary key.
     *
     * Tasks that referenced the category are not removed with it; the foreign key relating
     * the two tables uses the ON DELETE SET NULL rule, so those tasks become uncategorised.
     *
     * @param id database identifier of the category
     * @return the number of rows deleted, one on success and zero when the identifier is unknown
     */
    public int deleteById(long id) {
        return jdbcTemplate.update("delete from category where id = ?", id);
    }

    /**
     * Deletes every category in a single statement.
     *
     * As with the single-row deletion, tasks pointing at the removed categories are detached
     * by the foreign key rule rather than deleted.
     *
     * @return the number of deleted rows
     */
    public int deleteAll() {
        return jdbcTemplate.update("delete from category");
    }
}
