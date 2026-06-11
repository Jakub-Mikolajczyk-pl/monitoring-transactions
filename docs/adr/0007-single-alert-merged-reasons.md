# ADR-0007: Jeden alert na transakcję, scalone powody, idempotentna detekcja

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

Transakcja może naruszyć kilka reguł naraz (kwota **i** częstotliwość). Model `Alert`
w PDF ma pojedyncze pole `reason`. Analiza działa asynchronicznie, więc trzeba założyć
możliwość ponownego przetworzenia tego samego zdarzenia.

## Decyzja

1. Silnik (`AmlRuleEngine`) ewaluuje **wszystkie** zarejestrowane reguły (`List<AmlRule>`
   wstrzykiwana przez Springa) i scala naruszenia w **jeden** alert:
   `reason = "SUSPICIOUS_AMOUNT,HIGH_FREQUENCY"`.
2. Unikalność: ograniczenie `UNIQUE (transaction_id)` na tabeli `alerts`. Powtórne
   przetworzenie zdarzenia kończy się przechwyconym konfliktem i logiem — **idempotencja
   na poziomie bazy**, nie pamięci.
3. Wynik reguły modeluje zamknięta hierarchia: `sealed interface RuleResult` z rekordami
   `Violation(reason, detail)` / `Clean()` — silnik konsumuje ją wyczerpującym
   `switch` z record patterns (kompilator pilnuje obsługi każdego wariantu).

## Uzasadnienie

- Jeden alert per transakcja = mniej szumu dla analityka i zgodność z modelem PDF.
- Gwarancja unikalności musi leżeć w bazie: listener może zostać wywołany ponownie,
  a dwie analizy mogą biec współbieżnie.
- `sealed` + record patterns to nie ozdobnik: katalog wyników reguł jest **domenowo
  zamknięty** (jak katalog reguł w banku — artefakt podlegający governance), a
  wyczerpujący `switch` zamienia błąd logiczny w błąd kompilacji.

## Konsekwencje

- (+) Deterministyczny, odporny na duplikaty zapis alertów.
- (+) Dodanie nowej reguły = nowa klasa implementująca `AmlRule` (rejestracja przez DI).
- (−) Pole `reason` jako string CSV jest kompromisem pod model PDF; ewolucja: tabela
  `alert_reasons` 1:N (odnotowane w README).

## Rozważane alternatywy

- **Alert per naruszona reguła** — szum operacyjny, niezgodny z pojedynczym `reason` w PDF.
- **Dedup w pamięci (sprawdzenie „czy istnieje")** — TOCTOU przy współbieżności; jako
  jedyne zabezpieczenie odrzucone (zostaje jako szybka ścieżka przed insertem).
