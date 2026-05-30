# Запуск и обновление на VPS (без nano)

Сервер: `root@159.194.200.114`, папка `~/server`.

---

## Один раз: подготовка на VPS

```bash
ssh root@159.194.200.114
cd ~/server
git pull
bash vps-setup.sh
```

Скрипт сам:
- удалит старые `PveAuction-*.jar` (кроме 0.1.13)
- скачает `Citizens.jar` (если нет)
- допишет `trader:` в `plugins/PveAuction/config.yml`

Если нет `PveAuction-0.1.13.jar` — залей с Mac (см. ниже).

---

## Залить плагин с Mac

```bash
scp -o StrictHostKeyChecking=accept-new \
  /Users/sasha_pshonko/Documents/4narek/server/plugins/PveAuction-0.1.13.jar \
  root@159.194.200.114:/root/server/plugins/
```

Потом на VPS снова:

```bash
cd ~/server
bash vps-setup.sh
```

---

## Перезапуск сервера

`stop` **только в консоли Minecraft**, не в обычном SSH.

Если сервер в этом же SSH-окне (`./start.sh`):

1. `Ctrl+C` или введи `stop` и дождись выключения
2. Запуск:

```bash
cd ~/server
./start.sh
```

Если сервер в **screen**:

```bash
screen -ls
screen -r
stop
cd ~/server && ./start.sh
# выйти из screen: Ctrl+A, потом D
```

**Не используй `/reload`** — только полный рестарт.

---

## Что должно быть в логе

```
Bukkit plugins (4):
 - Essentials, PveAuction (0.1.13), Vault, Citizens
[PveAuction] Торгаш (Citizens) заспавнен
```

Плохо (нет Citizens):

```
[PveAuction] Citizens не найден, используется ArmorStand fallback.
```

Плохо (битый Citizens.jar):

```
zip END header not found
Failed to open plugin jar plugins/Citizens.jar
```

Починка:

```bash
rm -f /root/server/plugins/Citizens.jar
# Paper 1.21.4 — НЕ последний Citizens 2.0.42, он отключится:
# "not compatible with your version of Minecraft"
wget -O /root/server/plugins/Citizens.jar \
  "https://ci.citizensnpcs.co/job/Citizens2/4138/artifact/dist/target/Citizens-2.0.41-b4138.jar"
file /root/server/plugins/Citizens.jar
# должно быть: Zip archive ...  (~4M)
```

В логе должно быть: `[Citizens] Enabling Citizens` **без** `Disabling... not compatible`.

Потом `stop` → `./start.sh`.

Альтернатива: обновить Paper до 1.21.6+ и тогда можно последний Citizens 2.0.42.

---

## Проверка в игре

| Действие | Ожидание |
|----------|----------|
| Умер | Респавн на спавне |
| `/spawn` | Телепорт на спавн |
| ПКМ по торгашу | Магазин отмычек |
| `/givesilver give <ник> 64` | Серебро в инвентаре |

Скин торгаша: в `plugins/PveAuction/config.yml` → `trader.skin-name: "Steve"` (любой ник Mojang).

После смены скина: `stop` → `./start.sh`.

---

## С Mac: сборка + заливка одной командой

```bash
cd /Users/sasha_pshonko/Documents/4narek/server/server-plugin
./deploy-remote.sh root@159.194.200.114
```

Потом на VPS: `bash vps-setup.sh` → `stop` → `./start.sh`.

---

## Частые косяки

| Проблема | Решение |
|---------|---------|
| Два PveAuction в логе | `rm -f plugins/PveAuction-0.1.12.jar` → рестарт |
| Нет jar на VPS | `scp` с Mac (см. выше) |
| Торгаш — стойка | Поставить `Citizens.jar`, `bash vps-setup.sh`, рестарт |
| `stop` в bash не работает | `stop` только в консоли сервера Minecraft |
