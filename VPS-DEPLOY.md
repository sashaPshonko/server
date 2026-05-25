# Деплой на VPS 159.194.200.114

## Проблема из лога

```
PveAuction v0.1.2 — скупка, раскладка 0.1.2
```

На VPS **старый JAR** (0.1.2). Локально актуальный: **PveAuction-0.1.8.jar**.

Частая причина: в `plugins/` лежит несколько `PveAuction-*.jar` — Paper грузит не тот. Перед заливкой удалить **все** старые.

`stop` — только **в консоли Minecraft** (screen), не в обычном bash.

---

## 1. С Mac — сборка и заливка

```bash
cd /Users/sasha_pshonko/Documents/4narek/server/server-plugin
./build.sh
./deploy-remote.sh
```

Или вручную:

```bash
scp build/libs/PveAuction-0.1.8.jar root@159.194.200.114:/root/server/plugins/
```

## 2. На VPS — удалить старое

```bash
ssh root@159.194.200.114

cd /root/server/plugins
rm -f PveAuction.jar PveAuction-*.jar
rm -rf .paper-remapped
# залить свежий JAR (если ещё не scp)
ls -la PveAuction*.jar
# должен остаться только PveAuction-0.1.8.jar
```

## 3. Перезапуск сервера

**Вариант A** — сервер в screen:

```bash
screen -ls
screen -r   # или screen -r minecraft
stop
# дождись "Saving worlds" / выключения
./start.sh
# Ctrl+A D — отсоединиться
```

**Вариант B** — убить процесс:

```bash
pkill -f paper.jar
sleep 5
cd /root/server
./start.sh
```

Не используй `/reload` для плагинов — нужен полный рестарт.

## 4. Проверка в логе

Должно быть:

```
[PveAuction] Loading server plugin PveAuction v0.1.8
[PveAuction] PveAuction v0.1.8 — скупка, раскладка 0.1.8
```

## 5. Проверка в игре

`/shop` → в чате: **«сборка 0.1.8»**  
Еда → морковь в слоте **33**; рыбак → треска **20** / жареная **29**.

Если в логе всё ещё **0.1.2** — на VPS остался старый jar или не было `stop` + `./start.sh`.
