# ADR-0009: Kontrakt REST — `ProblemDetail` (RFC 9457), kody statusów, brak paginacji

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

API konsumuje własny frontend i oceniający (curl/Swagger). Kontrakt błędów musi być
spójny i samoopisujący; kody statusów — semantycznie poprawne.

## Decyzja

1. **Każdy** błąd zwracany jest jako `application/problem+json` (RFC 9457) przez wspólny
   `@RestControllerAdvice`; walidacja pól dołącza tablicę `errors[{field, message}]`.
2. Mapowanie statusów:
   - `400` — błąd składniowy/walidacyjny żądania (w tym brak wymaganego `businessId`),
   - `404` — brak zasobu (klient, transakcja, alert),
   - `409` — konflikt wersji alertu (ADR-0008),
   - `422` — żądanie poprawne składniowo, ale łamiące spójność biznesową
     (np. `businessId` transakcji ≠ `businessId` klienta, ADR-0003).
3. `POST /api/transactions` zwraca **`201 Created`** + nagłówek `Location`. Zasób jest
   utrwalony synchronicznie; asynchroniczna jest wyłącznie **analiza** (osobny zasób:
   alert). `202 Accepted` kłamałoby — sugerowałoby, że sama transakcja może się nie zapisać.
4. Listy bez paginacji — świadome uproszczenie skali zadania; ścieżka: `Pageable` +
   nagłówki/koperta strony (odnotowane w README).
5. Dokumentacja kontraktu: springdoc-openapi (Swagger UI) generowana z kodu.

## Uzasadnienie

`ProblemDetail` jest natywny w Spring Framework 6+/Boot 3+ i eliminuje autorskie formaty
błędów. Rozróżnienie 400/422 oddziela „nie rozumiem żądania" od „rozumiem, ale to
narusza reguły domeny" — istotne dla klientów automatycznych.

## Konsekwencje

- (+) Frontend ma jeden parser błędów; komunikaty trafiają wprost do UI.
- (+) Kontrakt widoczny i testowalny w Swagger UI bez czytania kodu.
- (−) Decyzja `201 vs 202` odbiega od części przykładów spotykanych w sieci — uzasadnienie
  wyżej jest częścią dokumentacji właśnie dlatego.

## Rozważane alternatywy

- **Własny format błędów** — reinwencja koła; odrzucone.
- **`202 Accepted` dla transakcji** — semantycznie fałszywe (zapis jest synchroniczny); odrzucone.
