# Спавн (готовая карта)

## Куда класть мир

Папка мира = **`server/world/`** (на VPS: `~/server/world/`).

Внутри обязательно:
```text
world/level.dat
world/region/
```

В `server.properties` уже стоит:
```properties
level-name=world
```
Менять не нужно, если папка называется `world`.


---

## Установка карты

1. В консоли сервера: `stop`

2. Бэкап старого мира:
   ```bash
   cd ~/server
   mv world world_backup
   ```

3. Распакуй скачанную карту:
   ```bash
   unzip spawn-map.zip
   ls
   ```
   - Если видишь `level.dat` сразу → эту папку назови `world`
   - Если внутри одна папка `SpawnHub/` → `mv SpawnHub world`

4. Поставь EssentialsXSpawn (если ещё нет):
   ```bash
   ./install-economy.sh
   ```

5. Запуск:
   ```bash
   MIN_RAM=512M MAX_RAM=768M ./start.sh
   ```

6. Собери плагин и перезапусти сервер:
   ```bash
   cd server-plugin && ./build.sh
   ```

7. Отключи Essentials `/spawn` — см. `config-templates/essentials-spawn.txt`

---

## Что настроено в PveAuction

| Мир | Папка | Режим |
|-----|--------|--------|
| Спавн (карта) | `world/` | приключение, без PvP и блоков |
| RTP (выживание) | `rtp/` | обычный мир, граница 1000×1000 |

Команды:
- **`/rtp`** — случайная безопасная точка в мире RTP (1000×1000)
- **`/spawn`** — на спавн (`8 -58 8`)
- **Упал на 3 блока ниже точки спавна** (`spawn-rtp-y-offset: -3`) — автоматический RTP
- **На спавне** — без урона (падение, мобы, игроки)

Координаты в `plugins/PveAuction/config.yml`.

---

## Залить карту с Mac

```bash
# на Mac, после stop на VPS
scp -r ./папка_карты/* root@37.233.82.215:/root/server/world/
```

Перед этим на VPS: `rm -rf world` или `mv world world_backup`.

Карта для **Java 1.21.x** (1.20+ обычно тоже ок).

`world/` в git — после заливки карты: `git add world && git push`, на VPS `git pull`.
