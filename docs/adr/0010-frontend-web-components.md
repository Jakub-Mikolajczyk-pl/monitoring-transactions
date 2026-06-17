# ADR-0010: Frontend — czyste Web Components bez bundlera, serwowane przez Spring

- **Status:** zaakceptowany
- **Data:** 2026-06-12

## Kontekst

PDF narzuca technologię: „Web Components (JavaScript)" i wymaga frontendu **użytecznego**
(§5): listy klientów/transakcji/alertów, filtry, szczegóły alertu, historia decyzji,
formularz decyzji.

## Decyzja

1. Czyste standardy platformy: **Custom Elements + Shadow DOM + `<template>`**,
   ES modules ładowane natywnie przez przeglądarkę — **zero kroku budowania**.
2. Style w **osobnych plikach `.css`** (`static/styles/`), ładowane do każdego Shadow DOM
   przez `<link rel="stylesheet">` — wspólny `components.css` plus opcjonalny plik per
   komponent. CSS jest poza JavaScriptem (edytowalny bez dotykania logiki); tokeny
   z `:root` (`app.css`) przenikają granicę Shadow DOM, więc nic nie deklarujemy ponownie.
3. Routing hashowy (`#/customers`, `#/transactions`, `#/alerts`, `#/alerts/{id}`)
   w komponencie powłoki; komunikacja w górę przez `CustomEvent`, w dół przez
   atrybuty/właściwości.
4. Warstwa API: jeden moduł `api.js` (wrapper `fetch`) rozumiejący `application/problem+json`
   — błędy backendu wyświetlane użytkownikowi 1:1, w tym scenariusz 409 (konflikt decyzji).
5. Pliki w `src/main/resources/static` — frontend serwuje ta sama aplikacja Spring
   (jeden proces, jeden port, zero CORS).
6. Priorytet wizualny: **czytelna konsola operacyjna** (czytelne tabele, statusy,
   formularze z walidacją), nie efekty dekoracyjne. Użyteczność jest wymaganiem PDF;
   estetyka ma jej służyć.

## Uzasadnienie

Brak bundlera i frameworka usuwa całą klasę problemów (wersje, build, konfiguracja)
i pokazuje znajomość **platformy**, o którą wprost pyta zadanie. Shadow DOM daje
izolację stylów tam, gdzie ma to wartość (komponenty wielokrotnego użytku); wspólny
`components.css` ładowany przez `<link>` w każdym cieniu eliminuje jego klasyczną wadę
(duplikację stylów), a CSS pozostaje w plikach, nie w stringach JS.

## Konsekwencje

- (+) `git clone` → `./mvnw spring-boot:run` → działający UI; zero instalacji node/npm
  dla samej aplikacji.
- (+) Każdy komponent to jeden samodzielny plik — łatwy do code review.
- (+) Style są w plikach `.css` (`<link>` w Shadow DOM), nie w stringach JS — edycja
  wyglądu bez dotykania kodu.
- (−) Brak reaktywności frameworka — odświeżanie list jawne (po zdarzeniach); przy tej
  skali zaleta, nie wada.
- (−) Testy komponentów są w przeglądarce (`/test/index.html`), nie w `mvnw verify`/CI — patrz
  „Testowanie UI" niżej.

## Testowanie UI

Komponenty mają **testy jednostkowe** w bezzależnościowym harnessie
(`src/main/resources/static/test/`): montują custom element, stubują `fetch` i
sprawdzają render oraz zdarzenia (np. `<customer-form>` emituje `customer-registered`
i pokazuje błędy pól z `problem+json`; `<alerts-view>` renderuje wiersze z koperty
strony). Uruchamiane przez otwarcie `/test/index.html` w przeglądarce. Świadome ograniczenie:
nie wpinają się w mavenowe CI (to wymagałoby toolchainu node). Ścieżka produkcyjna:
te same specyfikacje pod Playwright/`@web/test-runner` jako osobny krok CI. Pliki
testowe są serwowane ze statycznych zasobów (w produkcji wykluczone z budowania).

Uzupełnieniem review jest **wizualne demo Playwright** w `tests/demo/`. Nie jest częścią
CI: uruchamia headed Chromium, spowalnia interakcje i pokazuje nakładki opisujące kolejne
kroki pracy analityka. Demo używa żywego backendu i realnego HTTP, ale jego celem jest
pokazanie przepływu użytkownikowi, a nie zastąpienie testów integracyjnych.

## Rozważane alternatywy

- **Lit / Stencil** — wygodniejsze, ale wprowadzają toolchain node wbrew prostocie zadania.
- **Framework SPA (React/Vue/Angular)** — sprzeczny z narzuconą technologią; odrzucone.
- **Style w stringach JS / `adoptedStyleSheets`** (wcześniejsza wersja) — działało, ale
  mieszało CSS z logiką; zastąpione plikami `.css` ładowanymi przez `<link>` w Shadow DOM.
