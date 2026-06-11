# ADR-0006: Asynchroniczna analiza — zdarzenia aplikacyjne `AFTER_COMMIT` na wirtualnych wątkach

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

PDF (§4.3): po zapisaniu transakcji publikowany jest event `{eventId, businessId,
transactionId}`, a **osobny komponent** decyduje o utworzeniu alertu. PDF dopuszcza
zaślepkę („komentarz i log do konsoli"). Cel projektu obejmuje praktyczne użycie
wirtualnych wątków.

## Decyzja

1. `TransactionService` po zapisie publikuje rekord `TransactionRegisteredEvent(eventId,
   businessId, transactionId)` przez `ApplicationEventPublisher` — kształt ładunku 1:1 z PDF.
2. Odbiorca: `TransactionAnalysisListener` w osobnym pakiecie `detection`, adnotowany
   `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`.
3. Wykonanie asynchroniczne na **wirtualnych wątkach**: `spring.threads.virtual.enabled=true`
   (obejmuje też pulę żądań Tomcata) — analiza to I/O-bound praca na bazie, idealny profil
   dla Project Loom.

## Uzasadnienie

- `AFTER_COMMIT` gwarantuje, że analiza widzi **utrwaloną** transakcję — brak alertów-widm
  po rollbacku i brak wyścigu „event przed danymi".
- Rejestracja transakcji nie czeka na analizę (wymaganie asynchroniczności spełnione
  semantycznie, nie tylko deklaratywnie).
- Mechanizm in-app to świadomy środek skali zadania: **więcej niż dozwolona zaślepka,
  mniej niż infrastruktura brokera**.

## Konsekwencje

- (+) Klient API otrzymuje odpowiedź natychmiast po commicie; analiza w tle.
- (+) Brak zewnętrznej infrastruktury — uruchomienie pozostaje jednokrokowe (NFR-01).
- (−) **Brak trwałości zdarzeń**: awaria JVM między commitem a analizą gubi analizę tej
  transakcji. W produkcji: wzorzec **Transactional Outbox** + broker (Kafka/RabbitMQ) +
  relay — zdarzenie zapisywane w tej samej transakcji DB co dane, publikowane osobno.
  Granica modułu (`detection` reaguje na zdarzenie, nie na wywołanie serwisu) sprawia,
  że ta ewolucja nie zmienia logiki reguł.
- (−) Test integracyjny musi czekać na efekt asynchroniczny — rozwiązane biblioteką
  Awaitility (bez `Thread.sleep`).

## Rozważane alternatywy

- **Zaślepka (log do konsoli)** — dozwolona przez PDF; odrzucona, bo gasi sens §4.3
  (osobny komponent + decyzja o alercie) i wartość edukacyjną.
- **Analiza synchroniczna w transakcji zapisu** — blokuje klienta, sprzeczna z §4.3; odrzucona.
- **Kafka/RabbitMQ + Outbox** — właściwe produkcyjnie; tu nieproporcjonalna złożoność
  operacyjna (NFR-01); opisane jako ścieżka ewolucji.
