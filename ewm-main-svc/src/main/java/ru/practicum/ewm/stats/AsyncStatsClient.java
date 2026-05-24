package ru.practicum.ewm.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.StatisticsClient;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AsyncStatsClient {

    private final StatisticsClient delegate;
    private final String appName;

    public AsyncStatsClient(StatisticsClient delegate,
                            @Value("${app.name}") String appName) {
        this.delegate = delegate;
        this.appName = appName;
    }

    @Async("statsExecutor")
    public void sendHit(String uri, String ip) {
        try {
            EndpointHitCreateDto dto = EndpointHitCreateDto.builder()
                    .app(appName)
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build();
            delegate.create(dto);
        } catch (Exception e) {
            log.warn("Не удалось отправить хит в stats-server: {}", e.getMessage());
        }
    }
}
