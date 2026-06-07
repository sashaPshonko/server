# WorldEdit (FastAsyncWorldEdit) — только админы

## Установка

```bash
cd ~/server   # или server/ на Mac
bash install-worldedit.sh
stop          # в консоли Minecraft
./start.sh
```

Игрокам **ничего** на клиент не ставить.

## Кто может пользоваться

| Право | Кто |
|--------|-----|
| `pveauction.worldedit` | полный WorldEdit (`worldedit.*`) |
| `pveauction.admin` | то же (включено как дочернее право) |
| OP | по умолчанию всё |

Обычные игроки команды `//` **не видят** — нет прав.

## Приваты vs WorldEdit

| Команда | Плагин | Зачем |
|---------|--------|--------|
| `/wand`, `/expand`, `/claim`, `/apriv` | PveAuction | админ-приваты |
| `//wand`, `//set`, `//copy` … | FAWE | строительство |

Не путать: **топор привата** — `/wand`, **топор WE** — `//wand`.
