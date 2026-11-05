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

@Repository
public class PlanRepository {

    private static final String SELECT = """
            select p.id, p.name, p.description, p.created_at,
                   (select count(*) from task t where t.plan_id = p.id) as task_count
            from plan p
            """;

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

    private final JdbcTemplate jdbcTemplate;

    public PlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Plan> findAll() {
        return jdbcTemplate.query(SELECT + " order by p.name collate nocase", MAPPER);
    }

    public Optional<Plan> findById(long id) {
        return jdbcTemplate.query(SELECT + " where p.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from plan where name = ? collate nocase",
                Integer.class, name);
        return count != null && count > 0;
    }

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

    public int update(long id, String name, String description) {
        return jdbcTemplate.update(
                "update plan set name = ?, description = ? where id = ?",
                name, description == null ? "" : description, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("delete from plan where id = ?", id);
    }

    public int deleteAll() {
        return jdbcTemplate.update("delete from plan");
    }
}