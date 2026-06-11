# Konwencja pracy z Gitem

Zadanie wprost premiuje historię commitów („duży plus"). Historia tego repozytorium jest
projektowana tak, by **opowiadała proces inżynierski**: analiza → decyzje → szkielet →
pionowe przyrosty funkcjonalne → dokumentacja końcowa.

## Model gałęzi

**Trunk-based, jedna gałąź `main`, historia liniowa.** Projekt jednoosobowy o krótkim
horyzoncie — feature branche i merge commity dodałyby szumu bez wartości. Atomowość
zapewniają same commity, nie gałęzie. (W zespole: krótkie gałęzie + PR + squash/rebase.)

## Format komunikatów: Conventional Commits 1.0.0

```
<typ>(<zakres>): <opis w trybie rozkazującym, ang., ≤ 72 znaki>

<treść: DLACZEGO ta zmiana, decyzje i kompromisy — nie parafraza diffa>

Refs: REQ-xx[, REQ-yy | ADR-nnnn]
```

| Typ | Zastosowanie |
|---|---|
| `feat` | nowa funkcjonalność widoczna dla użytkownika/API |
| `fix` | poprawka błędu |
| `test` | testy niezwiązane z nową funkcją (np. reguły architektury) |
| `docs` | dokumentacja |
| `build` | system budowania, zależności, konfiguracja projektu |
| `ci` | potoki CI |
| `refactor` | zmiana struktury bez zmiany zachowania |
| `chore` | porządki nie dotykające kodu produkcyjnego ani testów |

**Zakresy:** `customer`, `transaction`, `detection`, `alert`, `ui`, `api`, `arch`.

## Zasady

1. **Jeden commit = jedna decyzja/przyrost**; projekt buduje się i przechodzi testy po
   **każdym** commicie (historia bisect-owalna).
2. Treść commita tłumaczy **dlaczego** — „co" widać w diffie.
3. Stopka `Refs:` wiąże commit z wymaganiem z [requirements.md](requirements.md)
   (traceability wymaganie → commit → kod → test).
4. Język: komunikaty po angielsku (standard branżowy), dokumentacja po polsku
   (adresat: polski zespół rekrutacyjny).
5. Commity tworzone z asystą AI niosą stopkę `Co-Authored-By` — spójnie z jawną
   deklaracją użycia AI w README (wymóg PDF §7).

## Przykład

```
feat(detection): evaluate suspicious-amount rule and raise alerts

Rules run asynchronously after commit so registration latency stays flat.
A single alert per transaction (DB-enforced) keeps the analyst queue free
of duplicates when an event is redelivered.

Refs: REQ-08, REQ-09, ADR-0006, ADR-0007
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>
```
