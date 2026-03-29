# twitch-highlight-detector — TODO

## High Priority
- [ ] Реальное тестирование на живом стриме
- [ ] Docker образ для деплоя
- [ ] Telegram bot token — получить и настроить
- [ ] Twitch IRC token — анонимный или зарегистрировать app

## Medium Priority
- [ ] Shared state между инстансами (Redis/DB) — чтобы 2+ юзеров не слушали один канал дважды
- [ ] Канальные emotes (7TV/FFZ per-channel, не только global)
- [ ] UI/Фронтенд — список хайлайтов, подключение каналов
- [ ] Rate limiting на API endpoints

## Low Priority
- [ ] ML-модель для sentiment analysis (вместо эвристик)
- [ ] Автоматическое создание клипов (требует авторизацию стримера)
- [ ] Export хайлайтов (CSV/JSON)
- [ ] Метрики (Prometheus + Grafana)
- [ ] WebSocket push для real-time обновлений
- [ ] Multi-tenant — разделение хайлайтов по юзерам
