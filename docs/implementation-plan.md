# Plan wdrożenia — pionowe przyrosty

Każdy przyrost (slice) przechodzi przez wszystkie warstwy potrzebne do dostarczenia
wartości i kończy się zielonym buildem oraz commitem (lub serią commitów). Kolejność
minimalizuje ryzyko: najpierw kręgosłup danych i API, potem detekcja, na końcu UI,
który konsumuje stabilny kontrakt.

| # | Slice | Zakres | Wymagania | Definicja ukończenia |
|---|---|---|---|---|
| S0 | Fundament | analiza wymagań, ADR-y, konwencje, szkielet Boot 4.0.7/Java 25 (Initializr), konfiguracja bazowa | REQ-13 (część), NFR-05 | `./mvnw verify` zielony na pustym szkielecie |
| S1 | Klienci | encja + migracja `V1`, `POST/GET /api/customers`, walidacja, `ProblemDetail`, testy web + integracyjne | REQ-01, REQ-03 | testy zielone; błędy walidacji w RFC 9457 |
| S2 | Transakcje | encja niemutowalna + migracja `V2` (indeksy), rejestracja `201`, wyszukiwanie z wymaganym `businessId`, zgodność z klientem (422), testy filtrów | REQ-02, REQ-03, REQ-06 | komplet filtrów pokryty testami |
| S3 | Zdarzenia | `TransactionRegisteredEvent` (kształt z PDF), listener `AFTER_COMMIT` + `@Async`, wirtualne wątki, log przebiegu | REQ-07, REQ-08 | test: event po commicie, nie przed |
| S4 | Detekcja: kwota | `AmlRule`/`RuleResult` (sealed), `SuspiciousAmountRule`, próg z konfiguracji (`@ConfigurationProperties` na rekordzie), encja `Alert` + migracja `V3`, `GET /api/alerts`, test z Awaitility | REQ-09, REQ-03 | transakcja 2500 → alert `OPEN` asynchronicznie |
| S5 | Detekcja: częstotliwość | `HighFrequencyRule` (okno 1h po indeksie), scalanie powodów | REQ-10 | 6. transakcja w oknie → alert `HIGH_FREQUENCY`; obie reguły naraz → jeden alert |
| S6 | Decyzje | `GET /api/alerts/{id}` (szczegóły + historia), `POST .../decisions`, migracja `V4`, optimistic locking → 409, testy (w tym deterministyczny test stale-version) | REQ-04, REQ-05, REQ-11 | 409 przy nieaktualnej wersji; historia append-only |
| S7 | Frontend | powłoka + routing, widoki: klienci, transakcje (filtry), alerty, szczegóły alertu (timeline decyzji + formularz z obsługą 409) | REQ-12 | scenariusz manualny z README przechodzi |
| S8 | Domknięcie | ArchUnit (granice pakietów), CI (GitHub Actions, JDK 25), README (uruchomienie, architektura, decyzje, AI, weryfikacja), mapa nowości Javy, aktualizacja macierzy | REQ-13, NFR-05/06 | `./mvnw clean verify` + smoke run + przegląd macierzy |

## Architektura pakietów (package-by-feature)

```
pl.jakubmikolajczyk.monitoring
├── common/        konfiguracja, błędy (ProblemDetail), identyfikatory, czas
├── customer/      encja, repozytorium, serwis, kontroler, DTO
├── transaction/   encja, wyszukiwanie, kontroler, zdarzenie rejestracji
├── detection/     reguły AML (sealed), silnik, listener zdarzeń, properties progów
└── alert/         alert, decyzje, statusy, kontroler
```

Zależności dozwolone: `detection → transaction, alert`; funkcje biznesowe nie zależą
od `detection`; `common` nie zależy od żadnej funkcji. Granice pilnowane testem ArchUnit.

## Ryzyka i ich mitygacje

| Ryzyko | Mitygacja |
|---|---|
| Świeży ekosystem Boot 4 (zależności) | wersje przypięte po weryfikacji (ADR-0001); szkielet z oficjalnego Initializr |
| Testy asynchroniczne niestabilne | Awaitility z timeoutem zamiast `sleep`; reguły testowane też synchronicznie (unit) |
| Konflikt decyzji trudny do testowania | scenariusz deterministyczny: zapis ze stale `alertVersion` (bez wyścigu wątków) |
| Rozjazd dokumentacji i kodu | macierz w `requirements.md` aktualizowana w slice S8; ADR-y niemutowalne |
