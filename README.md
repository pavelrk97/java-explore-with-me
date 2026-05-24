# Explore-Me

Платформа публикации и продвижения локальных событий: пользователи создают
мероприятия, отправляют заявки на участие, подписываются на интересных авторов.
Сервис статистики выделен отдельным приложением — копит просмотры через
клиентскую обёртку и отдаёт агрегаты.

## Стек

- **Java 21**, **Spring Boot 3.3.2** (Web, Data JPA, Validation, Actuator)
- **PostgreSQL 16.1** (профиль `postgres`), **H2** для локальной разработки (`h2`)
- **Maven** multi-module, **Lombok**
- **Docker Compose** — 2 приложения + 2 БД с healthcheck'ами и условным запуском
- **Checkstyle**, **SpotBugs**, **JaCoCo** — профили `check` и `coverage`

## Архитектура

```
┌───────────────┐         ┌──────────────────┐
│  Public API   │◄────────┤   ewm-main-svc   │
│  /events,...  │         │     :8080        │      ┌──────────────────┐
└───────────────┘         │                  │      │   stats-server   │
┌───────────────┐         │  events          │─────►│      :9090       │
│  Private API  │◄────────┤  users           │ HTTP │                  │
│  /users/{id}  │         │  categories      │      │  EndpointHit     │
└───────────────┘         │  compilations    │      │  ViewStats       │
┌───────────────┐         │  requests        │      └────────┬─────────┘
│   Admin API   │◄────────┤  subscriptions   │               │
│   /admin/...  │         └────────┬─────────┘               │
└───────────────┘                  ▼                          ▼
                            ┌────────────┐            ┌────────────┐
                            │   ewmdb    │            │   statdb   │
                            │ Postgres   │            │ Postgres   │
                            └────────────┘            └────────────┘
```

`ewm-main-svc` отправляет хит в `stats-server` на каждый запрос к Public API
через тонкий HTTP-клиент `stat-client`. DTO-контракт лежит в общем модуле
`stat-dto` и переиспользуется обеими сторонами.

## Модули

### `ewm-main-svc` (`:8080`)

Основной сервис. REST API разделён на три уровня доступа по URL-префиксам:

| Уровень | Префикс                                   | Назначение                    |
|---------|-------------------------------------------|-------------------------------|
| Public  | `/events`, `/categories`, `/compilations` | Открытый поиск и просмотр     |
| Private | `/users/{userId}/...`                     | Операции от лица пользователя |
| Admin   | `/admin/...`                              | Модерация, справочники        |

Доменные пакеты: `event`, `user`, `category`, `compilation`, `location`,
`request`, `subscription`. Глобальная обработка ошибок — `ErrorHandler` +
типизированные исключения (`NotFoundException`, `ConflictException`,
`ForbiddenException`, …).

### `stat-svc`

Сервис статистики, разбит на три Maven-модуля:

- `stat-dto` — общие DTO между клиентом и сервером
- `stat-client` — HTTP-клиент, используется из `ewm-main-svc`
- `stats-server` (`:9090`) — REST-сервер

## Запуск

### Docker Compose (рекомендуется)

```bash
docker compose up --build
```

- `ewm-service` — http://localhost:8080
- `stats-server` — http://localhost:9090
- `ewmdb` (Postgres) — `localhost:6542`
- `statdb` (Postgres) — `localhost:6541`

### Локально с H2

```bash
mvn clean install -DskipTests
cd ewm-main-svc && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Health-check

```
GET /actuator/health
```

## API

Документация автогенерируется из кода через **springdoc-openapi**.
После запуска контейнеров:

- `ewm-main-svc` — Swagger UI на `http://localhost:8080/swagger-ui.html`, OpenAPI JSON на `http://localhost:8080/v3/api-docs`
- `stats-server` — Swagger UI на `http://localhost:9090/swagger-ui.html`, OpenAPI JSON на `http://localhost:9090/v3/api-docs`

## Качество кода

```bash
mvn -P check verify        # Checkstyle + SpotBugs
mvn -P coverage verify     # + JaCoCo report
```

## Roadmap

- [x] **Liquibase** вместо `schema.sql` — версионирование схемы
- [x] **springdoc-openapi** — автогенерация OpenAPI и Swagger UI
- [x] **`@Async`** для отправки хитов в `stats-server`, чтобы не блокировать hot path
- [ ] **Spring Security + Keycloak**: OIDC, замена `userId` из URL на `@AuthenticationPrincipal`, роли `USER`/`ADMIN`
- [ ] **Тесты**: JUnit 5 + Mockito (сервисы), `@WebMvcTest` (контроллеры), Testcontainers (интеграция)
