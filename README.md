# Photo Gallery — ЛР1 (Spring Security) + ЛР2 (Unit-тести)

Персональний хмарний сервіс для зберігання фото: альбоми, теги, рівні доступу (PRIVATE / SHARED / PUBLIC).
Кожен користувач бачить лише свої альбоми та фото (ізоляція за `preferred_username`).

## Структура
```
src/main/java/com/gallery
 ├─ config/       SecurityConfig, OpenApiConfig
 ├─ controller/   AlbumController, PhotoController, TagController
 ├─ dto/          *Request / *Response / TagDto (records)
 ├─ mapper/       MapStruct: AlbumMapper, PhotoMapper, TagMapper
 ├─ model/        Album, Photo, Tag, AccessLevel
 ├─ repository/   Spring Data JPA
 ├─ service/      AlbumService, PhotoService, TagService
 ├─ exception/    NotFoundException, GlobalExceptionHandler
 └─ GalleryApplication.java
src/main/resources
 ├─ db.changelog/ Liquibase (master + changes/001-init-schema.yaml)
 ├─ static/index.html   простий UI
 └─ application.yml
src/test/java/com/gallery/service   Unit-тести (ЛР2)
postman/Gallery.postman_collection.json
```

## Запуск
1. Postgres: база `gallery`, користувач `postgres`. Пароль — у `application.yml` (`DB_PASSWORD`) або змінна середовища `DB_PASSWORD`.
2. Keycloak: `docker compose up -d` → http://localhost:8180 (admin / admin).
3. Налаштування Keycloak:
   - Створити realm **gallery**.
   - Realm roles: **USER**, **ADMIN**.
   - Users: `alice` / `alice` (роль USER) та `admin1` / `admin1` (роль ADMIN) — Credentials → пароль, Temporary = Off.
   - Clients → Create: `gallery-app`, Client authentication = **On**, Standard flow + **Direct access grants** = On,
     Valid redirect URIs: `http://localhost:8080/login/oauth2/code/keycloak`. Секрет — вкладка Credentials.
   - Client scopes → `roles` → Mappers → `realm roles`: увімкнути **Add to ID token** та **Add to userinfo**
     (щоб ролі потрапляли у UI-логін).
4. Задати секрет: `KEYCLOAK_CLIENT_SECRET=<secret>` (або у `application.yml`).
5. `mvn spring-boot:run` → http://localhost:8080 (UI, редирект на Keycloak). Swagger: `/swagger-ui.html`.

Postman: імпортувати колекцію, у змінних колекції вказати `clientSecret`, запустити папки по порядку (Runner).

## ЛР1 — де що
| AC | Реалізація |
|----|-----------|
| 1 | Controller/Service/Repository/Entity/DTO + MapStruct (`mapper/`) |
| 2 | Liquibase `db.changelog/`, `ddl-auto: validate` |
| 3 | `static/index.html` |
| 4 | `oauth2Login` у `SecurityConfig` (редирект на Keycloak) |
| 5 | `oauth2ResourceServer().jwt()` — `Authorization: Bearer <token>` |
| 6 | `DELETE /api/tags/**` → `hasRole("ADMIN")` у `SecurityFilterChain` (USER отримує 403) |
| 7 | `@PreAuthorize("hasRole('ADMIN')")` на `PhotoService.getAllForAdmin()` (`GET /api/photos/admin/all`; фільтр пускає всіх автентифікованих, метод блокує USER) |
| 8 | Postman-колекція |

## ЛР2 — Unit-тести
Запуск: `mvn test`. Звіт покриття: `target/site/jacoco/index.html` (сервіси ≈ 100%).
Тести: `AlbumServiceTest`, `PhotoServiceTest`, `TagServiceTest` — Mockito (`@Mock/@InjectMocks`), без Spring-контексту, БД і мережі;
кожен публічний метод має позитивний і негативний тест; побічні ефекти перевіряються через `verify`/`ArgumentCaptor`.

**AC4 (доказ, що тест справді перевіряє логіку):**
1. У `AlbumService.getOwnedEntity` змінити `album.getOwner().equals(owner)` на `!album.getOwner().equals(owner)`.
2. `mvn test` → тести падають → зробити скріншот.
3. Повернути код назад → `mvn test` → усе зелене → (за бажанням) другий скріншот.
