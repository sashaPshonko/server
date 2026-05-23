# Paper-сервер 4narek (Mac → VPS)

**Одинаково везде:** `./start.sh`, `public-host.txt` — один адрес для друзей.

## Быстрый старт

```bash
cd server
./setup.sh
./install-economy.sh
cp public-host.example.txt public-host.txt
# впиши адрес (см. ниже)
./start.sh
./address.sh
```

## Куда подключаться игрокам

**Игрокам ничего не ставить** — только адрес в Minecraft.

```bash
cat CONNECT.md    # главная инструкция
./address.sh      # адрес из public-host.txt
```

| Ситуация | Адрес |
|----------|--------|
| Та же Wi‑Fi | `192.168.0.85` |
| Из интернета | `90.151.80.16` (нужен проброс 25565 на роутере) |
| VPS | IP сервера |

Tailscale / playit игрокам **не нужны**.

- Версия: **Java 1.21.4**
- Ты на хосте: `localhost`

## VPS

Скопируй `paper.jar`, `world/`, `plugins/`, скрипты → `./vps-setup.sh` → в `public-host.txt` IP VPS.

## Плагины

```bash
cd server-plugin && ./build.sh
```

`/ah`, `/ah sell`, `/admin` — остальные команды отключены.
