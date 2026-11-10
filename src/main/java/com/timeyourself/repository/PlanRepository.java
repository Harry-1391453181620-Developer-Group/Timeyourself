package com.timeyourself.repository;

import com.timeyourself.model.Plan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository providing persistence operations for plans, the optional groupings of tasks.
 *
 * Reads return each plan together with the number of non-deleted tasks attached to it,
 * computed by a correlated subquery, and writes run directly against the plan table through a
 * JdbcTemplate. Descriptions are normalised to an empty string instead of null whenever a plan
 * is inserted or updated, matching the not-null column definition.
 */
@Repository
public class PlanRepository {

    /** SQL selecting every plan with the number of non-deleted tasks attached to each. */
    private static final String SELECT = """
            select p.id, p.name, p.description, p.created_at,
                   (select count(*) from task t where t.plan_id = p.id and t.deleted = 0) as task_count
            from plan p
            """;

    /**
     * Row mapper converting one plan row into a populated Plan model instance, parsing the
     * stored creation timestamp text and leaving it null when the column is SQL null.
     */
    private static final RowMapper<Plan> MAPPER = (rs, rowNum) -> {
        Plan p = new Plan();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setTaskCount(rs.getLong("task_count"));
        String createdAt = rs.getString("created_at");
        p.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt, CategoryRepository.TS));
        return p;
    };

    /** Template used to execute every query and update of this repository. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the repository with the application JdbcTemplate.
     *
     * @param jdbcTemplate template used to execute SQL statements
     */
    public PlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Loads every plan ordered by name, ignoring case.
     *
     * The task count of each returned plan is computed by a correlated subquery and already
     * excludes soft-deleted tasks.
     *
     * @return all plans ordered by name, an empty list when the table is empty
     */
    public List<Plan> findAll() {
        return jdbcTemplate.query(SELECT + " order by p.name collate nocase", MAPPER);
    }

    /**
     * Finds a single plan by its primary key.
     *
     * @param id database identifier of the plan
     * @return the matching plan, or an empty optional when no row exists
     */
    public Optional<Plan> findById(long id) {
        return jdbcTemplate.query(SELECT + " where p.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    /**
     * Checks whether a plan with the given name already exists.
     *
     * The comparison ignores case, matching the collation used when plans are listed, so names
     * that differ only in letter case are reported as duplicates.
     *
     * @param name plan name to look up
     * @return true when at least one plan carries the name, false otherwise
     */
    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from plan where name = ? collate nocase",
                Integer.class, name);
        return count != null && count > 0;
    }

    /**
     * Inserts a new plan with the supplied name and description and the current local timestamp.
     *
     * A null description is stored as an empty string so that the not-null column constraint is
     * always satisfied, and the generated key is requested from the driver.
     *
     * @param name name of the new plan
     * @param description description of the plan, null is stored as an empty string
     * @return the generated identifier, or -1 when the driver did not report one
     */
    public long insert(String name, String description) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into plan (name, description, created_at) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, description == null ? "" : description);
            ps.setString(3, LocalDateTime.now().format(CategoryRepository.TS));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    /**
     * Updates the name and description of an existing plan.
     *
     * A null description is normalised to an empty string, and because the name column is
     * unique, a name that collides with another plan under the column collation is rejected by
     * the database and surfaces as a data access exception from the template.
     *
     * @param id database identifier of the plan
     * @param name new name of the plan
     * @param description new description, null is stored as an empty string
     * @return the number of rows updated, one on success and zero when the identifier is unknown
     */
    public int update(long id, String name, String description) {
        return jdbcTemplate.update(
                "update plan set name = ?, description = ? where id = ?",
                name, description == null ? "" : description, id);
    }

    /**
     * Deletes a plan by its primary key.
     *
     * Tasks that referenced the plan are not removed with it; the foreign key relating the two
     * tables uses the ON DELETE SET NULL rule, so those tasks lose their plan assignment.
     *
     * @param id database identifier of the plan
     * @return the number of rows deleted, one on success and zero when the identifier is unknown
     */
    public int deleteById(long id) {
        return jdbcTemplate.update("delete from plan where id = ?", id);
    }

    /**
     * Deletes every plan in a single statement.
     *
     * As with the single-row deletion, tasks pointing at the removed plans are detached by the
     * foreign key rule rather than deleted.
     *
     * @return the number of deleted rows
     */
    public int deleteAll() {
        return jdbcTemplate.update("delete from plan");
    }
}
