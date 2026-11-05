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

@Repository
public class TaskRepository {

    private static final String BASE_SELECT = """
            select v.id, v.name, v.description, v.start_at, v.due_at,
                   v.completed, v.completed_at, v.status,
                   v.category_id, c.name as category_name,
                   v.plan_id, p.name as plan_name,
                   v.created_at, v.updated_at
            from v_task v
            left join category c on c.id = v.category_id
            left join plan p on p.id = v.plan_id
            """;

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

    private final JdbcTemplate jdbcTemplate;

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskDto> search(LocalDateTime from, LocalDateTime to, Long categoryId, Long planId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" where 1 = 1");
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

    public Optional<TaskDto> findById(long id) {
        return jdbcTemplate.query(BASE_SELECT + " where v.id = ?", MAPPER, id)
                .stream().findFirst();
    }

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

    public int update(long id, String name, String description,
                      LocalDateTime startAt, LocalDateTime dueAt,
                      Long categoryId, Long planId) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);

        return jdbcTemplate.update("""
                update task
                   set name = ?, description = ?, start_at = ?, due_at = ?,
                       category_id = ?, plan_id = ?, updated_at = ?
                 where id = ?
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

    public int updateCompletion(long id, boolean completed) {
        String now = LocalDateTime.now().format(CategoryRepository.TS);
        if (completed) {
            return jdbcTemplate.update(
                    "update task set completed = 1, completed_at = ?, updated_at = ? where id = ?",
                    now, now, id);
        }
        return jdbcTemplate.update(
                "update task set completed = 0, completed_at = null, updated_at = ? where id = ?",
                now, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("delete from task where id = ?", id);
    }

    public int deleteCompleted() {
        return jdbcTemplate.update("delete from task where completed = 1");
    }

    private static LocalDateTime parseTime(String raw) {
        return raw == null ? null : LocalDateTime.parse(raw, CategoryRepository.TS);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}