# Mapa nowości Javy użytych w projekcie (Java 14 → 25)

Cel: każdy feature ma wskazane **konkretne miejsce w kodzie**, żeby dało się go obejrzeć
w działaniu i obronić na rozmowie. Wersja w nawiasie = finalizacja w JDK.

## Język

| Feature (JEP, wersja) | Gdzie w projekcie | Po co tutaj |
|---|---|---|
| **Rekordy** (JEP 395, 16) | wszystkie DTO ([`CustomerRequest`](../src/main/java/pl/jakubmikolajczyk/monitoring/customer/dto/CustomerRequest.java), [`AlertDetailsResponse`](../src/main/java/pl/jakubmikolajczyk/monitoring/alert/dto/AlertDetailsResponse.java)…), event [`TransactionRegisteredEvent`](../src/main/java/pl/jakubmikolajczyk/monitoring/transaction/TransactionRegisteredEvent.java) | niemutowalne nośniki danych bez boilerplate'u; jasny rozdział: encje JPA = klasy, kontrakty = rekordy |
| **Kompaktowy konstruktor rekordu** | [`TransactionSearchCriteria`](../src/main/java/pl/jakubmikolajczyk/monitoring/transaction/TransactionSearchCriteria.java) | inwarianty (wymagany `businessId`, poprawny zakres dat) przy danych, które chronią |
| **Sealed types** (JEP 409, 17) | [`AmlRule`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/AmlRule.java) (`permits` = audytowalny katalog reguł), [`RuleResult`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/RuleResult.java) | zamknięta hierarchia = kompilator pilnuje kompletności obsługi |
| **Pattern matching dla `switch` + wyczerpujący switch** (JEP 441, 21) | [`AmlRuleEngine.analyse`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/AmlRuleEngine.java) | brak `default`: nowy wariant `RuleResult` to błąd kompilacji, nie cicha luka |
| **Record patterns** (JEP 440, 21) | [`SuspiciousAmountRuleTest`](../src/test/java/pl/jakubmikolajczyk/monitoring/detection/SuspiciousAmountRuleTest.java) — `instanceof RuleResult.Violation(var reason, var detail)` | dekonstrukcja wyniku w jednym kroku |
| **Unnamed variables `_`** (JEP 456, 22) | [`AmlRuleEngine`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/AmlRuleEngine.java) (`case RuleResult.Clean _`), [`TransactionAnalysisListener`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/TransactionAnalysisListener.java) (`catch (… _)`) | nieużywana zmienna oznaczona wprost, bez wymyślania nazwy |
| **Switch expressions** (JEP 361, 14) | [`Decision.resultingStatus()`](../src/main/java/pl/jakubmikolajczyk/monitoring/alert/Decision.java) | totalne mapowanie decyzja → status |
| **Text blocks** (JEP 378, 15) | [`TransactionRepository.countInWindow`](../src/main/java/pl/jakubmikolajczyk/monitoring/transaction/TransactionRepository.java) (JPQL), wszystkie JSON-y w testach integracyjnych | czytelne zapytania i ładunki bez ucieczek |
| **`var`** (JEP 286, 10) | konsekwentnie tam, gdzie typ widać z prawej strony | mniej szumu, bez utraty czytelności |
| **`String.formatted`** (15) | komunikaty błędów i naruszeń (np. [`HighFrequencyRule`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/HighFrequencyRule.java)) | zwięzła interpolacja |
| **Markdown doc comments `///`** (JEP 467, 23) | komentarze dokumentacyjne w całym kodzie | nowa składnia Javadoc; komentarze tłumaczą „dlaczego" |

## Biblioteka standardowa i runtime

| Feature | Gdzie w projekcie | Po co tutaj |
|---|---|---|
| **Wirtualne wątki** (JEP 444, 21; pinning na `synchronized` usunięty — JEP 491, 24) | `spring.threads.virtual.enabled=true` ([application.properties](../src/main/resources/application.properties)), [`AsyncConfig`](../src/main/java/pl/jakubmikolajczyk/monitoring/common/config/AsyncConfig.java), log wątku w [`TransactionAnalysisListener`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/TransactionAnalysisListener.java) | obsługa HTTP i analiza AML (I/O-bound: JDBC) bez strojenia pul wątków; w logu widać `VirtualThread[#…]` |
| **`Stream.mapMulti`** (16) | [`AmlRuleEngine`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/AmlRuleEngine.java) | filtrowanie + rzutowanie wariantów sealed w jednym przejściu |
| **`Stream.toList()`** (16) | wszystkie mapowania na DTO | krótsze i niemutowalne |
| **Sequenced Collections — `getFirst()`** (JEP 431, 21) | testy ([`TransactionEventTest`](../src/test/java/pl/jakubmikolajczyk/monitoring/transaction/TransactionEventTest.java), `DetectionIntegrationTest`) | intencja „pierwszy element" wprost |
| **`InstantSource`** (17) | [`TimeConfig`](../src/main/java/pl/jakubmikolajczyk/monitoring/common/time/TimeConfig.java) + wstrzykiwany do serwisów i fabryk encji | węższy interfejs niż `Clock`; czas w pełni testowalny |
| **UUIDv7 wg RFC 9562** (własna implementacja — JDK 25 ma tylko v4) | [`Uuids`](../src/main/java/pl/jakubmikolajczyk/monitoring/common/id/Uuids.java) + [test](../src/test/java/pl/jakubmikolajczyk/monitoring/common/id/UuidsTest.java) | id rosnące w czasie = lokalność indeksów B-tree; 20 świadomych linii zamiast zależności |

## Spring Boot 4 / ekosystem (przy okazji)

| Element | Gdzie |
|---|---|
| Modularne startery Boot 4 (`spring-boot-starter-webmvc`, osobne startery testowe per technologia) | [pom.xml](../pom.xml) |
| Nowe pakiety klas testowych (`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`) | wszystkie testy integracyjne |
| **`MockMvcTester`** (AssertJ-first MockMvc) | wszystkie testy API |
| `@ConfigurationProperties` na rekordzie + walidacja na starcie | [`AmlProperties`](../src/main/java/pl/jakubmikolajczyk/monitoring/detection/AmlProperties.java) |
| `ProblemDetail` (RFC 9457) | [`GlobalExceptionHandler`](../src/main/java/pl/jakubmikolajczyk/monitoring/common/web/GlobalExceptionHandler.java) |
| Mockito jako statyczny `-javaagent` (przyszłe JDK zablokują self-attach) | [pom.xml](../pom.xml), konfiguracja surefire |

## Czego świadomie NIE użyłem (i dlaczego)

| Feature | Powód |
|---|---|
| **Scoped Values** (JEP 506, 25) | brak naturalnego przypadku: nie propagujemy kontekstu przez wątki poza eventem niosącym identyfikatory |
| **Structured Concurrency** (preview w 25) | API wciąż preview; pojedyncze zadanie analizy nie wymaga orkiestracji podzadań |
| **Flexible constructor bodies** (JEP 513, 25) | encje budują fabryki statyczne — walidacja mieszka tam, nie przed `super(...)` |
| **Module imports / compact source files** (JEP 511/512, 25) | wygodne w skryptach; w aplikacji Spring jawne importy niosą informację |
| **String templates** | wycofane z JDK (po preview w 21/22) — przykład, że nie każdy preview-feature dożywa finału |
