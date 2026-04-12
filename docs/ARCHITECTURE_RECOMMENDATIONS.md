# Архитектурные рекомендации

*Результаты ревью Backend Architect (agency-agents), 12.04.2026*

---

## 🔴 Критические (сделать в первую очередь)

### 1. ~~Секреты в Git~~ ✅ Исправлено
Секреты заменены на `${ENV_VAR}`, история очищена через git filter-repo.

### 2. IRC nick захардкожен
В `TwitchIrcClient.doConnect()`:
```java
send("NICK grombila");
```
**Решение:** Вынести в конфиг `twitch.irc.nick=${TWITCH_IRC_NICK}`.

### 3. H2 console включена в дефолтном профиле
```properties
spring.h2.console.enabled=true
```
**Решение:** Выключить по умолчанию, включать только в test-профиле.

---

## 🟡 Архитектурные

### 4. IrcManager — God Object
~200 строк: создание IRC-клиентов, конфигурация детекторов, скореров, viewer-трекер, управление подключениями, уведомления, метрики.

**Решение:** Разнести на:
- `ConnectionManager` — lifecycle IRC-подключений
- `DetectionPipeline` — wire детектор → скорер → сохранение
- `NotificationFacade` — отправка уведомлений

### 5. Нет Event Bus
Прямые callback-цепочки: `detector.onSpike → highlightService.addHighlight → notificationService.sendHighlight`. Если Telegram API падает — деградация всего pipeline.

**Решение:** Spring `ApplicationEventPublisher` или `CompletableFuture`-цепочка с error handling. В масштабе — Redis Streams / Kafka.

### 6. ViewerCountTracker вне Spring-контекста
Создаётся через `new`, lifecycle вручную, нет graceful shutdown, `accessToken` меняется без синхронизации.

**Решение:** Сделать Spring `@Component`, использовать `@Scheduled`, `AtomicReference<String>` для токена.

### 7. Telegram JSON собирается вручную
`sendTelegramMessage()` — string concatenation для JSON. Хрупко и чревато инъекциями.

**Решение:** Использовать Jackson `ObjectMapper` или библиотеку Telegram Bot API.

### 8. countTextEmotes — O(n*m)
Перебор всех emote-имён на каждом сообщении.

**Решение:** `HashMap<String, Pattern>` или Aho-Corasick trie.

### 9. JSON в TEXT колонках
`top_emotes` и `top_messages` — JSON-строки в TEXT.

**Решение:** PostgreSQL `jsonb` через `@JdbcTypeCode(SqlTypes.JSON)`.

### 10. Нет max-size лимита на пагинацию
Клиент может передать `size=10000`.

**Решение:** `@PageableDefault(size = 20)` + `spring.data.web.pageable.max-page-size=100`.

### 11. Нет аутентификации на API
`POST /api/channels/{channel}/connect` и `/disconnect` открыты.

**Решение:** API-key через header или Twitch OAuth.

---

## 🟡 Надёжность

### 12. Нет graceful shutdown для IRC-клиентов
`executor.shutdownNow()` без `awaitTermination`.

**Решение:** `shutdown()` + `awaitTermination(5, SECONDS)`.

### 13. SpikeDetector.baselineRate не синхронизирован
`double` читается/пишется из разных потоков.

**Решение:** `volatile` или `AtomicLong` + `Double.longBitsToDouble`.

### 14. Нет rate limiting на Twitch API calls
`ViewerCountTracker` опрашивает Twitch Helix без защиты от 429.

**Решение:** Resilience4j rate limiter или ручной backoff.

### 15. Нет retry для Telegram
Уведомление теряется при недоступности API.

**Решение:** Async queue с retry (или хотя бы логировать потерянные).

---

## 🟢 Масштабирование (на будущее)

1. **Dockerfile** для приложения (сейчас только docker-compose для БД)
2. **Вынести detection pipeline** в отдельный сервис
3. **Redis/Kafka** для horizontal scaling
4. **SSE/WebSocket push** для real-time обновлений клиентам
5. **Per-channel emotes** (сейчас только глобальные)
6. **Multi-tenant** — разделение по пользователям, авторизация через Twitch OAuth
