# ADR-0008: Decyzje analityka — append-only + blokowanie optymistyczne (HTTP 409)

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

PDF (§4.5): decyzja `APPROVE`/`REJECT`, **każda zapisana jako nowy wpis**. W realnym
zespole dwóch analityków może obsługiwać ten sam alert równocześnie — klasyczny
**lost update** na statusie alertu.

## Decyzja

1. `AlertDecision` jest **append-only** (brak edycji/usuwania); status alertu =
   odwzorowanie ostatniej decyzji (`APPROVE → APPROVED`, `REJECT → REJECTED`).
   Kolejne decyzje są dozwolone (re-review) — pełna historia pozostaje w tabeli.
2. Encja `Alert` ma pole `@Version`. Żądanie `POST /api/alerts/{id}/decisions` niesie
   `alertVersion` — wersję, którą analityk **widział** przy podejmowaniu decyzji.
3. Niezgodność wersji (ktoś zdecydował w międzyczasie) → **HTTP 409 Conflict**
   z `ProblemDetail` instruującym odświeżenie widoku. Wyścig na samym zapisie
   (`OptimisticLockingFailureException`) mapowany identycznie.

## Uzasadnienie

Wymóg „każda decyzja jako nowy wpis" wprost sugeruje audyt append-only. Wersja w żądaniu
przenosi ochronę przed lost update na poziom **biznesowy**: analityk B, decydując na
podstawie nieaktualnego widoku, dostaje jednoznaczny sygnał zamiast cichego dopisania
sprzecznej decyzji. To minimalny, standardowy mechanizm (JPA `@Version`) — bez blokad
pesymistycznych i ich ryzyk (deadlock, blokowanie odczytów).

## Konsekwencje

- (+) Brak utraconych aktualizacji; konflikt jest jawny i obsłużony w UI (komunikat + odświeżenie).
- (+) Test deterministyczny: zapis ze **stale** wersją musi zwrócić 409 (bez wyścigów w teście).
- (−) Klient API musi przekazywać `alertVersion` (pole wystawiane w `GET /api/alerts/{id}`).

## Rozważane alternatywy

- **ETag/If-Match (RFC 9110)** — semantycznie to samo na nagłówkach; odrzucone dla
  czytelności prostego klienta fetch (pole w body jest widoczne w przykładach i Swaggerze).
- **Blokowanie pesymistyczne** — nadmiarowe, ryzyko zakleszczeń; odrzucone.
- **Brak kontroli współbieżności** — cichy lost update statusu; w domenie decyzji
  audytowych niedopuszczalne.
