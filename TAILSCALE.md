# Бесплатно: Tailscale (как локальная сеть через интернет)

Работает Mac ↔ Mac, без playit и без проброса портов. Для друзей — тот же принцип, что «один адрес на VPS», только IP вида `100.x.x.x`.

## 1. Ты (хост с сервером)

1. https://tailscale.com/download → **macOS**, установи.
2. Войди (Google / email, бесплатно).
3. Включи Tailscale (иконка в меню → Connect).
4. Узнай свой IP:
   ```bash
   tailscale ip -4
   ```
   Будет что-то вроде `100.64.12.34`.

5. В `public-host.txt` впиши **этот IP** (одна строка).

6. Запусти сервер:
   ```bash
   ./start.sh
   ```

## 2. Друг

1. Тот же сайт → Tailscale на **свой Mac**.
2. Войти **в твой tailnet**:
   - Ты в админке https://login.tailscale.com/admin/settings/users → **Invite user** → ссылка другу  
   - или общий аккаунт (не обязательно).
3. Minecraft → **Прямое подключение** → IP из твоего `public-host.txt` (`100.x.x.x`).
4. Версия **Java 1.21.4**.

## 3. Проверка

На Mac друга:
```bash
ping 100.x.x.x    # твой tailscale ip
nc -zv 100.x.x.x 25565
```

В консоли `./start.sh` при входе: `Друг[/100.x.x.x:...] logged in`.

---

**На VPS потом:** Tailscale не нужен — в `public-host.txt` просто IP VPS.
