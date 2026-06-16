# ADR-0012: Jawne granice transakcji

- **Status:** zaakceptowany
- **Data:** 2026-06-16

## Kontekst

W systemie bankowym granice transakcji bazodanowych nie mogą być przypadkowe.
Domyślne zachowanie Springa (transakcja na repozytorium per wywołanie) bywa
wystarczające, ale niejawne — chcemy, by każda granica była **widoczna w kodzie**
i opisana, a nie wywiedziona z konfiguracji.

## Decyzja

1. **Serwisy jako granica transakcji**, nie repozytoria ani kontrolery. Każdy serwis
   ma klasowo `@Transactional(readOnly = true)`; metody zmieniające stan nadpisują to
   jawnym `@Transactional`:
   - `CustomerService.register`, `TransactionService.register`,
     `AlertService.raise`, `AlertService.decide` → zapis (`@Transactional`),
   - cała reszta (odczyty, wyszukiwanie, paginacja) → `readOnly = true`.
2. **`spring.jpa.open-in-view=false`** (ADR-0002): brak „magicznej" sesji rozciągniętej
   na renderowanie odpowiedzi — każdy dostęp do danych dzieje się w jawnej granicy
   serwisu, a leniwe kolekcje nie ładują się przypadkiem poza transakcją.
3. **Detekcja AML to dwie krótkie transakcje, nie jedna długa.** `AmlRuleEngine.analyse`
   **nie** jest opakowany w `@Transactional`: najpierw czyta transakcję (krótka
   transakcja `readOnly` w `TransactionService`), potem `AlertService.raise` zapisuje
   alert we własnej transakcji. Powód: idempotencja opiera się na ograniczeniu
   `UNIQUE(transaction_id)` — przy współbieżnym dublu to `raise` ma wygenerować
   `DataIntegrityViolationException`, przechwycony w `TransactionAnalysisListener`
   (ADR-0007). Gdyby całą analizę spiąć w jedną transakcję, kolizja oznaczałaby
   rollback całości — działa, ale zaciera intencję „zapis alertu jest atomowy i to on
   posiada swoją transakcję".
4. **Publikacja zdarzenia wewnątrz transakcji zapisu, konsumpcja po commicie.**
   `TransactionService.register` publikuje `TransactionRegisteredEvent` w trakcie swojej
   transakcji; `@TransactionalEventListener(AFTER_COMMIT)` gwarantuje, że analiza widzi
   dane utrwalone (ADR-0006). Po `AFTER_COMMIT` nie ma już aktywnej transakcji — listener
   działa na wirtualnym wątku i otwiera własne, nowe transakcje przez serwisy.

## Uzasadnienie

- `readOnly = true` to nie kosmetyka: pozwala JPA/Hibernate pominąć dirty-checking i
  flush, a czytelnikowi mówi wprost „ta ścieżka nie pisze".
- Trzymanie granicy w serwisie (nie w kontrolerze) izoluje transakcyjność od warstwy web
  i czyni ją testowalną bez HTTP.
- Rozdzielenie odczytu i zapisu w detekcji jest świadome i sprzężone z modelem
  idempotencji z ADR-0007.

## Konsekwencje

- (+) Każda granica transakcji jest jawna w kodzie (adnotacja na metodzie serwisu).
- (+) Brak niespodzianek z leniwym ładowaniem poza transakcją (`open-in-view=false`).
- (−) Dwie krótkie transakcje w analizie zamiast jednej — przy tym profilu (I/O na
  jednym alercie) bez znaczenia wydajnościowego, a zysk w czytelności semantyki.

## Rozważane alternatywy

- **Transakcyjność na repozytoriach (domyślna Springa)** — niejawna, rozproszona,
  trudniejsza do opisania w review; odrzucona na rzecz granicy w serwisie.
- **`analyse` jako jedna transakcja read+write** — opisane wyżej; odrzucone, bo zaciera
  własność transakcji przez zapis alertu i model idempotencji.
- **`open-in-view=true` (domyślne Boota)** — wygodne, ale ukrywa granice i zachęca do
  leniwego ładowania w widoku; odrzucone.
