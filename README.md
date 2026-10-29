## Аудит змін даних за допомогою JaVers

За основу взято проєкт [gprohorov/inno](https://github.com/gprohorov/inno/tree/inno-requirements):
Spring Boot 4.1 + MongoDB, CRUD для сутності `Item`. Код лежить у папці [`inno/`](inno).

Стандартний аудит Spring Data (`@CreatedDate`, `@LastModifiedBy` тощо) зберігає лише час створення
та останньої зміни, а всі проміжні стани втрачаються. Тому для повної історії змін підключено
JaVers, який зберігає кожну версію об'єкта в MongoDB.

Попередня лабораторна (оптимізація пам'яті) лежить у [README-lab3.md](README-lab3.md).

## Що зроблено

Шляхи нижче відносно `inno/src/main/java/chnu/edu/inno/`.

| Файл | Що в ньому |
|---|---|
| `inno/pom.xml` | залежність `javers-spring-boot-starter-mongo` 7.11.9 (версія, сумісна зі Spring Boot 4.1); для тестів додано Testcontainers |
| `config/AuditionConfiguration.java` | `@EnableMongoAuditing`, бін `AuditorAware<String>` (завжди `george`), бін `DateTimeProvider`, що обрізає час до мілісекунд |
| `config/JaversConfiguration.java` | бін `AuthorProvider`: автор комітів JaVers — `ADMIN`. Сам `Javers` створює стартер |
| `model/AuditMetadata.java` | поля `createdDate`, `createdBy`, `lastModifiedDate`, `lastModifiedBy`; від нього успадковується `Item` |
| `model/Item.java` | нове поле `description`, `equals`/`hashCode` за `id` без `NullPointerException` |
| `model/dto/request/` | `ItemCreateRequest` і `ItemUpdateRequest` з методом `toItem()` |
| `service/ItemService.java` | `@JaversAuditable` на `createItem` і `updateItem`, `@JaversAuditableDelete` на `deleteItem(Item)` |
| `controller/ItemController.java` | POST і PUT приймають DTO, DELETE повертає 204 або 404 |
| `controller/AuditController.java` | ендпоінти для перегляду історії: Changes і Shadows |

Чому `updateItem` кидає `ResponseStatusException(404)`, а не повертає `null`: JaVers робить коміт
після того, як метод завершився без винятку. Якби сервіс повернув `null` для неіснуючого `id`,
у історію потрапив би запис про зміну, якої не було. З винятком в аудит потрапляють лише
успішні операції. З тієї ж причини `DELETE` перевіряє наявність запису в контролері й лише потім
викликає `itemService.deleteItem(item)`.

`deleteItem` публічний і викликається з іншого біна не випадково: JaVers працює через Spring AOP,
а він не перехоплює приватні методи та виклики з того самого класу.

При оновленні `createdDate` і `createdBy` беруться зі старого запису, бо в тілі PUT-запиту їх немає
і без цього вони б обнулилися.

MongoDB зберігає дати з точністю до мілісекунд, а `LocalDateTime.now()` має більше знаків. Через це
JaVers бачив `createdDate` зміненим при кожному PUT (`23:12:11 -> 23:12:11`). Тому `DateTimeProvider`
обрізає час до мілісекунд, і в історії лишаються лише справжні зміни.

## Ендпоінти

CRUD (`/api/v1/items`):

| Метод | URL | Відповідь |
|---|---|---|
| GET | `/api/v1/items` | 200, усі записи |
| GET | `/api/v1/items/{id}` | 200, запис |
| GET | `/api/v1/items/response/{id}` | 200, запис в `ApiResponse` (або мета з кодом 404) |
| GET | `/api/v1/items/page?page=0&size=5` | сторінка записів |
| POST | `/api/v1/items` | 200, створений запис |
| PUT | `/api/v1/items` | 200; 404, якщо запису з таким `id` немає |
| DELETE | `/api/v1/items/{id}` | 204; 404, якщо запису немає |

`id` для PUT іде в тілі запиту (`ItemUpdateRequest`), а не в URL.

Аудит (`/audit`):

| URL | Що повертає |
|---|---|
| `/audit/items` | JSON зі змінами всіх записів `Item` |
| `/audit/shadows` | JSON зі знімками всіх записів `Item` |
| `/audit/shadows/{id}` | JSON: повний стан одного запису після кожного коміту |
| `/audit/changes/item/{id}` | зміни одного запису текстом у `<pre>` |

## Перевірка

Запущено застосунок і проведено один запис через весь життєвий цикл.

1. POST `{"name":"Apple","code":"000009","quantity":5,"description":"green apple"}` → у відповіді
   `id`, `createdBy=george`, заповнений `createdDate`. У `GET /api/v1/items` 9 записів: 8 стартових і новий.
2. PUT з тим самим `id`, `name=Apple Red`, `quantity=7` → `createdDate` не змінився, `lastModifiedDate` оновився.
3. PUT з неіснуючим `id` → 404, `/audit/changes/item/nonexistent123` порожній.
4. DELETE → 204, повторний DELETE → 404.

Результат `/audit/changes/item/{id}` після кроків 1 і 2 (для іншого запису `Pear` → `Pear2`, `1` → `2`):

```
Commit 5.00 done by ADMIN at 29 окт. 2026, 23:13:27 :
* changes on chnu.edu.inno.model.Item/6ae3b6f5fc7363fe666ce660 :
  - 'lastModifiedDate' changed: '29 окт. 2026, 23:13:25' -> '29 окт. 2026, 23:13:27'
  - 'name' changed: 'Pear' -> 'Pear2'
  - 'quantity' changed: '1' -> '2'
Commit 4.00 done by ADMIN at 29 окт. 2026, 23:13:25 :
* new object: chnu.edu.inno.model.Item/6ae3b6f5fc7363fe666ce660
  ...
```

Після видалення в історії з'являється ще один коміт, де JaVers позначає об'єкт як видалений:

```
* object removed: chnu.edu.inno.model.Item/6ae3b6ab858847df9aefc324
  - 'name' value 'Apple Red' unset
  - 'quantity' value '7' unset
  ...
```

`/audit/shadows/{id}` для цього запису повертає 2 версії до видалення (`Apple`, 5 і `Apple Red`, 7)
і 3 після нього, бо видалення додає останній (terminal) знімок. Changes показують, яке саме поле
змінилося і з якого значення на яке. Shadows показують, як виглядав увесь об'єкт у кожен момент.

Автоматично цей сценарій перевіряє `ItemAuditIntegrationTest`: створення, оновлення, 404 для
неіснуючого `id`, зміни в аудиті, видалення. Тест працює на реальній MongoDB у контейнері
(Testcontainers), тож для `mvn verify` потрібен лише запущений Docker.

## Запуск

Потрібні JDK 25 (у `pom.xml` `java.version=25`) і Docker.

1. Запустити MongoDB:

```bash
docker run -d --name inno-mongo -p 27017:27017 mongo:7
```

2. Запустити застосунок:

```bash
cd inno
./mvnw spring-boot:run
```

Якщо встановлено лише JDK 17, додати `-Djava.version=17`: Spring Boot 4 працює і на ньому.
Порт за замовчуванням 8080, змінити його можна аргументом
`-Dspring-boot.run.arguments=--server.port=8081`.

3. Тести:

```bash
./mvnw verify
```

JaVers сам створює в базі `inno` колекції `jv_snapshots` і `jv_head_id`.
