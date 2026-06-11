# ADR-0001: Java 25 (LTS) + Spring Boot 4.0.7

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

Zadanie wymaga „Java i Spring Boot" bez wskazania wersji. Dodatkowy cel projektu:
praktyczne wykorzystanie nowości języka wprowadzonych po Javie 11 (rekordy, sealed types,
pattern matching, wirtualne wątki) oraz aktualnej generacji Spring Boota.

Stan ekosystemu na dzień decyzji (2026-06-12):

- **Java 25** — najnowszy LTS (wrzesień 2025). Wirtualne wątki są w pełni dojrzałe:
  JEP 444 (finał w 21) + JEP 491 (Java 24) usunął pinning na `synchronized`.
- **Spring Boot 4.0** — GA 20.11.2025 (Spring Framework 7, Jackson 3, wsparcie Java 17–25);
  najnowszy patch linii: **4.0.7**.
- **Spring Boot 4.1.0** — wydany **10.06.2026, dwa dni przed tą decyzją**.

## Decyzja

Java **25** (Eclipse Temurin) + Spring Boot **4.0.7**, springdoc-openapi **3.0.3**.

## Uzasadnienie

- Java 25 spełnia cel edukacyjny i jest wspierana długoterminowo — naturalny wybór na nowy projekt.
- Spring Boot 4.0.7 to **najnowszy patch sprawdzonej linii**: osiem miesięcy poprawek,
  potwierdzona kompatybilność bibliotek towarzyszących (springdoc 3.0.3 deklaruje wsparcie
  Boot 4.0.5+).
- Boot 4.1.0 odrzucono **świadomie**: w środowisku bankowym nie adoptuje się wydań minor
  w dniu premiery — brak potwierdzenia kompatybilności zależności trzecich i zerowy czas
  ekspozycji na błędy regresji. Migracja 4.0.x → 4.1.x to w przyszłości zmiana wyłącznie
  wersji w `pom.xml`.

## Konsekwencje

- (+) Dostęp do pełnego zestawu nowości Javy 21–25 i Spring Framework 7 (m.in. natywne
  API versioning, JSpecify null-safety, `ProblemDetail`).
- (+) Okno wsparcia OSS linii 4.0 do końca 2026 r.; przejście na 4.1.x trywialne.
- (−) Ekosystem Boot 4 jest młody — niektóre biblioteki wymagają nowych linii wydań
  (np. springdoc 3.x zamiast 2.x); wersje dobrano jawnie i przypięto.

## Rozważane alternatywy

- **Spring Boot 4.1.0** — odrzucony: 2 dni od premiery (uzasadnienie wyżej).
- **Spring Boot 3.5.x** — odrzucony: poprzednia generacja; cel projektu zakłada Boot ≥ 4.
- **Java 21** — odrzucona: poprzedni LTS; brak JEP 491 (pinning wirtualnych wątków
  na `synchronized`), mniejsza wartość edukacyjna.
