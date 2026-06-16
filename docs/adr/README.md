# Rejestr decyzji architektonicznych (ADR)

Format: [MADR](https://adr.github.io/madr/) w wariancie uproszczonym. Decyzje są
niemutowalne — zmiana decyzji to nowy ADR z adnotacją „zastępuje".

| Nr | Decyzja | Obszar |
|---|---|---|
| [0001](0001-java-25-spring-boot-4.md) | Java 25 (LTS) + Spring Boot 4.0.7 (świadomie nie 4.1.0) | stack |
| [0002](0002-maven-h2-flyway.md) | Maven wrapper + H2 in-memory + Flyway (`ddl-auto=validate`) | build / dane |
| [0003](0003-businessid-business-context.md) | `businessId` = identyfikator kontekstu biznesowego | domena |
| [0004](0004-money-bigdecimal.md) | Pieniądze: `BigDecimal` + ISO 4217; próg bez FX | domena |
| [0005](0005-immutable-transactions.md) | Transakcje niemutowalne; czas biznesowy vs systemowy | domena / audyt |
| [0006](0006-async-in-app-events.md) | Zdarzenia in-app `AFTER_COMMIT` + wirtualne wątki | architektura |
| [0007](0007-single-alert-merged-reasons.md) | Jeden alert/transakcję; sealed `RuleResult`; idempotencja w bazie | domena / detekcja |
| [0008](0008-optimistic-locking-decisions.md) | Decyzje append-only + optimistic locking → 409 | domena / współbieżność |
| [0009](0009-rest-contract-problemdetail.md) | RFC 9457 `ProblemDetail` (+ catch-all 500); 201 dla transakcji; paginacja z limitem | API |
| [0010](0010-frontend-web-components.md) | Czyste Web Components, CSS w plikach (`<link>`), bez bundlera | frontend |
| [0011](0011-uuidv7-identifiers.md) | UUIDv7 generowane w aplikacji | persystencja |
| [0012](0012-transaction-boundaries.md) | Jawne granice transakcji (serwis, `readOnly`, 2 krótkie transakcje w detekcji) | transakcyjność |
