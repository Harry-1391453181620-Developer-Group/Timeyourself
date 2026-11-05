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

@RestController
@RequestMapping("/api")
public class StatsController {

    private static final List<String> SENTENCES = loadSentences();

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

    private final StatsRepository repository;

    public StatsController(StatsRepository repository) {
        this.repository = repository;
    }

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

    private String sentenceOfTheDay() {
        int index = (int) Math.floorMod(LocalDate.now().toEpochDay(), SENTENCES.size());
        return SENTENCES.get(index);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}