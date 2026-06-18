# twitch-highlight-detector — TODO

Обновлено после ревью/фикса от 2026-06-18. Исправленные пункты отмечены `~~[x]~~`.

## High Priority
- [ ] Реальное тестирование на живом стриме
- [ ] Docker образ для приложения (сейчас в docker-compose только PostgreSQL)
- [ ] Telegram bot token / Twitch IRC token / Twitch API credentials — получить и настроить
- [ ] **Аутентификация на API**: `POST /api/channels/{channel}/connect|disconnect` открыты. API-key header или Twitch OAuth.
- [ ] **Pagination max-size**: добавить `@PageableDefault(size=20)` + `spring.data.web.pageable.max-page-size=100`. Сейчас клиент может передать `size=10000`.
- [ ] **Разнести `IrcManager`** (~225 строк): God-Object — создаёт IRC-клиенты, конфигурит детекторы/скореры, viewer-tracker, управляет подключениями, шлёт уведомления, метрики. Разбить на:
  - `ConnectionManager` — lifecycle IRC-подключений
  - `DetectionPipeline` — wire детектор → скорер → сохранение
  - `NotificationFacade` — отправка уведомлений
- [ ] **Event Bus / async pipeline**: сейчас прямые callback-цепочки `detector.onSpike → highlightService.addHighlight → notificationService.sendHighlight`. Если Telegram API падает — деградация всего pipeline. Spring `ApplicationEventPublisher` + `@Async` с error handling; в масштабе — Redis Streams / Kafka.

## Medium Priority
- [ ] **Retry + async queue для Telegram**: уведомление теряется при недоступности API. Resilience4j retry или очередь.
- [ ] **Rate limiting / 429 backoff для Twitch Helix** в `ViewerCountTracker`: Resilience4j rate limiter или ручной backoff.
- [ ] **`ViewerCountTracker` как Spring `@Component`**: сейчас создаётся через `new`, lifecycle вручную. Вынести в бин с `@Scheduled`, `AtomicReference<String>` для токена (token sync уже исправлен).
- [ ] **Per-channel emotes** (7TV/FFZ per-channel, не только global)
- [ ] **`jsonb` колонки** для `top_emotes`/`top_messages` через `@JdbcTypeCode(SqlTypes.JSON)` вместо TEXT + ручного `JsonListConverter`.
- [ ] **SpikeDetector.maybeDebugLog** дважды зовёт `buffer.snapshot()` (2 аллокации копий) — мелкая оптимизация.
- [ ] UI/Фронтенд — список хайлайтов, подключение каналов

## Low Priority
- [ ] ML-модель для sentiment analysis (вместо эвристик)
- [ ] Автоматическое создание клипов (требует авторизацию стримера)
- [ ] Export хайлайтов (CSV/JSON)
- [ ] WebSocket/SSE push для real-time обновлений клиентам
- [ ] Multi-tenant — разделение хайлайтов по юзерам
- [ ] Shared state между инстансами (Redis/DB) — чтобы 2+ юзеров не слушали один канал дважды

## ~~Done~~ (ревью 2026-06-18, commit b1bd28d)
- ~~[x] Секреты в Git~~ — уже исправлено ранее (a29bc52)
- ~~[x] Хардкод IRC nick `grombila`~~ → `twitch.irc.nick` / `TWITCH_IRC_NICK`
- ~~[x] Хардкод `postgres/postgres` в prod-профиле~~ → env vars
- ~~[x] H2 console в дефолтном профиле~~ — выключена
- ~~[x] `SpikeDetector.baselineRate` не синхронизирован~~ — `volatile`
- ~~[x] `ViewerCountTracker.accessToken` race~~ — `AtomicReference`
- ~~[x] Нет graceful shutdown для IRC~~ — `shutdown()` + `awaitTermination`
- ~~[x] `countTextEmotes` O(n*m) + баги substring/двойного учёта~~ — один regex-проход
- ~~[x] Telegram JSON собирается вручную~~ — `ObjectMapper`
- ~~[x] 17 падающих api-тестов~~ — ComponentScan slice + placeholder defaults + mock fix
- ~~[x] Пустой `MetricsConfig`, неиспользуемое `notification.enabled-platforms`~~ — удалены
