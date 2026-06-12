# Analiza wymagań — System monitorowania transakcji (AML)

> Źródło wymagań: `Zadanie_rekrutacyjne.pdf` (materiał rekrutacyjny, świadomie poza repozytorium).
> Ten dokument jest jedynym punktem odniesienia dla zakresu — każde wymaganie ma identyfikator
> `REQ-xx`, do którego odwołują się commity (stopka `Refs:`), kod i testy.

## 1. Cel i kontekst biznesowy

System wspiera proces wykrywania podejrzanych transakcji finansowych (AML — Anti-Money
Laundering) w uproszczonym wariancie rekrutacyjnym:

1. rejestracja klientów i ich transakcji,
2. automatyczna, **asynchroniczna** analiza każdej transakcji,
3. generowanie alertów dla operacji podejrzanych,
4. obsługa alertów przez analityka (decyzje z pełną historią — audyt).

Cytat wprost z zadania, traktowany jako naczelna zasada projektu:

> „Chodzi bardziej o sensowne podejście i jakość rozwiązania niż o perfekcyjny produkt."

## 2. Zakres

**W zakresie:** backend REST API (Java + Spring Boot), prosty użyteczny frontend
(Web Components, czysty JavaScript), relacyjna baza danych, przetwarzanie zdarzeniowe,
logika generowania alertów, README z opisem decyzji.

**Poza zakresem (świadomie):** uwierzytelnianie/autoryzacja, zewnętrzny broker komunikatów,
przeliczenia walutowe (FX), paginacja list, wielojęzyczność UI. Każde z tych uproszczeń jest
odnotowane w README wraz ze ścieżką ewolucji.

## 3. Słownik domeny

| Pojęcie | Znaczenie w systemie |
|---|---|
| **Customer** | Klient banku zarejestrowany w kontekście biznesowym (`businessId`). |
| **Transaction** | Niemutowalny fakt finansowy klienta: kwota, waluta, czas biznesowy (`transactionDate`). |
| **Alert** | Wynik analizy AML jednej transakcji; status `OPEN → APPROVED/REJECTED`. |
| **AlertDecision** | Pojedyncza decyzja analityka (`APPROVE`/`REJECT` + komentarz); historia jest append-only. |
| **businessId** | Identyfikator **kontekstu biznesowego** (np. jednostki banku), propagowany klient → transakcja → alert. Patrz [ADR-0003](adr/0003-businessid-business-context.md) — to najważniejsza niejednoznaczność zadania. |
| **reason** | Kod(y) naruszonej reguły AML, np. `SUSPICIOUS_AMOUNT`, `HIGH_FREQUENCY`. |

## 4. Wymagania funkcjonalne (macierz traceability)

| ID | Wymaganie (źródło: PDF §) | Realizacja | Weryfikacja |
|---|---|---|---|
| REQ-01 | Dodanie klienta (§4.1) | `POST /api/customers` — `CustomerController` | `CustomerApiIntegrationTest` |
| REQ-02 | Dodanie transakcji (§4.1) | `POST /api/transactions` — `TransactionController` | `TransactionApiIntegrationTest` |
| REQ-03 | Przeglądanie list klientów, transakcji i alertów (§4.1) | `GET /api/customers`, `GET /api/transactions`, `GET /api/alerts` | testy integracyjne ww. modułów |
| REQ-04 | Podgląd szczegółów alertu (§4.1) | `GET /api/alerts/{id}` — `AlertController` | `AlertDecisionIntegrationTest` |
| REQ-05 | Dodanie decyzji do alertu (§4.1, §4.5) | `POST /api/alerts/{id}/decisions` | `AlertDecisionIntegrationTest` |
| REQ-06 | Filtrowanie transakcji: `businessId` (wymagane), `customerId` (opcjonalne), zakres dat (opcjonalny) (§4.2) | `GET /api/transactions` + `TransactionRepository` | `TransactionSearchIntegrationTest` |
| REQ-07 | Po zapisie transakcji publikowany jest event `{eventId, businessId, transactionId}` (§4.3) | `TransactionRegisteredEvent` (rekord), publikacja w `TransactionService` | `TransactionEventTest` |
| REQ-08 | Osobny komponent odbiera event i decyduje o utworzeniu alertu (§4.3) | `TransactionAnalysisListener` + `AmlRuleEngine` (pakiet `detection`) | `DetectionIntegrationTest` |
| REQ-09 | Alert, gdy kwota przekroczy wartość z parametru, np. 2000 zł (§4.4) | `SuspiciousAmountRule` + `aml.rules.suspicious-amount.threshold` | `SuspiciousAmountRuleTest`, test integracyjny |
| REQ-10 | Alert, gdy klient wykonał > 5 transakcji w ciągu 1 godziny (§4.4) | `HighFrequencyRule` + zapytanie po indeksie `(customer_id, transaction_date)` | `HighFrequencyRuleTest`, test integracyjny |
| REQ-11 | Decyzje `APPROVE`/`REJECT`; każda zapisana jako **nowy wpis** (§4.5) | encja `AlertDecision` (append-only), status alertu = ostatnia decyzja | `AlertDecisionIntegrationTest` |
| REQ-12 | Frontend: lista klientów, lista transakcji z filtrami, lista alertów, szczegóły alertu, historia decyzji, formularz decyzji (§5) | Web Components w `src/main/resources/static` | smoke test manualny (README §5 pkt 2), wykonany przed oddaniem |
| REQ-13 | README: uruchomienie, architektura, decyzje, użycie AI, weryfikacja poprawności (§7) | `README.md` | przegląd dokumentu |

## 5. Wymagania niefunkcjonalne (wywiedzione z treści i kontekstu bankowego)

| ID | Wymaganie | Realizacja |
|---|---|---|
| NFR-01 | Proste uruchomienie przez oceniającego — bez zewnętrznych zależności | H2 in-memory, `./mvnw spring-boot:run`, frontend serwowany przez Spring |
| NFR-02 | Audytowalność: transakcje niemutowalne, decyzje append-only, schemat bazy wersjonowany | `updatable=false`, brak `PUT`/`DELETE`, Flyway |
| NFR-03 | Spójny, samoopisujący kontrakt błędów | RFC 9457 `application/problem+json` |
| NFR-04 | Bezpieczeństwo współbieżności przy decyzjach analityków | optimistic locking (`@Version`) → HTTP 409 |
| NFR-05 | Czytelna, atomowa historia commitów (PDF §7: „duży plus") | Conventional Commits + `Refs: REQ-xx`, patrz [git-convention.md](git-convention.md) |
| NFR-06 | Obserwowalność minimum | Spring Boot Actuator (health/info/metrics), logi przebiegu analizy |

## 6. Niejednoznaczności i przyjęte interpretacje

Zadanie celowo pozostawia luzy interpretacyjne. Każdą decyzję podjęto jawnie:

1. **Czym jest `businessId`?** W przykładach PDF ta sama wartość `b001` występuje na
   kliencie, transakcji i alercie, a w przykładzie eventu pojawia się `BANK_A`. Gdyby
   `businessId` był unikalnym identyfikatorem pojedynczego rekordu, wymagany filtr
   `businessId` przy wyszukiwaniu transakcji czyniłby pozostałe filtry bezużytecznymi.
   **Interpretacja:** identyfikator kontekstu biznesowego (partycja danych) — [ADR-0003](adr/0003-businessid-business-context.md).
2. **Próg kwotowy a waluta.** PDF: „np. 2000 zł", ale transakcje mają dowolną walutę.
   **Interpretacja:** porównujemy samą kwotę (uproszczenie bez FX) — [ADR-0004](adr/0004-money-bigdecimal.md).
3. **Okno „1 godziny".** Liczone względem `transactionDate` analizowanej transakcji
   (czas biznesowy), nie czasu systemowego analizy — deterministyczne i testowalne.
4. **`> 5 transakcji`** — alert od **szóstej** transakcji w oknie (ostra nierówność, zgodnie z literą PDF).
5. **Kilka reguł naruszonych jednocześnie** → **jeden** alert ze scalonymi powodami — [ADR-0007](adr/0007-single-alert-merged-reasons.md).
6. **Wielokrotne decyzje dla alertu** są dozwolone (re-review); status odzwierciedla ostatnią,
   historia przechowuje wszystkie — [ADR-0008](adr/0008-optimistic-locking-decisions.md).
7. **Kod statusu odpowiedzi przy rejestracji transakcji:** `201 Created` (zasób utrwalony
   synchronicznie; asynchroniczna jest wyłącznie analiza) — [ADR-0009](adr/0009-rest-contract-problemdetail.md).

## 7. Dozwolone uproszczenia z PDF i nasze stanowisko

PDF dopuszcza zaślepkę eventu („komentarz i log do konsoli"). Świadomie robimy **więcej niż
minimum**, ale **mniej niż produkcję**: pełny mechanizm zdarzeń wewnątrz aplikacji
(`ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + wirtualne wątki),
bez zewnętrznego brokera. Granica i ścieżka ewolucji (Transactional Outbox + Kafka/RabbitMQ)
opisana w [ADR-0006](adr/0006-async-in-app-events.md).
