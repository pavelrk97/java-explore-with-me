package ru.practicum.stat.service;

import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.StatsRequestDto;
import ru.practicum.stat.ViewStatsDto;

import java.util.List;

public interface StatisticsService {
    EndpointHitDto create(EndpointHitCreateDto endpoint);

    List<ViewStatsDto> getStats(StatsRequestDto statsRequestDto);
}
