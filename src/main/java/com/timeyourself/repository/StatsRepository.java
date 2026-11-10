package com.timeyourself.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC repository exposing the aggregate queries behind the summary and report endpoints.
 *
 * The class holds the read-only aggregation SQL, which computes its values in the database so
 * that no rows are loaded into memory, plus the two bulk deletion statements used by the report
 * reset endpoint. Both a plain JdbcTemplate and a named-parameter template are injected; the
 * latter binds the two window bounds of the report query by name once, so the statement can
 * reuse them through a common table expression.
 */
@Repository
public class StatsRepository {

    /**
     * Counts of completed and still open tasks shown in the application summary.
     *
     * @param finishedTaskNumber number of non-deleted tasks marked as completed
     * @param remainingTaskNumber number of non-deleted tasks not marked as completed
     */
    public record Counts(long finishedTaskNumber, long remainingTaskNumber) {
    }

    /**
     * Raw aggregate values computed for one reporting window.
     *
     * @param tasksDone number of tasks completed inside the window
     * @param tasksFailed number of open tasks whose due date passed inside the window
     * @param tasksNotDone number of open tasks scheduled inside the window whose due date has not passed
     * @param meanTaskHours mean hours between start and completion of the completed tasks in the window
     * @param eventsPast number of events that ended inside the window and are already over
     */
    public record ReportNumbers(long tasksDone, long tasksFailed, long tasksNotDone,
                                double meanTaskHours, long eventsPast) {
    }

    /** SQL computing the global completed and remaining task counters as two scalar subqueries. */
    private static final String COUNTS_SQL = """
            select
              (select count(*) from task where completed = 1 and deleted = 0) as finished_task_number,
              (select count(*) from task where completed = 0 and deleted = 0) as remaining_task_number
            """;


    /**
     * SQL selecting the name of the most urgent ongoing task, preferring rows with a due date
     * and ordering by the earliest due date and then by name ignoring case.
     */
    private static final String ONGOING_TASK_NAME_SQL = """
            select name from v_task
             where status = 'ONGOING' and deleted = 0
             order by (due_at is null), due_at asc, name collate nocase asc
             limit 1
            """;

    /**
     * SQL selecting the name of the ongoing event that ends first, preferring rows with an end
     * timestamp and ordering by the earliest end and then by name ignoring case.
     */
    private static final String ONGOING_EVENT_NAME_SQL = """
            select name from v_event
             where state = 'ONGOING' and deleted = 0
             order by (end_at is null), end_at asc, name collate nocase asc
             limit 1
            """;

    /**
     * Named-parameter SQL computing all report aggregates for the half-open window bound to the
     * from and to parameters, using SQLite local time to decide which due dates have passed.
     */
    private static final String REPORT_SQL = """
            with p as (select :from as f, :to as t, datetime('now','localtime') as n)
            select
              coalesce(sum(case when tk.completed = 1 and tk.completed_at >= p.f and tk.completed_at < p.t
                                then 1 else 0 end), 0) as tasks_done,
              coalesce(sum(case when tk.completed = 0 and tk.due_at is not null and tk.due_at < p.n
                                 and tk.due_at >= p.f and tk.due_at < p.t
                                then 1 else 0 end), 0) as tasks_failed,
              coalesce(sum(case when tk.completed = 0 and (tk.due_at is null or tk.due_at >= p.n)
                                 and ((tk.start_at >= p.f and tk.start_at < p.t)
                                      or (tk.start_at is null and tk.created_at >= p.f and tk.created_at < p.t))
                                then 1 else 0 end), 0) as tasks_not_done,
              coalesce(avg(case when tk.completed = 1 and tk.start_at is not null and tk.completed_at is not null
                                then (julianday(tk.completed_at) - julianday(tk.start_at)) * 24.0 end), 0) as mean_task_hours,
              (select count(*) from event e
                where e.end_at >= p.f and e.end_at < p.t and e.end_at < p.n) as events_past
            from task tk, p
            """;

    /** Template used to run the statements of this repository that take no named parameters. */
    private final JdbcTemplate jdbcTemplate;

    /** Named-parameter template used to run the report query with its window bounds bound by name. */
    private final NamedParameterJdbcTemplate namedJdbc;

    /**
     * Creates the repository with the templates it needs to execute its statements.
     *
     * @param jdbcTemplate template used to execute the plain SQL statements
     * @param namedJdbc named-parameter template used to execute the report query
     */
    public StatsRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
    }

    /**
     * Reads the global completed and remaining task counters.
     *
     * Both counters ignore soft-deleted tasks and are produced by scalar subqueries, so the
     * query always returns exactly one row.
     *
     * @return the current task counters
     */
    public Counts counts() {
        return jdbcTemplate.queryForObject(COUNTS_SQL, (rs, rowNum) -> new Counts(
                rs.getLong("finished_task_number"),
                rs.getLong("remaining_task_number")));
    }

    /**
     * Finds the name of the most urgent task whose derived status is ONGOING.
     *
     * The task view derives the status from the completion flag and the due date, so a task
     * with a passed deadline is not considered ongoing. Among the remaining candidates the
     * query prefers tasks that have a due date and then the earliest one, with the name used
     * as a deterministic tie-breaker.
     *
     * @return the selected task name, or an empty optional when no ongoing task exists
     */
    public Optional<String> ongoingTaskName() {
        return jdbcTemplate.query(ONGOING_TASK_NAME_SQL, (rs, rowNum) -> rs.getString("name"))
                .stream().findFirst();
    }

    /**
     * Finds the name of the ongoing event that ends first.
     *
     * The event view derives the state from the stored start and end timestamps, so only
     * events that have started and not yet ended qualify. Among them the query prefers events
     * that have an end timestamp and then the earliest one, with the name used as a
     * deterministic tie-breaker.
     *
     * @return the selected event name, or an empty optional when no ongoing event exists
     */
    public Optional<String> ongoingEventName() {
        return jdbcTemplate.query(ONGOING_EVENT_NAME_SQL, (rs, rowNum) -> rs.getString("name"))
                .stream().findFirst();
    }

    /**
     * Computes the aggregate numbers for a half-open time window.
     *
     * Both bounds are required and are bound as text in the shared timestamp format, which is
     * the form SQLite compares correctly. The window includes its start and excludes its end,
     * matching the ranges built by the API layer, and tasks whose start timestamp is missing
     * fall back to their creation timestamp when deciding whether they belong to it.
     *
     * @param from inclusive start of the window
     * @param to exclusive end of the window
     * @return the aggregate values for the window
     */
    public ReportNumbers report(LocalDateTime from, LocalDateTime to) {
        var params = new MapSqlParameterSource()
                .addValue("from", from.format(CategoryRepository.TS))
                .addValue("to", to.format(CategoryRepository.TS));

        return namedJdbc.queryForObject(REPORT_SQL, params, (rs, rowNum) -> new ReportNumbers(
                rs.getLong("tasks_done"),
                rs.getLong("tasks_failed"),
                rs.getLong("tasks_not_done"),
                rs.getDouble("mean_task_hours"),
                rs.getLong("events_past")));
    }

    /**
     * Permanently removes every task row, including the soft-deleted ones.
     *
     * This backs the report reset endpoint, so the removal is physical and the rows cannot be
     * restored afterwards.
     *
     * @return the number of deleted rows
     */
    public int deleteAllTasks() {
        return jdbcTemplate.update("delete from task");
    }

    /**
     * Permanently removes every event row, including the soft-deleted ones.
     *
     * This backs the report reset endpoint, so the removal is physical and the rows cannot be
     * restored afterwards.
     *
     * @return the number of deleted rows
     */
    public int deleteAllEvents() {
        return jdbcTemplate.update("delete from event");
    }
}
