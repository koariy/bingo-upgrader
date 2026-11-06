# Апгрейдер — часть 3

Финальная версия мода 1.0.0. Minecraft 26.2, Java 25 и зависимости части 2 сохранены.
Это документация серверной механики, созданной в части 3. Графический клиент
реализован в финальной части: см. CLIENT.md и README.md. Блок использует свою
текстуру textures/block/upgrader.png; модели, рецепт, loot и теги кирки включены.

## Проверка в игре

Работает в Survival/Adventure при закрытом чужом меню. Creative/Spectator
исключены из экономических операций. Основная рука автоматически выбирается
целиком при открытии; броня и вторая рука не участвуют.

1. Взять 64 палки и выполнить `/upgrader`, нажать **U** или нажать ПКМ по блоку.
2. `/upgrader preview minecraft:diamond 1` — сервер покажет **23.04%** при
   стандартных значениях. Для одной палки будет **0.36%**, плюс предупреждение.
3. `/upgrader roll` — совершить одну попытку. Предметы списываются при любом
   исходе; при успехе приз помещается в инвентарь. Повторная команда без нового
   превью не совершает попытку.
4. `/upgrader quick x2` (также x4, x8, p30, p50, p70) — подобрать ближайшую цель
   и получить превью; для самой попытки всё равно требуется `/upgrader roll`.
5. `/upgrader clear` — очистить выбор. `/upgrader offer 0 16` и
   `/upgrader offer 9 2` — выбрать несколько стопок/их части. Слот 0..8 — хотбар,
   9..35 — основной инвентарь. Повторный offer заменяет количество в этом слоте.

Клавиша переназначается в стандартных настройках управления Minecraft.
Выбранные предметы остаются в инвентаре до commit, поэтому закрытие игры,
разрыв подключения и истечение сессии не требуют возврата предметов из escrow.

Рецепт блока `bingo_upgrader:upgrader`:

```text
I R I
R C R
I I I
```

I = железный слиток, R = редстоун, C = верстак. Добывается каменной киркой
или лучше. Блок не хранит инвентарь/состояние игрока и не требует BlockEntity.
Команда и клавиша доступны без блока; удаление блока не запирает предметы.

## Конфиг

Автоматически создаётся `config/bingoupgrader/upgrader.json`:

```json
{
  "houseEdge": 0.1,
  "minChance": 0.0005,
  "maxChance": 0.9,
  "maxTargetValue": 100000,
  "attemptsPerMinute": 30,
  "requestsPerSecond": 10,
  "sessionSeconds": 300,
  "previewSeconds": 15,
  "pityEnabled": false,
  "allowUnknownTargets": false,
  "pityAfterFailures": 10,
  "pityStep": 0.002,
  "pityMaxBonus": 0.05,
  "blacklist": [
    "minecraft:dragon_egg", "minecraft:bedrock", "minecraft:barrier",
    "minecraft:command_block", "minecraft:chain_command_block",
    "minecraft:repeating_command_block", "minecraft:structure_block",
    "minecraft:structure_void", "minecraft:jigsaw", "minecraft:debug_stick",
    "minecraft:spawner", "minecraft:trial_spawner", "minecraft:light",
    "minecraft:knowledge_book"
  ],
  "blacklistTags": ["minecraft:spawn_eggs"]
}
```

Вероятности — доли единицы: 0.0005 = 0.05%, 0.002 = 0.2 процентных пункта.
`/upgrader reload` (OP 2) перезагружает values.json и upgrader.json; оба JSON
проверяются до применения, старые сессии становятся недействительны. Некорректный
файл сохраняется для исправления; при первоначальном запуске ошибка логируется
и используются defaults. Списки в JSON заменяют соответствующий default-список.
Чёрный список values.json действует дополнительно; SpawnEggItem всегда запрещён,
даже если его тег удалён. И вход, и выход проходят одинаковую проверку.

Выход — одна обычная стопка одного типа, count <= maxStackSize. Допускаются
несколько входных стопок. Входные data component patches допускают только
DAMAGE, ENCHANTMENTS, STORED_ENCHANTMENTS, POTION_CONTENTS: непосчитанное содержимое
контейнеров, произвольный CUSTOM_DATA и прочие изменения исключены. Например,
предмет с custom name пока также отклоняется. Это ограничение можно расширять
вместе с оценкой новых компонентов, а не выдавать за них необоснованную ценность.

## Математика

```text
raw = inputValue / targetValue * (1 - houseEdge)
если raw < minChance: отказ без списания
chance = min(raw, maxChance)
```

Цель должна иметь суммарную ценность **строго выше** входа. Минимум не округляет
шанс вверх: такое округление позволяло бы получать положительное математическое
ожидание на дорогих целях. При выключенной pity и неактивном верхнем cap:
`E[выход] = chance * targetValue = (1-houseEdge) * inputValue`.
Верхний cap только уменьшает ожидание. Поэтому требование «близко к 90%»
проверяется для пар без ограничения cap; для ограниченных пар верно «не выше».

Pity по умолчанию выключена. Начиная с N неудач, прибавка составляет
`min(pityMaxBonus, (failures-N+1)*pityStep)`. Итог дополнительно ограничен
`input/target*(1-houseEdge/2)` и maxChance. Это даёт повышение шанса, сохраняя
минимум половину маржи: даже после длинной серии ожидание строго ниже вложения.
Успех сбрасывает серию, отказы валидации её не увеличивают. При включённой pity
ожидание выше базовых 90%, но не выше 95% при houseEdge=10%.

Быстрый выбор ищет известные/допустимые/включённые предметы и ближайшее число
штук, ограниченное размером стопки и maxTargetValue. Режим xN ищет input*N,
pNN — input*(1-houseEdge)/(NN/100). Из-за дискретных предметов проценты могут
отличаться; сервер всегда показывает фактический шанс. unknown исключён из
автоподбора, но ручная цель использует unknownValue только при `allowUnknownTargets=true`.
По умолчанию unknown-призы запрещены, чтобы недооценённые редкие ресурсы не
создавались за цену по умолчанию; overrides делают оценку известной.

## Протокол и защита транзакции

В `common.network.UpgradePackets`:

| Пакет | Направление | Содержимое |
| --- | --- | --- |
| OpenRequest | C2S | Запрос сессии |
| Opened | S2C | UUID сессии, revision таблицы |
| UpgradeCatalog | S2C | Разрешённые обычные цели, edge/min/max/cap, preview TTL |
| PreviewRequest | C2S | Сессия, requestId, список Offer(slot,count), target/count или quick mode |
| Quoted | S2C | requestId, одноразовый token, выбранная цель, вход/выход, шанс, low flag |
| StartRequest | C2S | Сессия и token, без доверенных значений/шанса |
| Result | S2C | Token, won, цель/count, chance, landing |
| Rejected | S2C | Ограниченный код причины и requestId |

Каждый codec ограничивает строки, количество слотов (<=36) и данные Offer.
Preview проверяет уникальность слотов, реальные количества, компоненты,
blacklist обоих модулей, enabled features и пределы. Клиент не может передать
доверенный ItemStack с произвольными данными в качестве приза.

Start на серверном потоке проверяет одноразовый token, срок, revision таблицы,
режим/состояние игрока, открытое меню, текущий rolling-minute лимит и **точные
снимки** выбранных стопок (с количеством и компонентами). Смена выбора и reload
инвалидируют preview. При отличии даже количества предметов требуется новое
preview. Повторный start с использованным token отклоняется.

Сначала строятся копии инвентаря после списания и после возможного выигрыша.
При нехватке места отказ происходит без списания и RNG. После всех проверок
SecureRandom определяет исход, затем единый синхронный commit записывает debit
и credit в 36 серверных слотов, обновляет историю и синхронизирует инвентарь.
Внутри commit нет await/сетевых callbacks. Результат отправляется после commit;
разрыв соединения не оставляет отложенную выдачу. Гарантия атомарности относится
к серверному потоку; сохранение мира/инвентаря на диск следует обычному циклу Minecraft.

`landing` — нормированная координата остановки колеса [0,1). Успех выбирает точку
в [0,chance), неудача — [chance,1). Финальный клиент анимирует уже полученный
результат; он не генерирует исход и не запрашивает выдачу после анимации.

Rolling-minute попытки и pity записываются в SavedData overworld
`bingo_upgrader:upgrade_history` по UUID, сохраняются при штатном сохранении мира,
не сбрасываются при переподключении. Ограничение входящих запросов также хранится
отдельно от сессии, чтобы повторное открытие не сбрасывало его. Сессии временные,
не держат предметы и удаляются при disconnect/остановке.

## API следующей части

Все методы UpgradeService вызываются на серверном потоке после SERVER_STARTED:

- `get(server)`, `open(player)` — получить сервис/открыть сессию.
- `currentSession(player)` — UUID активной сессии для команд.
- `select(player,slot,count)`, `clearSelection(player)` — текстовое управление выбором.
- `preview(player,session,offers,targetId,count,mode)` — расчёт и выдача токена.
- `roll(player,session,token)` — проверить и совершить попытку.
- `commandPreview(...)`, `commandRoll(player)` — адаптеры временных подкоманд.
- `reload()`, `applyConfig(validatedConfig)` — обновить конфиг и закрыть сессии.
- `disconnect(uuid)` — удалить сессию без сброса истории/лимита.

Общий `UpgradeMath.calculate(input,output,failures,config)` возвращает
`Odds(chance,available,veryLow)`; чистый метод, без RNG. `wins(sample,chance)`
используется сервером и симуляцией. `UpgradePolicy.allowed(stack,values,config)`
применяет ограничения поверх модуля ценностей; есть перегрузка со снимком ValueTable.
`InventoryTransaction.insert(simulatedSlots,reward)` работает только на частной
копии инвентаря; при false копию следует выбросить, она может быть частично изменена.

На клиенте `ClientUpgrader.opened()/quoted()/result()/rejected()` дают последние
серверные сообщения. `preview(...)` и `start()` отправляют запросы, не меняя
инвентарь. GUI 1.0.0 использует их; следующий клиент может применять собственный контроллер поверх
того же протокола. `clear()` сбрасывает клиентское состояние.

## Классы

- common/config: UpgraderConfig.
- common/upgrader: Offer, UpgradeMath, UpgraderBlock.
- common/network: UpgradePackets с семью typed payload records.
- server/upgrader: UpgraderConfigStore, UpgradeService, UpgradePolicy,
  InventoryTransaction, UpgradeHistory, UpgradeCommands.
- server/network: UpgradeNetworking.
- client/network: ClientUpgrader (receivers, клавиша, состояние для будущего GUI).
- test: UpgradeModuleChecks, вызывается из общего valueChecks.

## Проверка и источники

`./gradlew build` выполняет проверки части 2 и части 3. Balance simulation:
2 000 000 попыток для каждой из шести пар (12 млн суммарно), фиксированный seed,
допуск 6 стандартных ошибок среднего Bernoulli. Проверяются также пороги,
pity, капы, данные blacklist, неподдерживаемые компоненты, инвентарь без места,
заполнение/объединение стопок, сохранение истории и сетевые codecs. Отчёт:
`build/value-checks/balance.csv`.

Реальный Fabric dedicated-server bootstrap проверен до EULA: мод 0.3.0 загружен,
entrypoint/регистрация блока и пакетов выполняются без ошибки. EULA не принималась;
одиночный мир и финальный GUI 1.0.0 проверены дополнительно, см. VERIFICATION.md.

Использованы закреплённые API 26.2: Registry.register + ResourceKey/setId для
Block/BlockItem, UseBlockCallback, SavedDataType с Codec и SavedDataStorage,
KeyMapping.Category.register и KeyMappingHelper (client.keymapping.v1),
ClientTickEvents, serverboundPlay/clientboundPlay typed codecs/receivers.
Сигнатуры сверены по Minecraft 26.2 / Fabric API 0.161.0+26.2 через javap;
JSON рецепта/loot сверены с vanilla smithing_table из Minecraft 26.2.
Официальные руководства:
https://docs.fabricmc.net/develop/networking,
https://docs.fabricmc.net/develop/key-mappings,
https://docs.fabricmc.net/develop/saved-data,
https://docs.fabricmc.net/develop/blocks/first-block.

Финальные визуальные проверки и состояние сборки описаны в docs/VERIFICATION.md.
