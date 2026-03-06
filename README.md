# SkupPlugin (Paper 1.16.5)

Плагин скупщика для сервера Paper 1.16.5.

## Функции
- Продажа предмета из руки (`/skup sellhand`)
- Продажа всех подходящих предметов из инвентаря (`/skup sellall`)
- Гибкая таблица цен в `config.yml`
- Множители цен по permission
- Счастливый час с дополнительным бустом
- Дневной лимит продаж
- Персональная статистика и топ по заработку

## Команды
- `/skup sellhand`
- `/skup sellall`
- `/skup prices`
- `/skup stats`
- `/skup top`
- `/skup reload` (нужно `skup.admin`)

## Сборка
```bash
mvn clean package
```

Готовый jar будет в `target/skup-plugin-1.0.0.jar`.
