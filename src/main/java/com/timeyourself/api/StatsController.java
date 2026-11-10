package com.timeyourself.api;

import com.timeyourself.dto.ReportDto;
import com.timeyourself.dto.SummaryDto;
import com.timeyourself.repository.StatsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing dashboard summary, period reports and report reset under {@code /api}.
 *
 * The summary combines task and event counters with a motivational sentence, while the report endpoint maps
 * a daily, weekly, monthly or yearly period onto a calendar range and derives completion and throughput
 * metrics from the statistics repository.
 */
@RestController
@RequestMapping("/api")
public class StatsController {

    /**
     * Immutable list of motivational sentences loaded once when the class is initialised; the sentence of the
     * day is selected from it by index.
     */
    private static final List<String> SENTENCES = loadSentences();

    /**
     * Loads the motivational sentences from the optional {@code /sentences.txt} classpath resource.
     *
     * Lines are read as UTF-8, trimmed and stripped of empty entries. The built-in fallback list is returned
     * when the resource is absent, empty or unreadable.
     *
     * @return the loaded sentences, or the fallback list when no usable resource content is available
     */
    private static List<String> loadSentences() {
        List<String> fallback = List.of(
                "Think less, do more.",
                "Done is better than perfect.",
                "Small steps every day."
        );

        try (var stream = StatsController.class.getResourceAsStream("/sentences.txt")) {
            if (stream == null) {
                return fallback;
            }

            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                List<String> lines = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .toList();
                return lines.isEmpty() ? fallback : lines;
            }
        } catch (IOException error) {
            return fallback;
        }
    }

    /**
     * Repository used to gather task, event and report statistics.
     */
    private final StatsRepository repository;

    /**
     * Creates the controller with the statistics repository it reads all figures from.
     *
     * @param repository repository providing counts and period aggregates
     */
    public StatsController(StatsRepository repository) {
        this.repository = repository;
    }

    /**
     * Builds the dashboard summary from the current task and event counters.
     *
     * The ongoing task and event names are reported only when at least one item is currently ongoing, and
     * the sentence of the day is stable for the whole calendar day.
     *
     * @return the finished and remaining task numbers, the optional ongoing task and event names, and the
     *         sentence of the day
     */
    @GetMapping("/summary")
    public SummaryDto summary() {
        StatsRepository.Counts counts = repository.counts();

        return new SummaryDto(
                counts.finishedTaskNumber(),
                counts.remainingTaskNumber(),
                repository.ongoingTaskName().orElse(null),
                repository.ongoingEventName().orElse(null),
                sentenceOfTheDay());
    }

    /**
     * Computes a report for one calendar period anchored on a given date.
     *
     * The period is mapped to a scope and the anchor defaults to today, after which the scope is resolved
     * into a half-open range. The completion rate is the share of done tasks among done, failed and not-done
     * tasks, and the average throughput is expressed per hour for a daily report and per day for the longer
     * periods, both rounded to two decimals.
     *
     * @param period period label: daily, weekly, monthly or yearly, case-insensitive; defaults to daily
     * @param anchor optional yyyy-MM-dd anchor date used to resolve the period; defaults to today
     * @return the assembled report, including the echoed period label in upper case and the unit of the
     *         average
     * @throws ResponseStatusException with status 400 when the period label or the anchor cannot be parsed
     */
    @GetMapping("/reports")
    public ReportDto report(@RequestParam(value = "period", defaultValue = "daily") String period,
                            @RequestParam(value = "anchor", required = false) String anchor) {
        TaskScope scope = scopeOf(period);
        LocalDate day = parseAnchor(anchor);
        TaskScope.Range range = scope.range(day);

        StatsRepository.ReportNumbers n = repository.report(range.from(), range.to());

        long total = n.tasksDone() + n.tasksFailed() + n.tasksNotDone();
        double completionRate = total == 0 ? 0 : round2(n.tasksDone() * 100.0 / total);

        boolean hourly = scope == TaskScope.DAY;
        double hours = Duration.between(range.from(), range.to()).toMinutes() / 60.0;
        double units = hourly ? hours : hours / 24.0;
        double average = units == 0 ? 0 : round2(n.tasksDone() / units);

        return new ReportDto(
                period.trim().toUpperCase(),
                day,
                range.from(),
                range.to(),
                n.tasksDone(),
                n.eventsPast(),
                n.tasksFailed(),
                n.tasksNotDone(),
                completionRate,
                round2(n.meanTaskHours()),
                average,
                hourly ? "hour" : "day");
    }

    /**
     * Permanently deletes every task and every event, resetting all report data.
     *
     * @return a map reporting how many task rows and how many event rows were removed
     */
    @DeleteMapping("/reports")
    public Map<String, Integer> resetReports() {
        int tasksDeleted = repository.deleteAllTasks();
        int eventsDeleted = repository.deleteAllEvents();
        return Map.of("tasksDeleted", tasksDeleted, "eventsDeleted", eventsDeleted);
    }

    /**
     * Maps a report period label to the calendar scope it represents.
     *
     * @param period period label, case-insensitive and trimmed before matching; {@code null} means daily
     * @return the scope matching the label
     * @throws ResponseStatusException with status 400 when the label is not a supported period
     */
    private TaskScope scopeOf(String period) {
        String key = period == null ? "daily" : period.trim().toLowerCase();
        return switch (key) {
            case "daily" -> TaskScope.DAY;
            case "weekly" -> TaskScope.WEEK;
            case "monthly" -> TaskScope.MONTH;
            case "yearly" -> TaskScope.YEAR;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "period must be daily, weekly, monthly or yearly");
        };
    }

    /**
     * Parses the optional report anchor date.
     *
     * @param anchor ISO date text in yyyy-MM-dd form, or {@code null} or blank to use the current date
     * @return the parsed date, or the current date when no anchor was supplied
     * @throws ResponseStatusException with status 400 when the supplied text is not a valid ISO date
     */
    private LocalDate parseAnchor(String anchor) {
        if (anchor == null || anchor.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(anchor);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anchor must be yyyy-MM-dd");
        }
    }

    /**
     * Selects a motivational sentence that remains stable for a whole calendar day.
     *
     * The current epoch day is reduced modulo the number of loaded sentences, so the index is always valid
     * and the choice rotates from one day to the next.
     *
     * @return the sentence mapped to the current day
     */
    private String sentenceOfTheDay() {
        int index = (int) Math.floorMod(LocalDate.now().toEpochDay(), SENTENCES.size());
        return SENTENCES.get(index);
    }

    /**
     * Rounds a value to two decimal places.
     *
     * @param value raw value to round
     * @return the value rounded to two decimal places
     */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
