# ADR-0011: Identyfikatory — UUIDv7 generowane w aplikacji

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

Encje potrzebują identyfikatorów nadawanych **przed** zapisem (publikacja zdarzeń,
`Location` w odpowiedziach, brak round-tripu po sekwencję). Losowe UUIDv4 jako klucz
główny degradują lokalność indeksu B-tree (wstawienia w losowe strony).

## Decyzja

Identyfikatory typu **UUIDv7** (RFC 9562: 48-bitowy timestamp ms + losowość), generowane
w aplikacji przez mały, samodzielny generator (`Uuids.v7()`, ~20 linii, pokryty testem
monotoniczności), przypisywane w konstruktorze encji — encja jest kompletna od urodzenia.

## Uzasadnienie

- Czasowo-rosnące identyfikatory = wstawienia na „prawym końcu" indeksu (lokalność jak
  sekwencja, globalna unikalność jak UUID).
- Generacja w aplikacji uniezależnia od mechanizmów bazy (H2 dziś, PostgreSQL jutro)
  i upraszcza testy (id znane przed `save`).
- JDK 25 nie ma jeszcze wbudowanego v7 (`UUID.randomUUID()` to v4) — własna implementacja
  RFC 9562 to 20 świadomych linii zamiast zależności.

## Konsekwencje

- (+) Przyjazne indeksom, sortowalne po czasie utworzenia, bezpieczne do logowania.
- (−) Timestamp w identyfikatorze ujawnia czas utworzenia rekordu — w tej domenie
  nieistotne (czas i tak jest atrybutem jawnym).

## Rozważane alternatywy

- **UUIDv4 (`UUID.randomUUID()`)** — najprostsze, ale fragmentacja indeksów; odrzucone.
- **Sekwencje bazy (`IDENTITY`/`SEQUENCE`)** — id dopiero po zapisie, sprzężenie z bazą; odrzucone.
- **Biblioteka zewnętrzna (uuid-creator)** — zależność dla 20 linii kodu; odrzucone.
