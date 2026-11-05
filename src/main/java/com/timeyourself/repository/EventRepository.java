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

@Repository
public class EventRepository {

    private static final String BASE_SELECT = """
            select v.id, v.name, v.description, v.start_at, v.end_at,
                   v.completed, v.state, v.created_at, v.updated_at
            from v_event v
            """;

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

    private final JdbcTemplate jdbcTemplate;

    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EventDto> findAll() {
        return jdbcTemplate.query(
                BASE_SELECT + " order by coalesce(v.start_at, v.created_at)",
                MAPPER);
    }

    public List<EventDto> search(LocalDateTime from, LocalDateTime to) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" where 1 = 1");

        if (from != null && to != null) {
            sql.append(" and (v.start_at is null or v.start_at < ?)");
            args.add(to.format(CategoryRepository.TS));
            sql.append(" and (v.end_at   is null or v.end_at   >= ?)");
            args.add(from.format(CategoryRepository.TS));
        }

        sql.append(" order by coalesce(v.start_at, v.created_at)");
        return jdbcTemplate.query(sql.toString(), MAPPER, args.toArray());
    }

    public Optional<EventDto> findById(long id) {
        return jdbcTemplate.query(BASE_SELECT + " where v.id = ?", MAPPER, id)
                .stream().findFirst();
    }

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

    public int update(long id, String name, String description,
                      LocalDateTime startAt, LocalDateTime endAt) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);

        return jdbcTemplate.update("""
                update event
                   set name = ?, description = ?, start_at = ?, end_at = ?, updated_at = ?
                 where id = ?
                """, ps -> {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, startAt == null ? null : startAt.format(CategoryRepository.TS));
            ps.setString(4, endAt == null ? null : endAt.format(CategoryRepository.TS));
            ps.setString(5, now);
            ps.setLong(6, id);
        });
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("delete from event where id = ?", id);
    }

    public int deleteEnded() {
        return jdbcTemplate.update(
                "delete from event where end_at is not null "
                        + "and end_at <= datetime('now','localtime')");
    }

    private static LocalDateTime parseTime(String raw) {
        return raw == null ? null : LocalDateTime.parse(raw, CategoryRepository.TS);
    }
}