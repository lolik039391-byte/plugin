# BuyerPlugin (Paper 1.16.5)

Плагин скупщика с красивым GUI-меню для Paper 1.16.5.

## Что умеет
- `/buyer` открывает меню скупщика (54 слота)
- Пагинация меню для большого списка предметов (стрелки внизу)
- Продажа предметов кликом по иконке:
    - ЛКМ = 1 предмет
    - ПКМ = 16 предметов
    - Shift+ЛКМ = 64 предмета
    - Q = продать все предметы этого типа
- Подсчет персональной цены с учетом permissions и happy-hour
- Дневной лимит продаж
- Статистика игрока и топ по заработку

## Команды
- `/buyer` или `/buyer open` — открыть GUI
- `/buyer open <page>` — открыть конкретную страницу меню
- `/buyer prices` — список цен в чат
- `/buyer stats` — личная статистика
- `/buyer top` — топ-5 по заработку
- `/buyer reload` — перезагрузка (право `buyer.admin`)

## Permissions
- `buyer.admin`
- `buyer.multiplier.vip`
- `buyer.multiplier.elite`

## Сборка
```bash
mvn clean package
```

Готовый файл для сервера:
- `target/buyer-plugin.jar`

## Важно (ошибка `Jar does not contain plugin.yml`)
Если сервер пишет `Invalid plugin.yml` / `Jar does not contain plugin.yml`, значит в папку `plugins/` положен **не тот JAR** (часто IDEA-artifact вроде `plugin-main-_1_.jar`).

Используйте только JAR из `target/buyer-plugin.jar`, собранный Maven — в нём гарантированно присутствуют:
- `plugin.yml`
- `config.yml`
