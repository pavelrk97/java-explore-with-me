package ru.practicum.ewm.subscription.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.subscription.dto.NewRequestSubscription;
import ru.practicum.ewm.subscription.dto.SubscriberData;
import ru.practicum.ewm.subscription.dto.SubscriptionDto;
import ru.practicum.ewm.subscription.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {

    SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto subscribe(@PathVariable Long userId,
                                     @Valid @RequestBody NewRequestSubscription requestSubscription) {
        log.info("Получен POST запрос на подписку пользователя {} на пользователя {}", userId, requestSubscription.getOwnerId());
        return subscriptionService.subscribe(userId, requestSubscription);
    }

    @DeleteMapping("/{ownerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable Long userId, @PathVariable Long ownerId) {
        log.info("Получен DELETE запрос на отписку пользователя {} от пользователя {}", userId, ownerId);
        subscriptionService.unsubscribe(userId, ownerId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getEventsFromSubscriptions(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                          @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получен GET запрос на получение событий от подписок пользователя {}", userId);
        return subscriptionService.getEventsFromSubscriptions(userId, from, size);
    }

    @GetMapping("/subscribers/count")
    public Long getSubscriberCount(@PathVariable Long userId) {
        log.info("Получен GET запрос на получение количества подписчиков пользователя {}",userId);
        return subscriptionService.getSubscriberCount(userId);
    }

    @GetMapping("/subscribers")
    public List<SubscriberData> getAllSubscribers(@PathVariable Long userId,
                                                  @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                  @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получен GET запрос на получение всех подписчиков пользователя {}", userId);
        return subscriptionService.getAllSubscribers(userId, from, size);
    }
}