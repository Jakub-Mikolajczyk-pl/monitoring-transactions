# ADR-0004: Pieniądze: `BigDecimal` + jawna waluta ISO 4217; próg bez przeliczeń FX

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

Transakcja niesie kwotę (`1500.75`) i walutę (`"PLN"`). Reguła AML porównuje kwotę
z progiem konfiguracyjnym („np. 2000 zł"). Reprezentacja pieniędzy w systemie bankowym
nie może wprowadzać błędów zaokrągleń.

## Decyzja

1. Kwota: `BigDecimal`, kolumna `DECIMAL(19,2)`, skala 2, zaokrąglanie `HALF_UP`
   tam, gdzie kiedykolwiek będzie potrzebne (obecnie brak arytmetyki — tylko porównania
   `compareTo`).
2. Waluta: osobna kolumna `CHAR(3)`, walidowana względem ISO 4217 (`java.util.Currency`).
3. Próg `SUSPICIOUS_AMOUNT` porównuje **samą kwotę**, niezależnie od waluty —
   **świadome uproszczenie** zgodne z duchem zadania.

## Uzasadnienie

- `double`/`float` są niedopuszczalne dla pieniędzy (binarna reprezentacja ułamków
  dziesiętnych); `BigDecimal` z jawną skalą to standard sektora.
- Porównania progowe wykonujemy `compareTo` (nie `equals` — `2000.0` vs `2000.00`).
- Pełna poprawność wielowalutowa wymagałaby kursów FX lub progów per waluta — poza
  zakresem zadania; PDF sam podaje próg w złotych przy transakcjach z polem waluty.

## Konsekwencje

- (+) Zero błędów reprezentacji; jednoznaczne porównania.
- (−) Brak operatorów arytmetycznych (`.add()`, `.compareTo()`) — akceptowalny koszt.
- (−) Transakcja `2500 EUR` i `2500 PLN` przekraczają próg tak samo — odnotowane
  w README jako uproszczenie; ewolucja: `Map<Currency, BigDecimal>` progów lub
  normalizacja FX na dedykowanym kursie.

## Rozważane alternatywy

- **`long` w groszach** — szybkie, ale nieczytelne przy wielu walutach (różne wykładniki)
  i przy integracjach JSON; odrzucone.
- **Typ `Money` (JavaMoney/Moneta)** — dodatkowa zależność nieuzasadniona zakresem; odrzucone.
