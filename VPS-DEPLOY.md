# Деплой на VPS 159.194.200.114

## Проблема из лога

```
PveAuction (0.1.0)
раскладка 0.1.1
```

На сервере **старый JAR**. Нужен **PveAuction-0.1.2.jar** и удаление `PveAuction-0.1.0.jar`.

`stop` — только **в консоли Minecraft** (screen), не в обычном bash.

---

## 1. С Mac — залить JAR

```bash
cd /Users/sasha_pshonko/Documents/4narek/server/server-plugin
./build.sh

scp build/libs/PveAuction-0.1.2.jar root@159.194.200.114:/root/server/plugins/
```

## 2. На VPS — удалить старое

```bash
ssh root@159.194.200.114

cd /root/server/plugins
rm -f PveAuction-0.1.0.jar PveAuction-0.1.1.jar
rm -rf .paper-remapped
ls -la PveAuction*.jar
# должен остаться только PveAuction-0.1.2.jar (свежая дата)
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

**Вариант B** — просто убить процесс:

```bash
pkill -f paper.jar
sleep 5
cd /root/server
./start.sh
```

## 4. Проверка в логе

Должно быть:

```
[PveAuction] Loading server plugin PveAuction v0.1.2
[PveAuction] PveAuction v0.1.2 — скупка, раскладка 0.1.2
```

## 5. Проверка в игре

`/shop` → в чате: **«сборка 0.1.2»**  
Фермер → заголовок: **«Скупка [0.1.2]: Фермер»**

Если в логе всё ещё **0.1.0** — `scp` не сработал или залил не тот файл.
