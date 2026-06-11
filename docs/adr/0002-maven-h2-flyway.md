# ADR-0002: Maven (wrapper) + H2 in-memory + Flyway

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

Zadanie wymaga relacyjnej bazy danych i ma być **łatwe do uruchomienia przez oceniającego**
(NFR-01). Jednocześnie kontekst bankowy wymaga audytowalnej ewolucji schematu (NFR-02).

## Decyzja

1. Budowanie: **Maven + Maven Wrapper** (`./mvnw`) — zero instalacji po stronie oceniającego.
2. Baza: **H2 in-memory** jako jedyny profil uruchomieniowy zadania.
3. Schemat: **Flyway** (migracje SQL `V1__...` … `V4__...`), Hibernate w trybie
   `ddl-auto=validate` — ORM **waliduje** schemat, nigdy go nie tworzy.

## Uzasadnienie

- Maven to standard de facto w polskim sektorze bankowym; deklaratywny `pom.xml` jest
  natychmiast czytelny dla każdego oceniającego.
- H2 eliminuje wymaganie Dockera/instalacji bazy — `./mvnw spring-boot:run` wystarcza.
- Flyway czyni schemat **kodem podlegającym review**: każda migracja odpowiada slice'owi
  funkcjonalnemu i commitowi — historia schematu opowiada tę samą historię co git log.
  `ddl-auto=validate` wyłapuje rozjazd encji i schematu już na starcie kontekstu.

## Konsekwencje

- (+) Klonowanie → uruchomienie w jednym kroku; testy integracyjne szybkie (bez kontenerów).
- (−) Dane znikają po restarcie (akceptowalne w zadaniu; odnotowane w README).
- (−) H2 ≠ produkcyjny silnik. Ścieżka ewolucji: PostgreSQL + Testcontainers w testach —
  wymaga jedynie podmiany sterownika, datasource i ewentualnych poprawek dialektu w migracjach.

## Rozważane alternatywy

- **PostgreSQL + Docker Compose + Testcontainers** — produkcyjnie tak; tu odrzucone,
  bo podnosi próg uruchomienia (Docker) wbrew NFR-01.
- **Gradle** — pełnoprawna alternatywa; odrzucony na rzecz powszechności Mavena w domenie.
- **Liquibase** — funkcjonalnie równoważny; Flyway wybrany za prostotę (czyste SQL).
- **`ddl-auto=update`** — niedopuszczalne: niekontrolowane, nieaudytowalne zmiany schematu.
