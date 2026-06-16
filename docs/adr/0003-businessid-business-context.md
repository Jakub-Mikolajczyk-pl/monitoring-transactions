# ADR-0003: `businessId` jako identyfikator kontekstu biznesowego

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

PDF nie definiuje semantyki pola `businessId`, a jest ono kluczowe: występuje w **każdej**
encji modelu domenowego i jest **wymaganym** filtrem wyszukiwania transakcji. Przesłanki
z przykładów zadania:

1. Ta sama wartość `b001` pojawia się na `Customer`, `Transaction` **i** `Alert` —
   trzy „przypadkowo" identyczne identyfikaty własne są nieprawdopodobne; wartość się propaguje.
2. Przykład eventu używa `"businessId": "BANK_A"` — nazwa sugeruje jednostkę organizacyjną.
3. Wyszukiwanie: `businessId` **wymagany**, `customerId` **opcjonalny**. Gdyby `businessId`
   identyfikował pojedynczą transakcję, dodatkowe filtry (klient, zakres dat) nie miałyby sensu.
   Filtr wymagany + filtry zawężające = klasyczne wyszukiwanie w obrębie partycji.

## Decyzja

`businessId` = **identyfikator kontekstu biznesowego** (np. bank, linia biznesowa, system
źródłowy), w którym rejestrowane są dane:

- klient otrzymuje `businessId` przy rejestracji (bez unikalności — wiele rekordów dzieli wartość),
- transakcja przyjmuje `businessId` w żądaniu i system **waliduje zgodność** z `businessId`
  klienta — niezgodność to błąd spójności danych → **HTTP 422 (Unprocessable Content)**,
- alert **dziedziczy** `businessId` z transakcji (denormalizacja pod wyszukiwanie i audyt).

## Uzasadnienie

To jedyna interpretacja spójna ze wszystkimi trzema przesłankami naraz. Dodatkowo otwiera
naturalną ścieżkę do multi-tenancy — realnego wymagania systemów AML obsługujących wiele
jednostek.

## Konsekwencje

- (+) Wyszukiwanie transakcji ma sens biznesowy (analityk pracuje w obrębie jednostki).
- (+) Indeks złożony `(business_id, transaction_date)` wspiera główne zapytanie filtrów.
- (+) Walidacja zgodności klient–transakcja chroni przed cichym pomieszaniem kontekstów.
- (−) Interpretacja może odbiegać od intencji autora zadania — dlatego jest **jawnie
  udokumentowana** tu i w README; zmiana interpretacji dotyka wyłącznie walidacji i indeksów.

## Rozważane alternatywy

- **Zewnętrzny, unikalny identyfikator każdego rekordu** (przyjęty w odrzuconym szkicu planu):
  sprzeczny z przesłankami 1 i 3 — wymagany filtr wyszukiwania zwracałby zawsze ≤ 1 rekord.
- **Brak walidacji zgodności klient–transakcja:** prostsze, ale pozwala na niespójne dane
  w obrębie agregatu analizy — w domenie AML nieakceptowalne.
