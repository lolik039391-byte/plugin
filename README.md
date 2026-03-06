# BuyerPlugin (Paper 1.16.5)

Плагин скупщика с красивым GUI-меню для Paper 1.16.5.

## Что умеет
- `/buyer` открывает меню скупщика (54 слота)
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

JAR: `target/skup-plugin-1.0.0.jar` (artifactId можно менять в `pom.xml` при необходимости).
