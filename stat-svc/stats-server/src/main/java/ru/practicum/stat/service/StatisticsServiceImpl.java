package ru.practicum.stat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.*;
import ru.practicum.stat.mapper.EndpointHitMapper;
import ru.practicum.stat.mapper.ViewStatsMapper;
import ru.practicum.stat.model.EndpointHit;
import ru.practicum.stat.model.ViewStats;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final EndpointHitRepository endpointHitRepository;

    @Override
    public EndpointHitDto create(EndpointHitCreateDto endpointHitCreateDto) {
        log.info("Создание EndpointHit с данными: {}", endpointHitCreateDto);
        EndpointHit hit = EndpointHitMapper.toEndpointHitFromCreateDto(endpointHitCreateDto);
        EndpointHit createdHit = endpointHitRepository.save(hit);
        return EndpointHitMapper.toEndpointHitDto(createdHit);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewStatsDto> getStats(StatsRequestDto request) {
        log.info("Получение статистики: {}", request);

        if (request.getStart().isAfter(request.getEnd())) {
            throw new IllegalArgumentException("Дата начала периода не может быть позже даты конца периода");
        }

        List<ViewStats> viewStats;
        if (Boolean.TRUE.equals(request.getUnique())) {
            if (request.getUris() != null && !request.getUris().isEmpty()) {
                viewStats = endpointHitRepository.findStatsUniqueIp(request.getStart(), request.getEnd(), request.getUris());
            } else {
                viewStats = endpointHitRepository.findStatsUniqueIpAllUris(request.getStart(), request.getEnd());
            }
        } else {
            if (request.getUris() != null && !request.getUris().isEmpty()) {
                viewStats = endpointHitRepository.findStats(request.getStart(), request.getEnd(), request.getUris());
            } else {
                viewStats = endpointHitRepository.findStatsAllUris(request.getStart(), request.getEnd());
            }
        }
        log.info("Получена статистика: {}", viewStats);

        return viewStats != null ? viewStats.stream()
                .map(ViewStatsMapper::toViewStatsDto)
                .collect(Collectors.toList()) : List.of();
    }
}
