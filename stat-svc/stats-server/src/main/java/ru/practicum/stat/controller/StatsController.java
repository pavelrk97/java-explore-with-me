package ru.practicum.stat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.StatsRequestDto;
import ru.practicum.stat.ViewStatsDto;
import ru.practicum.stat.service.StatisticsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final StatisticsService statisticsService;

    @PostMapping("/hit")
    public EndpointHitDto create(@RequestBody EndpointHitCreateDto endpoint) {
        log.info("POST запрос на создание нового EndpointHit ");
        return statisticsService.create(endpoint);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.info("GET запрос на получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        StatsRequestDto request = StatsRequestDto.builder()
                .start(start)
                .end(end)
                .uris(uris)
                .unique(unique)
                .build();

        return statisticsService.getStats(request);
    }
}