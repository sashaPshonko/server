# PveAuction

Плагин аукциона для Paper 1.21.4 (SQLite, GUI).

## Команды

| Команда | Описание |
|---------|----------|
| `/ah` | Аукцион (пагинация в GUI) |
| `/ah sell <цена>` | Выставить (макс. 5 лотов) |

Только `/ah` и `/admin` — остальные команды заблокированы.
| `/admin` | Админка: креатив, выдача себе $ |
| `/admin give <ник> <сумма>` | Выдать деньги игроку (Vault) |

- Свой лот на АХ / в хранилище — **ЛКМ** → снять с продажи, предмет в инвентарь
- **Хранилище** (сундук) — твои лоты, перевыставить (раз в 60 с), назад в аукцион
- Для денег: **Vault** + **EssentialsX** (или другой economy-плагин)

## Сборка

Нужна только **Java 21** (Gradle встроен — `gradlew`):

```bash
cd server/server-plugin
./build.sh
```

На VPS (Ubuntu):
```bash
sudo apt update && sudo apt install -y openjdk-21-jdk
chmod +x build.sh gradlew
./build.sh
```

JAR копируется в `server/plugins/`. Перезапусти Paper.

## Структура

- `AuctionMenu` — GUI 54 слота (лоты 0–44, обновить 49, закрыть 53)
- `LotRepository` — SQLite `plugins/PveAuction/auctions.db`
- Покупка: `BEGIN` → `UPDATE sold` → выдача предмета
