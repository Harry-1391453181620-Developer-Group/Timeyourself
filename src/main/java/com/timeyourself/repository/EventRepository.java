package com.timeyourself.repository;

import com.timeyourself.dto.EventDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository managing calendar events.
 *
 * Reads go through the v_event view, which derives the lifecycle state and the completion
 * flag from the stored timestamps at query time, and skip rows marked as deleted. Writes
 * target the event table directly: deletion is soft, implemented by setting the deleted flag
 * and refreshing the update timestamp, and the repository supplies every timestamp itself so
 * that stored values always share one text format.
 */
@Repository
public class EventRepository {

    /** SQL selecting the columns of every non-deleted event from the event view. */
    private static final String BASE_SELECT = """
            select v.id, v.name, v.description, v.start_at, v.end_at,
                   v.completed, v.state, v.created_at, v.updated_at
            from v_event v
            where v.deleted = 0
            """;

    /**
     * Row mapper building an EventDto from one event view row, converting the integer
     * completion flag and parsing every timestamp column through the shared helper.
     */
    private static final RowMapper<EventDto> MAPPER = (rs, rowNum) -> new EventDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            parseTime(rs.getString("start_at")),
            parseTime(rs.getString("end_at")),
            rs.getInt("completed") == 1,
            rs.getString("state"),
            parseTime(rs.getString("created_at")),
            parseTime(rs.getString("updated_at"))
    );

    /** Template used to execute every query and update of this repository. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the repository with the application JdbcTemplate.
     *
     * @param jdbcTemplate template used to execute SQL statements
     */
    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Loads every non-deleted event.
     *
     * Rows are ordered by their start timestamp and fall back to the creation timestamp for
     * events without a start, so the list reads chronologically.
     *
     * @return all non-deleted events, an empty list when none exist
     */
    public List<EventDto> findAll() {
        return jdbcTemplate.query(
                BASE_SELECT + " order by coalesce(v.start_at, v.created_at)",
                MAPPER);
    }

    /**
     * Searches non-deleted events that overlap a time window.
     *
     * The date filter is applied only when both bounds are supplied; passing null for either
     * one disables it and returns the whole collection. An event matches when it starts
     * before the exclusive upper bound, or has no start, and ends at or after the inclusive
     * lower bound, or has no end, so events spanning the window are included.
     *
     * @param from inclusive lower bound of the window, or null to disable the date filter
     * @param to exclusive upper bound of the window, or null to disable the date filter
     * @return matching events ordered by start time, falling back to creation time
     */
    public List<EventDto> search(LocalDateTime from, LocalDateTime to) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" and 1 = 1");

        if (from != null && to != null) {
            sql.append(" and (v.start_at is null or v.start_at < ?)");
            args.add(to.format(CategoryRepository.TS));
            sql.append(" and (v.end_at   is null or v.end_at   >= ?)");
            args.add(from.format(CategoryRepository.TS));
        }

        sql.append(" order by coalesce(v.start_at, v.created_at)");
        return jdbcTemplate.query(sql.toString(), MAPPER, args.toArray());
    }

    /**
     * Finds a single non-deleted event by its primary key.
     *
     * @param id database identifier of the event
     * @return the matching event, or an empty optional when no live row carries the identifier
     */
    public Optional<EventDto> findById(long id) {
        return jdbcTemplate.query(BASE_SELECT + " and v.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    /**
     * Inserts a new event and returns its generated identifier.
     *
     * The optional timestamps are stored as SQL null when absent, and the creation and update
     * timestamps are both set to the current local time so that a freshly inserted row looks
     * unmodified. The generated key is requested from the driver.
     *
     * @param name display name of the event
     * @param description description of the event, may be null
     * @param startAt planned start, or null when the event has none
     * @param endAt planned end, or null when the event has none
     * @return the generated identifier, or -1 when the driver did not report one
     */
    public long insert(String name, String description, LocalDateTime startAt, LocalDateTime endAt) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into event (name, description, start_at, end_at, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, startAt == null ? null : startAt.format(CategoryRepository.TS));
            ps.setString(4, endAt == null ? null : endAt.format(CategoryRepository.TS));
            ps.setString(5, now);
            ps.setString(6, now);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    /**
     * Updates the editable fields of a live event and refreshes its update timestamp.
     *
     * The update is restricted to rows that are not soft-deleted, so a stale identifier of a
     * deleted event changes nothing. Null timestamps clear the corresponding column.
     *
     * @param id database identifier of the event
     * @param name new display name
     * @param description new description, may be null
     * @param startAt new planned start, or null to clear it
     * @param endAt new planned end, or null to clear it
     * @return the number of rows updated, one on success and zero when the event is unknown or deleted
     */
    public int update(long id, String name, String description,
                      LocalDateTime startAt, LocalDateTime endAt) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);

        return jdbcTemplate.update("""
                update event
                   set name = ?, description = ?, start_at = ?, end_at = ?, updated_at = ?
                 where id = ? and deleted = 0
                """, ps -> {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, startAt == null ? null : startAt.format(CategoryRepository.TS));
            ps.setString(4, endAt == null ? null : endAt.format(CategoryRepository.TS));
            ps.setString(5, now);
            ps.setLong(6, id);
        });
    }

    /**
     * Soft-deletes one event by setting its deleted flag and refreshing the update timestamp.
     *
     * The row itself stays in the table so that historical data is preserved, while every read
     * of this repository stops returning it.
     *
     * @param id database identifier of the event
     * @return the number of rows updated, one on success and zero when the event is unknown or already deleted
     */
    public int deleteById(long id) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        return jdbcTemplate.update(
                "update event set deleted = 1, updated_at = ? where id = ? and deleted = 0",
                now, id);
    }

    /**
     * Soft-deletes every event whose end timestamp has already passed.
     *
     * The comparison is performed by SQLite against its local clock, which matches the format
     * used when the repository writes timestamps. Events without an end timestamp are never
     * matched, and already deleted rows are left untouched.
     *
     * @return the number of rows updated
     */
    public int deleteEnded() {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        return jdbcTemplate.update(
                "update event set deleted = 1, updated_at = ? "
                        + "where end_at is not null and end_at <= datetime('now','localtime') "
                        + "and deleted = 0",
                now);
    }

    /**
     * Parses a timestamp column read from SQLite into a local date-time.
     *
     * A stored text that does not match the shared timestamp layout makes the formatter throw
     * a DateTimeParseException, which propagates to the caller.
     *
     * @param raw stored timestamp text, or null when the column is SQL null
     * @return the parsed timestamp, or null when the input is null
     */
    private static LocalDateTime parseTime(String raw) {
        return raw == null ? null : LocalDateTime.parse(raw, CategoryRepository.TS);
    }
}
