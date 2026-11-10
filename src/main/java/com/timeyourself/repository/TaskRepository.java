package com.timeyourself.repository;

import com.timeyourself.dto.TaskDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistence gateway for tasks, backed by Spring JDBC and the SQLite {@code task} table.
 *
 * Reads run against the {@code v_task} view, which joins the category and plan names and hides rows that
 * were flagged as deleted.
 *
 * Every timestamp is stored and parsed with {@link CategoryRepository#TS}, and deletes are soft: affected
 * rows are flagged instead of being physically removed.
 */
@Repository
public class TaskRepository {

    /**
     * Base select that reads every task column plus the joined category and plan names and restricts the
     * result to rows that are not soft-deleted; callers append filters and ordering before execution.
     */
    private static final String BASE_SELECT = """
            select v.id, v.name, v.description, v.start_at, v.due_at,
                   v.completed, v.completed_at, v.status,
                   v.category_id, c.name as category_name,
                   v.plan_id, p.name as plan_name,
                   v.created_at, v.updated_at
            from v_task v
            left join category c on c.id = v.category_id
            left join plan p on p.id = v.plan_id
            where v.deleted = 0
            """;

    /**
     * Row mapper that materialises one {@link TaskDto} from a joined result-set row, parsing the timestamp
     * columns with {@link CategoryRepository#TS} and turning absent foreign keys into {@code null} values.
     */
    private static final RowMapper<TaskDto> MAPPER = (rs, rowNum) -> new TaskDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            parseTime(rs.getString("start_at")),
            parseTime(rs.getString("due_at")),
            rs.getInt("completed") == 1,
            parseTime(rs.getString("completed_at")),
            rs.getString("status"),
            nullableLong(rs, "category_id"),
            rs.getString("category_name"),
            nullableLong(rs, "plan_id"),
            rs.getString("plan_name"),
            parseTime(rs.getString("created_at")),
            parseTime(rs.getString("updated_at"))
    );

    /**
     * JDBC template used to execute every statement issued by this repository.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the repository with the JDBC template that provides all database access.
     *
     * @param jdbcTemplate template bound to the application's SQLite data source
     */
    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Searches non-deleted tasks, optionally restricted to a half-open time window, a category and a plan.
     *
     * The time filter is applied only when both bounds are supplied, and a task qualifies when its start
     * time is null or earlier than {@code to} and its due time is null or not earlier than {@code from}, so
     * tasks without scheduling metadata stay visible.
     *
     * Supplied filters are combined with AND, and the result is ordered by the earliest of start time, due
     * time and creation time.
     *
     * @param from inclusive lower bound of the time window; ignored when {@code to} is {@code null}
     * @param to exclusive upper bound of the time window; ignored when {@code from} is {@code null}
     * @param categoryId optional category identifier filter
     * @param planId optional plan identifier filter
     * @return the matching tasks in effective start order; empty when nothing matches, never {@code null}
     */
    public List<TaskDto> search(LocalDateTime from, LocalDateTime to, Long categoryId, Long planId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" and 1 = 1");
        List<Object> args = new ArrayList<>();

        if (from != null && to != null) {
            sql.append(" and (v.start_at is null or v.start_at < ?)");
            args.add(to.format(CategoryRepository.TS));
            sql.append(" and (v.due_at   is null or v.due_at   >= ?)");
            args.add(from.format(CategoryRepository.TS));
        }
        if (categoryId != null) {
            sql.append(" and v.category_id = ?");
            args.add(categoryId);
        }
        if (planId != null) {
            sql.append(" and v.plan_id = ?");
            args.add(planId);
        }

        sql.append(" order by coalesce(v.start_at, v.due_at, v.created_at)");
        return jdbcTemplate.query(sql.toString(), MAPPER, args.toArray());
    }

    /**
     * Finds one task by its identifier, ignoring rows that were soft-deleted.
     *
     * @param id identifier of the task to load
     * @return the task when it exists and is not soft-deleted, otherwise an empty optional
     */
    public Optional<TaskDto> findById(long id) {
        return jdbcTemplate.query(BASE_SELECT + " and v.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    /**
     * Inserts a task in the not-completed state and reports the generated identifier.
     *
     * The creation and update timestamps are both set to the current local time, the completion timestamp
     * stays null, and a missing category or plan identifier is written as SQL null.
     *
     * @param name task name, already normalised by the caller
     * @param description task description, already normalised by the caller
     * @param startAt optional start time
     * @param dueAt optional due time
     * @param categoryId optional owning category identifier
     * @param planId optional owning plan identifier
     * @return the generated identifier, or {@code -1} when the driver does not report one
     */
    public long insert(String name, String description,
                       LocalDateTime startAt, LocalDateTime dueAt,
                       Long categoryId, Long planId) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into task (name, description, start_at, due_at, completed,
                                      completed_at, category_id, plan_id, created_at, updated_at)
                    values (?, ?, ?, ?, 0, null, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, startAt == null ? null : startAt.format(CategoryRepository.TS));
            ps.setString(4, dueAt == null ? null : dueAt.format(CategoryRepository.TS));
            if (categoryId == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setLong(5, categoryId);
            }
            if (planId == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setLong(6, planId);
            }
            ps.setString(7, now);
            ps.setString(8, now);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    /**
     * Replaces the editable fields of an existing non-deleted task and refreshes its update timestamp.
     *
     * The completion state and the creation timestamp are left untouched, and a missing category or plan
     * identifier clears the previous association.
     *
     * @param id identifier of the task to update
     * @param name new task name, already normalised by the caller
     * @param description new task description, already normalised by the caller
     * @param startAt optional new start time
     * @param dueAt optional new due time
     * @param categoryId optional new owning category identifier
     * @param planId optional new owning plan identifier
     * @return the number of affected rows, zero when the task is missing or soft-deleted
     */
    public int update(long id, String name, String description,
                      LocalDateTime startAt, LocalDateTime dueAt,
                      Long categoryId, Long planId) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);

        return jdbcTemplate.update("""
                update task
                   set name = ?, description = ?, start_at = ?, due_at = ?,
                       category_id = ?, plan_id = ?, updated_at = ?
                 where id = ? and deleted = 0
                """, ps -> {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, startAt == null ? null : startAt.format(CategoryRepository.TS));
            ps.setString(4, dueAt == null ? null : dueAt.format(CategoryRepository.TS));
            if (categoryId == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setLong(5, categoryId);
            }
            if (planId == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setLong(6, planId);
            }
            ps.setString(7, now);
            ps.setLong(8, id);
        });
    }

    /**
     * Marks a task as completed or not completed and records the matching completion timestamp.
     *
     * Completion sets the completion timestamp to the current local time, while reopening clears it to
     * SQL null; the update timestamp is refreshed in both directions.
     *
     * @param id identifier of the task to update
     * @param completed desired completion flag
     * @return the number of affected rows, zero when the task is missing or already soft-deleted
     */
    public int updateCompletion(long id, boolean completed) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        if (completed) {
            return jdbcTemplate.update(
                    "update task set completed = 1, completed_at = ?, updated_at = ? "
                            + "where id = ? and deleted = 0",
                    now, now, id);
        }
        return jdbcTemplate.update(
                "update task set completed = 0, completed_at = null, updated_at = ? "
                        + "where id = ? and deleted = 0",
                now, id);
    }

    /**
     * Soft-deletes a single task by flagging it as deleted and refreshing its update timestamp.
     *
     * @param id identifier of the task to delete
     * @return the number of affected rows, zero when the task is missing or already soft-deleted
     */
    public int deleteById(long id) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        return jdbcTemplate.update(
                "update task set deleted = 1, updated_at = ? where id = ? and deleted = 0",
                now, id);
    }

    /**
     * Soft-deletes every finished task in a single statement.
     *
     * @return the number of tasks that were flagged as deleted
     */
    public int deleteCompleted() {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        return jdbcTemplate.update(
                "update task set deleted = 1, updated_at = ? where completed = 1 and deleted = 0",
                now);
    }

    /**
     * Parses a stored timestamp with the shared {@link CategoryRepository#TS} layout.
     *
     * @param raw timestamp text as stored in SQLite, possibly {@code null}
     * @return the parsed timestamp, or {@code null} when the input is {@code null}
     */
    private static LocalDateTime parseTime(String raw) {
        return raw == null ? null : LocalDateTime.parse(raw, CategoryRepository.TS);
    }

    /**
     * Reads a nullable long column and translates the SQL null marker into a Java {@code null}.
     *
     * @param rs result set positioned at the row being mapped
     * @param column name of the column to read
     * @return the column value, or {@code null} when the column holds SQL null
     * @throws SQLException when the column cannot be read
     */
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
