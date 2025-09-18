package ru.practicum.ewm.subscription.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.enums.FriendshipsStatus;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.subscription.SubscriptionRepository;
import ru.practicum.ewm.subscription.dto.NewRequestSubscription;
import ru.practicum.ewm.subscription.dto.SubscriberData;
import ru.practicum.ewm.subscription.dto.SubscriptionDto;
import ru.practicum.ewm.subscription.mapper.SubscriptionMapper;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService {
    UserRepository userRepository;
    SubscriptionRepository subscriptionRepository;
    EventRepository eventRepository;

    @Override
    @Transactional
    public SubscriptionDto subscribe(Long userId, NewRequestSubscription requestSubscription) {
        User follower = findUser(userId);
        User owner = findUser(requestSubscription.getOwnerId());

        validateSubscription(follower, owner);

        Subscription subscription = createOrUpdateSubscription(follower, owner, requestSubscription);
        log.info("Подписка успешно создана для пользователя {} на пользователя {}. Статус дружбы: {}",
                userId, requestSubscription.getOwnerId(), subscription.getFriendshipsStatus());
        return SubscriptionMapper.toSubscriptionDtoWithoutUnsubscribeTime(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(Long userId, Long ownerId) {
        User follower = findUser(userId);
        User owner = findUser(ownerId);
        log.info("Обработка запроса на отмену подписки от пользователя {} к пользователю {}", userId, ownerId);

        Optional<Subscription> subscription = subscriptionRepository
                .findByFollowerAndOwner(follower, owner);
        if (subscription.isEmpty()) {
            log.warn("У пользователя {} нет подписки на пользователя {}", userId, ownerId);
            throw new ConflictException("У пользователя нет подписки на пользователя");
        }
        Optional<Subscription> reverseSubscription = subscriptionRepository
                .findByFollowerAndOwner(owner, follower);
        if (reverseSubscription.isPresent() &&
                reverseSubscription.get().getFriendshipsStatus().equals(FriendshipsStatus.MUTUAL)) {
            reverseSubscription.get().setFriendshipsStatus(FriendshipsStatus.ONE_SIDED);
            reverseSubscription.get().setUnsubscribeTime(LocalDateTime.now());
            subscriptionRepository.save(reverseSubscription.get());
        }
        subscriptionRepository.delete(subscription.get());
        log.info("Пользователь {} успешно отписался от {}. Статус дружбы обновлен: {}",
                userId, ownerId, reverseSubscription.map(Subscription::getFriendshipsStatus).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsFromSubscriptions(Long userId, int from, int size) {
        User follower = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        List<Subscription> subscriptions = subscriptionRepository.findByFollower(follower);

        List<Long> ownerIds = subscriptions.stream()
                .map(subscription -> subscription.getOwner().getId())
                .collect(Collectors.toList());

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by("eventDate").descending());
        List<Event> events = eventRepository.findByInitiatorIdIn(ownerIds, pageRequest);

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getSubscriberCount(Long userId) {
        log.info("Получение количества подписчиков для пользователя с ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        long count = subscriptionRepository.countByOwnerAndFriendshipsStatusIn(user, List.of(FriendshipsStatus.ONE_SIDED, FriendshipsStatus.MUTUAL));
        log.info("У пользователя {} {} подписчиков.", userId, count);
        return count;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SubscriberData> getAllSubscribers(Long userId, int from, int size) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Pageable pageable = PageRequest.of(from / size, size);
        List<Subscription> subscriptions = subscriptionRepository.findByOwner(owner, pageable);
        return subscriptions.stream()
                .map(SubscriptionMapper::toSubscriberData)
                .collect(Collectors.toList());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
    }

    private void validateSubscription(User follower, User owner) {
        if (!follower.isAllowSubscriptions()) {
            log.warn("Пользователь {} не разрешает подписки", follower.getId());
            throw new ConflictException("Пользователь не разрешает подписки");
        }

        if (follower.getId().equals(owner.getId())) {
            log.warn("Пользователь {} не может подписаться на самого себя", follower.getId());
            throw new ConflictException("Пользователь не может подписаться на самого себя");
        }

        Optional<Subscription> existingSubscription = subscriptionRepository.findByFollowerAndOwner(follower, owner);
        if (existingSubscription.isPresent()) {
            Subscription sub = existingSubscription.get();
            if (sub.getFriendshipsStatus() == FriendshipsStatus.ONE_SIDED || sub.getFriendshipsStatus() == FriendshipsStatus.MUTUAL) {
                log.warn("У пользователя {} уже есть подписка на пользователя {}", follower.getId(), owner.getId());
                throw new ConflictException("У пользователя уже есть подписка на пользователя");
            }
        }
    }

    private Subscription createOrUpdateSubscription(User follower, User owner, NewRequestSubscription requestSubscription) {
        Subscription subscription = SubscriptionMapper.toNewSubscriptionFromRequest(follower, requestSubscription, owner);
        Optional<Subscription> existingReverseSubscription = subscriptionRepository.findByFollowerAndOwner(owner, follower);

        if (existingReverseSubscription.isPresent()) {
            subscription.setFriendshipsStatus(FriendshipsStatus.MUTUAL);
            subscription.setSubscribeTime(LocalDateTime.now());

            existingReverseSubscription.get().setFriendshipsStatus(FriendshipsStatus.MUTUAL);
            existingReverseSubscription.get().setSubscribeTime(LocalDateTime.now());
            subscriptionRepository.saveAll(List.of(subscription, existingReverseSubscription.get()));
        } else {
            subscription.setFriendshipsStatus(FriendshipsStatus.ONE_SIDED);
            subscription.setSubscribeTime(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        }
        return subscription;
    }
}