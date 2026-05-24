# Деплой PveAuction на VPS 159.194.200.114

## 1. С Mac

```bash
cd server/server-plugin
./deploy-remote.sh
```

Введи пароль SSH когда спросит.

## 2. На VPS — перезапуск (обязательно)

```bash
ssh root@159.194.200.114
cd /root/server

# в консоли Minecraft (screen -r или tmux):
stop

# подожди выключения, затем:
./start.sh
```

**Не используй `/reload`** — Paper может оставить старый плагин.

## 3. Проверка в игре

1. Подключись к **159.194.200.114** (не к локальному 192.168.x.x).
2. `/shop` — в чате: **«PveAuction скупка, сборка 0.1.2»**.
3. Фермер — заголовок окна: **«Скупка [0.1.2]: Фермер»**.
4. Какао в **центре** (слот 20), не в одну длинную линию.

Если в чате нет «0.1.2» — на сервере **старый JAR** или **не PveAuction** (другой плагин перехватывает `/shop`).

## 4. Если всё ещё старое

На VPS:

```bash
cd /root/server/plugins
rm -rf .paper-remapped
ls -la PveAuction*.jar
# должен быть только PveAuction-0.1.2.jar (свежая дата)
grep -i pveauction logs/latest.log | tail -20
```

В логе при старте: `PveAuction v0.1.2 — скупка, раскладка 0.1.2`
