# ADR-0009: Kontrakt REST — `ProblemDetail` (RFC 9457), kody statusów, paginacja list

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
   - `422` (Unprocessable Content) — żądanie poprawne składniowo, ale łamiące spójność
     biznesową (np. `businessId` transakcji ≠ `businessId` klienta, ADR-0003),
   - `500` — nieoczekiwany błąd: catch-all `@ExceptionHandler(Exception.class)` zwraca
     **zsanityzowany** `ProblemDetail` z `correlationId`; pełny stack trafia wyłącznie do
     logu serwera (zero wycieku detali, komunikatów ani SQL). Utrzymuje to kontrakt RFC 9457
     end-to-end zamiast domyślnej strony błędu kontenera serwletów.
3. `POST /api/transactions` zwraca **`201 Created`** + nagłówek `Location`. Zasób jest
   utrwalony synchronicznie; asynchroniczna jest wyłącznie **analiza** (osobny zasób:
   alert). `202 Accepted` kłamałoby — sugerowałoby, że sama transakcja może się nie zapisać.
4. **Listy są paginowane** kopertą `PageResponse` (`content` + `page`/`size`/
   `totalElements`/`totalPages`) — rekord, a nie `PageImpl` (którego kształt JSON Spring
   sam odradza serializować). Rozmiar strony jest **twardo ograniczony** (`Pages`,
   `MAX_SIZE = 100`), więc klient nie wciągnie całej tabeli żądaniem `?size=1000000`.
   Sort jest stały (najnowsze pierwsze); ekspozycja parametru `sort` oraz paginacja
   keyset/cursor to ścieżki ewolucji.
5. Dokumentacja kontraktu: springdoc-openapi (Swagger UI) generowana z kodu.

## Uzasadnienie

`ProblemDetail` jest natywny w Spring Framework 6+/Boot 3+ i eliminuje autorskie formaty
błędów. Rozróżnienie 400/422 oddziela „nie rozumiem żądania" od „rozumiem, ale to
narusza reguły domeny" — istotne dla klientów automatycznych.

## Konsekwencje

- (+) Frontend ma jeden parser błędów (łącznie z 500); komunikaty trafiają wprost do UI.
- (+) Kontrakt widoczny i testowalny w Swagger UI bez czytania kodu.
- (+) Listy nie wywrócą aplikacji przy dużym zbiorze — rozmiar strony jest ograniczony.
- (−) Klient list musi obsłużyć kopertę `PageResponse` zamiast surowej tablicy.
- (−) Decyzja `201 vs 202` odbiega od części przykładów spotykanych w sieci — uzasadnienie
  wyżej jest częścią dokumentacji właśnie dlatego.

## Rozważane alternatywy

- **Własny format błędów** — reinwencja koła; odrzucone.
- **`202 Accepted` dla transakcji** — semantycznie fałszywe (zapis jest synchroniczny); odrzucone.
