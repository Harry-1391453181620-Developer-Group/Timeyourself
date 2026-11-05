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

@Repository
public class CategoryRepository {

    public static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SELECT = """
            select c.id, c.name, c.created_at,
                   (select count(*) from task t where t.category_id = c.id) as task_count
            from category c
            """;

    private static final RowMapper<Category> MAPPER = (rs, rowNum) -> {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setTaskCount(rs.getLong("task_count"));
        String createdAt = rs.getString("created_at");
        c.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt, TS));
        return c;
    };

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAll() {
        return jdbcTemplate.query(SELECT + " order by c.name collate nocase", MAPPER);
    }

    public Optional<Category> findById(long id) {
        return jdbcTemplate.query(SELECT + " where c.id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from category where name = ? collate nocase",
                Integer.class, name);
        return count != null && count > 0;
    }

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

    public int rename(long id, String name) {
        return jdbcTemplate.update("update category set name = ? where id = ?", name, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("delete from category where id = ?", id);
    }

    public int deleteAll() {
        return jdbcTemplate.update("delete from category");
    }
}