package com.timeyourself.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class StatsRepository {

    public record Counts(long finishedTaskNumber, long remainingTaskNumber) {
    }

    public record ReportNumbers(long tasksDone, long tasksFailed, long tasksNotDone,
                                double meanTaskHours, long eventsPast) {
    }

    private static final String COUNTS_SQL = """
            select
              (select count(*) from task where completed = 1) as finished_task_number,
              (select count(*) from task where completed = 0) as remaining_task_number
            """;


    private static final String ONGOING_TASK_NAME_SQL = """
            select name from v_task
             where status = 'ONGOING'
             order by (due_at is null), due_at asc, name collate nocase asc
             limit 1
            """;

    private static final String ONGOING_EVENT_NAME_SQL = """
            select name from v_event
             where state = 'ONGOING'
             order by (end_at is null), end_at asc, name collate nocase asc
             limit 1
            """;

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

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public StatsRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
    }

    public Counts counts() {
        return jdbcTemplate.queryForObject(COUNTS_SQL, (rs, rowNum) -> new Counts(
                rs.getLong("finished_task_number"),
                rs.getLong("remaining_task_number")));
    }

    public Optional<String> ongoingTaskName() {
        return jdbcTemplate.query(ONGOING_TASK_NAME_SQL, (rs, rowNum) -> rs.getString("name"))
                .stream().findFirst();
    }

    public Optional<String> ongoingEventName() {
        return jdbcTemplate.query(ONGOING_EVENT_NAME_SQL, (rs, rowNum) -> rs.getString("name"))
                .stream().findFirst();
    }

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
}