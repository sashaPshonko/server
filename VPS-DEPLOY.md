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

## 2. На VPS — удалить старое и проверить JAR

**Сначала залей JAR с Mac, потом rm** — иначе в `plugins/` не останется ни одного файла.

```bash
ssh root@159.194.200.114

cd /root/server/plugins
ls -la PveAuction*.jar    # должен быть PveAuction-0.1.8.jar
rm -f PveAuction.jar      # только дубликаты без версии в имени
rm -rf .paper-remapped
```

Если `ls: cannot access 'PveAuction*.jar'` — на Mac снова:

```bash
scp build/libs/PveAuction-0.1.8.jar root@159.194.200.114:/root/server/plugins/
```

## 3. Где сервер и как перезапустить

`stop` и `./start.sh` **не в bash** — `stop` только в консоли Minecraft.

Найди сервер на VPS:

```bash
ls -la /root/server/
find /root -maxdepth 4 -name 'paper.jar' 2>/dev/null
find /root -maxdepth 4 -name 'start.sh' 2>/dev/null
ps aux | grep -E 'paper|minecraft' | grep -v grep
screen -ls
tmux ls 2>/dev/null
systemctl status minecraft 2>/dev/null
```

- Если есть `/root/server/paper.jar` и `/root/server/start.sh`:

```bash
cd /root/server
screen -ls
# подключись к сессии из списка, например:
screen -r 12345.minecraft
# внутри screen введи:
stop
# после выключения (в том же screen или новом ssh):
cd /root/server && ./start.sh
# Ctrl+A, затем D — выйти из screen
```

- Если `paper.jar` в другой папке — `cd` туда, где лежит `paper.jar`, и запускай `./start.sh` оттуда.

- Если сервер в **systemd**: `systemctl restart minecraft` (имя смотри в `systemctl list-units`).

- Если процесс уже крутится без screen — после заливки JAR: `systemctl restart …` или `kill` + `./start.sh` из каталога с `paper.jar`.

## 4. Перезапуск (кратко)

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

## 5. Проверка в логе

Должно быть:

```
[PveAuction] Loading server plugin PveAuction v0.1.8
[PveAuction] PveAuction v0.1.8 — скупка, раскладка 0.1.8
```

## 6. Проверка в игре

`/shop` → в чате: **«сборка 0.1.8»**  
Еда → морковь в слоте **33**; рыбак → треска **20** / жареная **29**.

Если в логе всё ещё **0.1.2** — на VPS остался старый jar или не было `stop` + `./start.sh`.
