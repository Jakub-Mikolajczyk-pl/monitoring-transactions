# ADR-0005: Transakcje niemutowalne + rozdzielenie czasu biznesowego i systemowego

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

Transakcja finansowa jest **faktem historycznym**. System AML buduje na transakcjach
wnioski (alerty, decyzje) — modyfikacja lub usunięcie transakcji po fakcie unieważniłaby
ślad audytowy całego łańcucha.

## Decyzja

1. Brak endpointów `PUT`/`PATCH`/`DELETE` dla transakcji; korekta błędu = transakcja
   kompensująca (storno) — poza zakresem zadania, ścieżka odnotowana.
2. Kolumny encji oznaczone `updatable = false`; encja nie udostępnia setterów.
3. Dwa wymiary czasu (minimalna bitemporalność):
   - `transaction_date` — **czas biznesowy**, podawany przez klienta API (kiedy transakcja
     zaszła w świecie),
   - `created_at` — **czas systemowy**, nadawany przez aplikację (kiedy system się o niej
     dowiedział).
   Reguły AML operują na czasie biznesowym; audyt — na systemowym.

## Uzasadnienie

Niezmienność danych źródłowych to fundament audytu finansowego. Rozdzielenie wymiarów
czasu eliminuje klasyczny błąd: liczenie okien częstotliwości względem chwili analizy
(niedeterministyczne, zależne od opóźnień przetwarzania) zamiast względem chwili transakcji.

## Konsekwencje

- (+) Ślad audytowy nie do podważenia; dane bezpieczne do keszowania.
- (+) Reguła `HIGH_FREQUENCY` jest deterministyczna i w pełni testowalna (czas wstrzykiwany
  przez `InstantSource`).
- (−) Brak „edycji pomyłek" — wymaga storna (świadomy koszt domenowy).

## Rozważane alternatywy

- **Pełny CRUD na transakcjach** — łamie zasady audytu finansowego; odrzucone.
- **Soft delete** — nadal fałszuje obraz historii dla reguł częstotliwości; odrzucone.
