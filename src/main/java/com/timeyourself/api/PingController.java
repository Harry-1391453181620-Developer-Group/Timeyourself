package com.timeyourself.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health probe controller that reports application liveness together with the embedded database version.
 *
 * The endpoint exists so that clients and deployment checks can confirm that the HTTP layer is up and that
 * a JDBC connection to SQLite can be obtained.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    /**
     * JDBC template used to query the SQLite version as part of the health response.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the controller with the JDBC template used for the database probe.
     *
     * @param jdbcTemplate template bound to the application's SQLite data source
     */
    public PingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Answers the health probe with a fixed status and the version of the connected SQLite engine.
     *
     * A failure to reach the database propagates as a server error rather than a partial response, so the
     * payload is only produced when the query succeeds.
     *
     * @return a map containing the literal {@code status} value {@code ok} and the {@code sqlite} version
     *         returned by the database
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "sqlite", jdbcTemplate.queryForObject("select sqlite_version()", String.class)
        );
    }
}
