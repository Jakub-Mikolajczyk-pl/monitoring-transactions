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
2. Współdzielenie stylów między komponentami przez **Constructable Stylesheets**
   (`adoptedStyleSheets`) — wspólny arkusz tokenów (kolory, typografia) bez duplikacji
   i bez wycieków CSS.
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
izolację stylów tam, gdzie ma to wartość (komponenty wielokrotnego użytku), a
`adoptedStyleSheets` rozwiązuje jego klasyczną wadę — duplikację wspólnych stylów.

## Konsekwencje

- (+) `git clone` → `./mvnw spring-boot:run` → działający UI; zero instalacji node/npm.
- (+) Każdy komponent to jeden samodzielny plik — łatwy do code review.
- (−) Brak reaktywności frameworka — odświeżanie list jawne (po zdarzeniach); przy tej
  skali zaleta, nie wada.
- (−) Brak testów jednostkowych UI (wymagałyby narzędzi node) — pokryte scenariuszem
  manualnym w README i smoke testem; odnotowane jako uproszczenie.

## Rozważane alternatywy

- **Lit / Stencil** — wygodniejsze, ale wprowadzają toolchain node wbrew prostocie zadania.
- **Framework SPA (React/Vue/Angular)** — sprzeczny z narzuconą technologią; odrzucone.
- **Style inline w każdym komponencie** (odrzucony szkic planu) — duplikacja designu
  w każdym Shadow DOM; zastąpione `adoptedStyleSheets`.
