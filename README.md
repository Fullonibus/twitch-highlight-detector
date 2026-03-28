# Twitch Highlight Detector

Автоматическое определение хайлайтов стрима по активности чата Twitch.

## Концепция

Когда на стриме происходит что-то эпичное, зрители реагируют — смайлы, смех, "Pog", "LUL". Бот анализирует чат в реальном времени, детектит всплески активности и сохраняет таймстампы хайлайтов с контекстом.

## MVP Scope

- Подключение к чату любого Twitch канала через IRC
- Детекция всплесков активности (spike detection)
- Распознавание Twitch emotes + кастомных канальных
- Генерация хайлайтов с таймстампом, контекстом и score
- REST API для получения списка хайлайтов
- Telegram бот для уведомлений

## Архитектура

```
Twitch IRC (WebSocket)
        ↓
ChatListener → MessageBuffer (sliding window)
        ↓
HighlightDetector (spike + emote density)
        ↓
EventBus
        ↓
├→ NotificationService → Telegram
├→ HighlightRepository → PostgreSQL / H2
└→ REST API → GET /highlights
```

## Модули

| Модуль | Ответственность |
|--------|----------------|
| `twitch-irc` | Подключение к чату, парсинг IRC-сообщений |
| `chat-analyzer` | Буферизация, spike detection, скользящее окно |
| `emote-dictionary` | Загрузка Twitch emotes + кастомных канальных |
| `highlight-service` | Бизнес-логика детекции хайлайтов |
| `notification-service` | Отправка уведомлений в Telegram |
| `api` | REST API для фронтенда |

## Стек

- Java 17 (Corretto)
- Gradle (Kotlin DSL)
- Spring Boot 3.x
- PostgreSQL (прод) / H2 (dev)
- WebSocket (Twitch IRC)
- Spring WebFlux (реактивный чат-лисенер)

## Sprint Plan

### Sprint 1 — Core
- [ ] Инициализация проекта, структура модулей
- [ ] Twitch IRC подключение и прослушивание чата
- [ ] Парсинг IRC-сообщений (username, message, emotes)
- [ ] Базовый REST endpoint: health check

### Sprint 2 — Detection
- [ ] Sliding window buffer (10s / 30s)
- [ ] Spike detection (threshold-based)
- [ ] Emote dictionary (Twitch global + 7TV/FFZ)
- [ ] Highlight scoring (activity + emote density)

### Sprint 3 — Persistence & API
- [ ] База данных (entity, repository)
- [ ] REST API: GET /highlights, GET /highlights/{channel}
- [ ] Сохранение хайлайтов с контекстом

### Sprint 4 — Notifications
- [ ] Telegram бот
- [ ] Уведомления при детекции хайлайта
- [ ] Настройка каналов для отслеживания
