# Backend Test Case Documentation

## OfferController Test Cases

This section outlines the test cases for the `OfferController`, which handles incoming requests related to offers.

### Test Case Structure

*   **Test Case ID:** A unique identifier for the test case.
*   **Description:** A brief explanation of what the test case is verifying.
*   **Component/Module:** The specific part of the `OfferController` being tested (e.g., a specific endpoint or method).
*   **Scenario:** The specific situation or input being tested.
*   **Expected Behavior:** What the `OfferController` should do or return in this scenario.
*   **Test Code Reference:** Reference to the corresponding test code file or method.

---

### Implemented Test Cases in `OfferControllerTests.java`

* **Test Case ID:** OC_000
* **Description:** Verify that the Spring application context loads successfully.
* **Component/Module:** Spring Application Context
* **Scenario:** The application starts up.
* **Expected Behavior:** The Spring context should load without errors.
* **Test Code Reference:** `contextLoads`


* **Test Case ID:** OC_001
* **Description:** Verify that the controller can retrieve offers for a valid address.
* **Component/Module:** Offer Retrieval Endpoint - Service Integration
* **Scenario:** A GET request is made to `/api/offers` with valid `street`, `houseNumber`, `city`, and `plz` parameters, and mocked services return offers.
* **Expected Behavior:** The controller should return an HTTP status of **200 OK** and a Server-Sent Events (SSE) stream containing the expected offer data (e.g., "Byte" and "WebWunder").
* **Test Code Reference:** `testGetAllOffersWithValidAddress`


* **Test Case ID:** OC_002
* **Description:** Verify that the controller returns a bad request error for missing required address parameters.
* **Component/Module:** Offer Retrieval Endpoint - Request Parameter Validation
* **Scenario:** A GET request is made to `/api/offers` with a missing mandatory required parameter (e.g., `city`, `plz`, `land` is omitted).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `MissingServletRequestParameterException`. The response body should contain a message indicating the missing parameter.
* **Test Code Reference:** `testGetAllOffersWithMissingRequiredAddressParams_shouldReturnBadRequest`


* **Test Case ID:** OC_003
* **Description:** Verify that the controller returns an empty response when no offers are found by any service.
* **Component/Module:** Offer Retrieval Endpoint - Service Integration
* **Scenario:** A GET request is made to `/api/offers` with a valid address, but all mocked external services return an empty stream of offers.
* **Expected Behavior:** The controller should return an HTTP status of **200 OK** and an empty response body (an empty SSE stream).
* **Test Code Reference:** `testGetAllOffersWhenNoOffersFound`


* **Test Case ID:** OC_004
* **Description:** Verify how the controller handles simulated internal errors from external services.
* **Component/Module:** Offer Retrieval Endpoint - Service Integration / Error Handling
* **Scenario:** A GET request is made to `/api/offers` with a valid address, and one of the mocked external services is configured to throw a `RuntimeException`.
* **Expected Behavior:** The controller should return an HTTP status of **200 OK** (as per SSE specification, the stream remains open but might not send events for internal errors) and an empty response body, indicating that the service error did not lead to a direct HTTP error status from the controller's main flow.
* **Test Code Reference:** `testGetAllOffersWhenServiceReturnsError`


* **Test Case ID:** OC_005_01
* **Description:** Validate `street` parameter with invalid characters.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Pattern`)
* **Scenario:** A GET request is made to `/api/offers` with `street` containing invalid characters (e.g., `@#$`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and the `GlobalExceptionHandler` should return an HTTP status of 400 Bad Request. The response body should contain a message indicating "Invalid characters in street name".
* **Test Code Reference:** `testGetAllOffersWithInvalidCharactersInStreet`


* **Test Case ID:** OC_005_02
* **Description:** Validate `houseNumber` parameter with invalid characters.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Pattern`)
* **Scenario:** A GET request is made to `/api/offers` with `houseNumber` containing invalid characters (e.g., `@#`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "Invalid characters in house number".
* **Test Code Reference:** `testGetAllOffersWithInvalidHouseNumber`


* **Test Case ID:** OC_005_03
* **Description:** Validate `city` parameter with invalid characters.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Pattern`)
* **Scenario:** A GET request is made to `/api/offers` with `city` containing invalid characters (e.g., `@#$`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "Invalid characters in city name".
* **Test Code Reference:** `testGetAllOffersWithInvalidCity`


* **Test Case ID:** OC_005_04
* **Description:** Validate `plz` parameter with invalid characters/format.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Pattern`)
* **Scenario:** A GET request is made to `/api/offers` with `plz` containing invalid characters or an incorrect format (e.g., "123" which is too short and doesn't match the pattern).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "Invalid characters in postal code".
* **Test Code Reference:** `testGetAllOffersWithInvalidPlz`


* **Test Case ID:** OC_006_01
* **Description:** Validate `street` parameter when it's empty.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@NotBlank`)
* **Scenario:** A GET request is made to `/api/offers` with an empty `street` parameter.
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "must not be blank" for street.
* **Test Code Reference:** `testGetAllOffersWithEmptyStreet`


* **Test Case ID:** OC_006_02
* **Description:** Validate `street` parameter when its length exceeds the maximum allowed.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Size`)
* **Scenario:** A GET request is made to `/api/offers` with `street` longer than 100 characters.
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "size must be between 0 and 100" for street.
* **Test Code Reference:** `testGetAllOffersWithStreetTooLong`


* **Test Case ID:** OC_006_03
* **Description:** Validate `houseNumber` parameter when it's empty.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@NotBlank`)
* **Scenario:** A GET request is made to `/api/offers` with an empty `houseNumber` parameter.
* **Expected Behavior:** The controller should return an HTTP status of **200 OK**
* **Test Code Reference:** `testGetAllOffersWithEmptyHouseNumber_shouldReturnOk`


* **Test Case ID:** OC_006_04
* **Description:** Validate `houseNumber` parameter when its length exceeds the maximum allowed.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Size`)
* **Scenario:** A GET request is made to `/api/offers` with `houseNumber` longer than 10 characters.
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "size must be between 0 and 10" for house number.
* **Test Code Reference:** `testGetAllOffersWithHouseNumberTooLong`


* **Test Case ID:** OC_006_05
* **Description:** Validate `city` parameter when it's empty.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@NotBlank`)
* **Scenario:** A GET request is made to `/api/offers` with an empty `city` parameter.
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "must not be blank" for city.
* **Test Code Reference:** `testGetAllOffersWithEmptyCity`


* **Test Case ID:** OC_006_06
* **Description:** Validate `city` parameter when its length exceeds the maximum allowed.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@Size`)
* **Scenario:** A GET request is made to `/api/offers` with `city` longer than 100 characters.
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "size must be between 0 and 100" for city.
* **Test Code Reference:** `testGetAllOffersWithCityTooLong`

* **Test Case ID:** OC_006_07
* **Description:** Validate `plz` parameter when it's empty.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation (`@NotBlank`)
* **Scenario:** A GET request is made to `/api/offers` with an empty `plz` parameter.
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message in the response body should indicate "must not be blank" for plz.
* **Test Code Reference:** `testGetAllOffersWithEmptyPlz`


* **Test Case ID:** OC_007_01
* **Description:** Handle malformed JSON requests to the endpoint (only applicable for POST).
* **Component/Module:** Offer Retrieval Endpoint - HTTP Method
* **Scenario:** A POST request is made to `/api/offers` with a malformed JSON request body.
* **Expected Behavior:** Since `/api/offers` is a GET endpoint, the controller should return an HTTP status of **405 Method Not Allowed**.
* **Test Code Reference:** `testGetAllOffersWithMalformedJson`


* **Test Case ID:** OC_007_02
* **Description:** Handle requests with invalid JSON structure to the endpoint (only applicable for POST).
* **Component/Module:** Offer Retrieval Endpoint - HTTP Method
* **Scenario:** A POST request is made to `/api/offers` with a JSON body that has a valid format but an incorrect structure/keys.
* **Expected Behavior:** Since `/api/offers` is a GET endpoint, the controller should return an HTTP status of **405 Method Not Allowed**.
* **Test Code Reference:** `testGetAllOffersWithInvalidJsonStructure`


* **Test Case ID:** OC_008
* **Description:** Basic performance test for retrieving a large number of offers.
* **Component/Module:** Offer Retrieval Endpoint - Performance
* **Scenario:** A GET request is made to `/api/offers` that is configured to return 1000 dummy offers.
* **Expected Behavior:** The controller should return an HTTP status of **200 OK**, and the request processing time should be within an acceptable limit (e.g., less than 5000 ms).
* **Test Code Reference:** `testGetAllOffersPerformanceWithLargeResponse`


* **Test Case ID:** OC_009_01
* **Description:** Basic security test for SQL injection attempt in `street` parameter.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation / Security
* **Scenario:** A GET request is made to `/api/offers` with `street` containing SQL injection characters (e.g., `'; DROP TABLE users;--`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message should indicate "Invalid characters in street name".
* **Test Code Reference:** `testGetAllOffersWithStreetSqlInjectionAttempt`


* **Test Case ID:** OC_009_02
* **Description:** Basic security test for SQL injection attempt in `houseNumber` parameter.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation / Security
* **Scenario:** A GET request is made to `/api/offers` with `houseNumber` containing SQL injection characters (e.g., `1; DROP TABLE users;`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message should indicate "Invalid characters in house number".
* **Test Code Reference:** `testGetAllOffersWithHouseNumberSqlInjectionAttempt`


* **Test Case ID:** OC_009_03
* **Description:** Basic security test for SQL injection attempt in `city` parameter.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation / Security
* **Scenario:** A GET request is made to `/api/offers` with `city` containing SQL injection characters (e.g., `somecity' OR '1'='1`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message should indicate "Invalid characters in city name".
* **Test Code Reference:** `testGetAllOffersWithCitySqlInjectionAttempt`


* **Test Case ID:** OC_009_04
* **Description:** Basic security test for SQL injection attempt in `plz` parameter.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation / Security
* **Scenario:** A GET request is made to `/api/offers` with `plz` containing SQL injection characters (e.g., `12345'; DELETE FROM offers;`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message should indicate "Invalid characters in postal code".
* **Test Code Reference:** `testGetAllOffersWithPlzSqlInjectionAttempt`


* **Test Case ID:** OC_009_05
* **Description:** Basic security test for XSS attempt in `street` parameter.
* **Component/Module:** Offer Retrieval Endpoint - Input Validation / Security
* **Scenario:** A GET request is made to `/api/offers` with `street` containing XSS script tags (e.g., `<script>alert('XSS')</script>`).
* **Expected Behavior:** The controller should return an HTTP status of **400 Bad Request** and resolve a `ConstraintViolationException`. The error message should indicate "Invalid characters in street name".
* **Test Code Reference:** `testGetAllOffersWithStreetXssAttempt`

---

## ByteMeService Test Cases

This section outlines the test cases for the `ByteMeService`, which is responsible for fetching and parsing internet offers from the ByteMe external API.

---

### Test Case Structure

* **Test Case ID:** A unique identifier for the test case.
* **Description:** A brief explanation of what the test case is verifying.
* **Component/Module:** The specific part of the `ByteMeService` being tested (e.g., offer fetching, CSV parsing, retry logic).
* **Scenario:** The specific situation or input being tested.
* **Expected Behavior:** What the `ByteMeService` should do or return in this scenario.
* **Test Code Reference:** Reference to the corresponding test code file or method.

---

### Implemented Test Cases in `ByteMeServiceTest.java`

* **Test Case ID:** BS_001
* **Description:** Verify that the service successfully retrieves and parses valid internet offers from the ByteMe API.
* **Component/Module:** Offer Fetching and CSV Parsing - Happy Path
* **Scenario:** The mocked `RestTemplate` returns a valid CSV response containing multiple offers with all mandatory and optional fields (including 'null' for optional numeric fields).
* **Expected Behavior:** The `getOffers` method should return a `Flux` emitting all correctly parsed `InternetOffer` objects in the order they appeared in the CSV.
* **Test Code Reference:** `getOffers_shouldReturnOffers_whenApiCallIsSuccessful`


* **Test Case ID:** BS_002
* **Description:** Verify that the service correctly handles an empty CSV response from the ByteMe API.
* **Component/Module:** Offer Fetching and CSV Parsing
* **Scenario:** The mocked `RestTemplate` returns a CSV response containing only the header row, indicating no offers.
* **Expected Behavior:** The `getOffers` method should return an empty `Flux` (no `InternetOffer` objects should be emitted).
* **Test Code Reference:** `getOffers_shouldReturnEmptyFlux_whenApiReturnsEmptyCsv`


* **Test Case ID:** BS_003
* **Description:** Verify that the service robustly handles malformed CSV responses from the ByteMe API.
* **Component/Module:** CSV Parsing - Error Handling
* **Scenario:** The mocked `RestTemplate` returns a malformed CSV string (e.g., missing column values, incorrect number of columns).
* **Expected Behavior:** The `getOffers` method should return an empty `Flux`, as the parsing process should fail to produce any valid offers from malformed input.
* **Test Code Reference:** `getOffers_shouldReturnEmptyFlux_whenApiReturnsMalformedCsv`


* **Test Case ID:** BS_004
* **Description:** Verify the retry mechanism when the ByteMe API returns an HTTP 5xx (Server Error) status.
* **Component/Module:** Offer Fetching - Retry Logic (Server Error)
* **Scenario:** The mocked `RestTemplate` initially throws `HttpServerErrorException` (e.g., 500 Internal Server Error) but succeeds on a subsequent retry.
* **Expected Behavior:** The `getOffers` method should perform a retry and successfully retrieve the offers after the first failure. The external API should be called twice (initial attempt + one retry).
* **Test Code Reference:** `getOffers_shouldRetryAndSucceed_onHttpServerError`


* **Test Case ID:** BS_005
* **Description:** Verify the retry mechanism when a `RestClientException` occurs during the API call (e.g., network issues).
* **Component/Module:** Offer Fetching - Retry Logic (Client Error)
* **Scenario:** The mocked `RestTemplate` initially throws a `RestClientException` (e.g., "Network error") but succeeds on a subsequent retry.
* **Expected Behavior:** The `getOffers` method should perform a retry and successfully retrieve the offers after the initial network failure. The external API should be called twice (initial attempt + one retry).
* **Test Code Reference:** `getOffers_shouldRetryAndSucceed_onRestClientException`


* **Test Case ID:** BS_006
* **Description:** Verify that the service stops retrying and returns an empty `Flux` if the maximum number of retries is reached for HTTP 5xx errors.
* **Component/Module:** Offer Fetching - Retry Logic (Max Retries Reached)
* **Scenario:** The mocked `RestTemplate` consistently throws `HttpServerErrorException` for all attempts up to the maximum retry limit (3 retries, total 4 attempts).
* **Expected Behavior:** The `getOffers` method should attempt the API call `MAX_RETRIES` (3) times, and ultimately return an empty `Flux` as no successful response is received.
* **Test Code Reference:** `getOffers_shouldReturnEmptyFlux_afterMaxRetriesReachedForHttpServerError`


* **Test Case ID:** BS_007
* **Description:** Verify that the service stops retrying and returns an empty `Flux` if the maximum number of retries is reached for `RestClientException` errors.
* **Component/Module:** Offer Fetching - Retry Logic (Max Retries Reached)
* **Scenario:** The mocked `RestTemplate` consistently throws `RestClientException` for all attempts up to the maximum retry limit (3 retries, total 4 attempts).
* **Expected Behavior:** The `getOffers` method should attempt the API call `MAX_RETRIES` (3) times, and ultimately return an empty `Flux` as no successful response is received.
* **Test Code Reference:** `getOffers_shouldReturnEmptyFlux_afterMaxRetriesReachedForRestClientException`


* **Test Case ID:** BS_008
* **Description:** Verify that the service does not retry on HTTP 4xx (Client Error) responses.
* **Component/Module:** Offer Fetching - Retry Logic (Client Error)
* **Scenario:** The mocked `RestTemplate` throws an `HttpServerErrorException` with a 4xx status code (e.g., 400 Bad Request).
* **Expected Behavior:** The `getOffers` method should not perform any retries and should return an empty `Flux` after the single failed attempt. The external API should be called only once.
* **Test Code Reference:** `getOffers_shouldNotRetry_onHttpClientError`

* **Test Case ID:** BS_009
* **Description:** Verify that the CSV parser filters out records with missing mandatory fields.
* **Component/Module:** CSV Parsing - Robustness
* **Scenario:** The mocked `RestTemplate` returns a CSV response where one or more mandatory fields are missing for a record.
* **Expected Behavior:** The `getOffers` method should parse only the records that have all mandatory fields present and correctly ignore (filter out) incomplete records.
* **Test Code Reference:** `getOffers_shouldFilterOutRecordsWithMissingMandatoryFields`


* **Test Case ID:** BS_010
* **Description:** Verify that the CSV parser filters out records with invalid numeric data in mandatory fields.
* **Component/Module:** CSV Parsing - Robustness
* **Scenario:** The mocked `RestTemplate` returns a CSV response where a mandatory numeric field contains non-numeric data.
* **Expected Behavior:** The `getOffers` method should parse only the records where numeric fields contain valid numbers and correctly ignore (filter out) records with invalid numeric data.
* **Test Code Reference:** `getOffers_shouldFilterOutRecordsWithInvalidNumericData`


* **Test Case ID:** BS_011
* **Description:** Verify that the CSV parser correctly handles and maps optional fields, including empty or 'null' string representations.
* **Component/Module:** CSV Parsing - Optional Field Handling
* **Scenario:** The mocked `RestTemplate` returns a CSV response where some optional fields are explicitly missing (empty string) or contain the literal "null" string.
* **Expected Behavior:** The `getOffers` method should parse both records. For the record with missing/null optional fields, those corresponding `InternetOffer` fields (e.g., `connectionType`, `installationService`, `tv`, `maxAge`) should be correctly mapped to `null` in the Java object.
* **Test Code Reference:** `getOffers_shouldCorrectlyParseOptionalFields`

---

## PingPerfect Integration Test Cases (`PingPerfectClientTest.java` & `PingPerfectServiceTest.java`)

This section outlines the test cases for the `PingPerfectClient` and `PingPerfectService`, which collectively handle the integration with the external PingPerfect API. The client manages API communication, including request signing and retry logic, while the service orchestrates the client call and transforms the raw JSON response into `InternetOffer` objects.

---

### Test Case Structure

* **Test Case ID:** A unique identifier for the test case.
* **Description:** A brief explanation of what the test case is verifying.
* **Component/Module:** The specific part of the PingPerfect integration being tested (e.g., API communication, request building, JSON parsing, retry logic).
* **Scenario:** The specific situation or input being tested.
* **Expected Behavior:** What the PingPerfect integration should do or return in this scenario.
* **Test Code Reference:** Reference to the corresponding test code file or method.

---

### Implemented Test Cases in `PingPerfectClientTest` and `PingPerfectServiceTest`

* **Test Case ID:** PP_001
* **Description:** Verify that the PingPerfect client successfully makes an API call with correct headers and body, and the service correctly parses the valid JSON response into `InternetOffer` objects.
* **Component/Module:** Client Request Building & Service JSON Parsing - Happy Path
* **Scenario:** The `PingPerfectClient` is mocked to return a valid JSON array response containing multiple offers. The `PingPerfectService` then processes this response.
* **Expected Behavior:** The `PingPerfectClient`'s `getInternetOffers` method should be called once with the correct URL, `POST` method, application/json content type, `X-Client-Id`, `X-Timestamp`, and `X-Signature` headers, and a correctly formatted JSON request body. The `PingPerfectService`'s `getOffers` method should then return a `Flux` emitting all correctly parsed `InternetOffer` objects.
* **Test Code Reference:**
    * `PingPerfectClientTest.getInternetOffers_shouldReturnOffers_whenApiCallIsSuccessful`
    * `PingPerfectServiceTest.getOffers_shouldReturnParsedOffers_whenClientReturnsValidJson`


* **Test Case ID:** PP_002
* **Description:** Verify the client's retry mechanism and the service's graceful handling when the PingPerfect API returns an HTTP 5xx (Server Error) status.
* **Component/Module:** Client Retry Logic & Service Error Handling
* **Scenario:** The `RestTemplate` (mocked in `PingPerfectClientTest`) or `PingPerfectClient` (mocked in `PingPerfectServiceTest`) initially throws an `HttpServerErrorException` (e.g., 500 Internal Server Error, 503 Service Unavailable) but succeeds on a subsequent retry.
* **Expected Behavior:**
    * **Client:** The `PingPerfectClient` should perform a retry and successfully retrieve the JSON response. The external API should be called twice (initial attempt + one retry).
    * **Service:** The `PingPerfectService` should receive the successful response after the retry and parse offers as normal.
* **Test Code Reference:**
    * `PingPerfectClientTest.getInternetOffers_shouldRetryAndSucceed_onHttpServerError`
    * `PingPerfectClientTest.getInternetOffers_shouldRetryAndSucceed_onServiceUnavailable`


* **Test Case ID:** PP_003
* **Description:** Verify the client's retry mechanism and the service's graceful handling when a `RestClientException` (e.g., network issues) occurs during the API call.
* **Component/Module:** Client Retry Logic & Service Error Handling
* **Scenario:** The `RestTemplate` (mocked in `PingPerfectClientTest`) or `PingPerfectClient` (mocked in `PingPerfectServiceTest`) initially throws a `RestClientException` (e.g., "Connection refused") but succeeds on a subsequent retry.
* **Expected Behavior:**
    * **Client:** The `PingPerfectClient` should perform a retry and successfully retrieve the JSON response. The external API should be called twice (initial attempt + one retry).
    * **Service:** The `PingPerfectService` should receive the successful response after the retry and parse offers as normal.
* **Test Code Reference:**
    * `PingPerfectClientTest.getInternetOffers_shouldRetryAndSucceed_onRestClientException`


* **Test Case ID:** PP_004
* **Description:** Verify that the client stops retrying and returns `null` if the maximum number of retries is reached for HTTP 5xx errors.
* **Component/Module:** Client Retry Logic - Max Retries Reached
* **Scenario:** The `RestTemplate` is consistently mocked to throw `HttpServerErrorException` for all attempts up to the maximum retry limit (3 retries).
* **Expected Behavior:** The `PingPerfectClient` should attempt the API call `MAX_RETRIES` (3) times, and ultimately return `null` as no successful response is received.
* **Test Code Reference:** `PingPerfectClientTest.getInternetOffers_shouldReturnNull_afterMaxRetriesReachedForHttpServerError`


* **Test Case ID:** PP_005
* **Description:** Verify that the client stops retrying and returns `null` if the maximum number of retries is reached for `RestClientException` errors.
* **Component/Module:** Client Retry Logic - Max Retries Reached
* **Scenario:** The `RestTemplate` is consistently mocked to throw `RestClientException` for all attempts up to the maximum retry limit (3 retries).
* **Expected Behavior:** The `PingPerfectClient` should attempt the API call `MAX_RETRIES` (3) times, and ultimately return `null` as no successful response is received.
* **Test Code Reference:** `PingPerfectClientTest.getInternetOffers_shouldReturnNull_afterMaxRetriesReachedForRestClientException`


* **Test Case ID:** PP_006
* **Description:** Verify that the client does not retry on HTTP 4xx (Client Error) responses.
* **Component/Module:** Client Error Handling - Non-retriable HTTP Errors
* **Scenario:** The `RestTemplate` throws an `HttpServerErrorException` with a 4xx status code (e.g., 400 Bad Request).
* **Expected Behavior:** The `PingPerfectClient` should not perform any retries and should return `null` immediately after the single failed attempt. The external API should be called only once.
* **Test Code Reference:** `PingPerfectClientTest.getInternetOffers_shouldReturnNull_onHttpClientError`


* **Test Case ID:** PP_007
* **Description:** Verify that the client correctly constructs the JSON request body based on `SearchRequests`.
* **Component/Module:** Client Request Building
* **Scenario:** The `buildRequestBody` method is invoked with various `SearchRequests` DTOs.
* **Expected Behavior:** The method should return a JSON string matching the expected structure and values, including correct boolean representation for `wantsFiber`.
* **Test Code Reference:** `PingPerfectClientTest.buildRequestBody_shouldCreateCorrectJsonPayload`


* **Test Case ID:** PP_008
* **Description:** Verify that the client correctly generates the HMAC-SHA256 signature for API requests.
* **Component/Module:** Client Security - Signature Generation
* **Scenario:** The `generateSignature` method is invoked with a known timestamp and payload.
* **Expected Behavior:** The method should return the correct hexadecimal HMAC-SHA256 signature calculated using the configured secret key.
* **Test Code Reference:** `PingPerfectClientTest.generateSignature_shouldReturnCorrectHmacSha256Signature`


* **Test Case ID:** PP_009
* **Description:** Verify that the service correctly handles a `null` JSON response body from the client.
* **Component/Module:** Service JSON Parsing - Robustness
* **Scenario:** The `PingPerfectClient` is mocked to return `null` when `getInternetOffers` is called.
* **Expected Behavior:** The `PingPerfectService`'s `getOffers` method should return an empty `Flux` and log a warning, indicating that no valid data was received.
* **Test Code Reference:**
    * `PingPerfectClientTest.getInternetOffers_shouldReturnNull_whenApiReturnsNullBody`
    * `PingPerfectServiceTest.getOffers_shouldReturnEmptyFlux_whenClientReturnsNull`


* **Test Case ID:** PP_010
* **Description:** Verify that the service correctly handles a non-array JSON response (e.g., an error object) from the client.
* **Component/Module:** Service JSON Parsing - Robustness
* **Scenario:** The `PingPerfectClient` is mocked to return a non-array `JsonNode` (e.g., a simple JSON object).
* **Expected Behavior:** The `PingPerfectService`'s `getOffers` method should return an empty `Flux` and log a warning, indicating that the response format was invalid.
* **Test Code Reference:** `PingPerfectServiceTest.getOffers_shouldReturnEmptyFlux_whenClientReturnsNonArrayJson`


* **Test Case ID:** PP_011
* **Description:** Verify that the service robustly filters out individual offer nodes that are missing mandatory fields or contain invalid data types.
* **Component/Module:** Service JSON Parsing - Robustness / Filtering Invalid Data
* **Scenario:** The `PingPerfectClient` is mocked to return a JSON array containing a mix of valid offer nodes and invalid ones (e.g., missing mandatory `speed` or `monthlyCostInCent`, invalid numeric format).
* **Expected Behavior:** The `PingPerfectService`'s `getOffers` method should emit only the correctly parsed `InternetOffer` objects, silently filtering out any invalid offer nodes.
* **Test Code Reference:** `PingPerfectServiceTest.getOffers_shouldFilterOutInvalidOfferNodes`


* **Test Case ID:** PP_012
* **Description:** Verify that the service gracefully handles errors propagated from the `PingPerfectClient`.
* **Component/Module:** Service Error Handling
* **Scenario:** The `PingPerfectClient` is mocked to throw a `RuntimeException` during the `getInternetOffers` call.
* **Expected Behavior:** The `PingPerfectService`'s `getOffers` method should propagate the error (terminate with `onError`) and not emit any `InternetOffer` objects. It should log the error appropriately.
* **Test Code Reference:** `PingPerfectServiceTest.getOffers_shouldHandleClientErrorGracefully`

---

## ServusSpeedClient Test Cases (`ServusSpeedClientRestTest.java` & `ServusSpeedClientWebClientTest.java`)

This section outlines the test cases for the `ServusSpeedClient`, which is responsible for interacting with the external ServusSpeed API. This includes fetching available product IDs using `RestTemplate` and fetching detailed product information using `WebClient`, as well as handling caching and error scenarios.

---

### Test Case Structure

* **Test Case ID:** A unique identifier for the test case.
* **Description:** A brief explanation of what the test case is verifying.
* **Component/Module:** The specific part of the `ServusSpeedClient` being tested (e.g., product ID fetching, detailed offer fetching, caching, error handling, JSON parsing).
* **Scenario:** The specific situation or input being tested.
* **Expected Behavior:** What the `ServusSpeedClient` should do or return in this scenario.
* **Test Code Reference:** Reference to the corresponding test code file or method.

---

### Implemented Test Cases in `ServusSpeedClientRestTest.java`

* **Test Case ID:** SSCR_001
* **Description:** Verify that the client successfully retrieves and parses product IDs from the ServusSpeed API using `RestTemplate`.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Happy Path
* **Scenario:** The mocked `RestTemplate` returns a valid JSON response containing multiple product IDs.
* **Expected Behavior:** The `getAvailableProductIds` method should return a `List` of the correctly parsed product IDs.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnProductIds_whenApiCallIsSuccessful`


* **Test Case ID:** SSCR_002
* **Description:** Verify that the client returns an empty list when the API returns an empty product IDs array.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Empty Response
* **Scenario:** The mocked `RestTemplate` returns a JSON response with an empty `availableProducts` array.
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsEmptyProductsArray`


* **Test Case ID:** SSCR_003
* **Description:** Verify that the client returns an empty list when the API returns a `null` response body.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Null Body
* **Scenario:** The mocked `RestTemplate` returns an HTTP 200 OK status but with a `null` response body.
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsNullBody`


* **Test Case ID:** SSCR_004
* **Description:** Verify that the client returns an empty list when the API returns a non-object JSON response.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Malformed JSON
* **Scenario:** The mocked `RestTemplate` returns a JSON response that is not an object (e.g., a direct array or string).
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsNonObjectResponse`


* **Test Case ID:** SSCR_005
* **Description:** Verify that the client returns an empty list when the API returns a JSON object missing the `availableProducts` field.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Malformed JSON
* **Scenario:** The mocked `RestTemplate` returns a valid JSON object, but it lacks the expected `availableProducts` field.
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsObjectWithoutAvailableProducts`


* **Test Case ID:** SSCR_006
* **Description:** Verify that the client handles HTTP 4xx (Client Error) responses by returning an empty list and not attempting retries (implicit by `RestTemplate` behavior).
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Error Handling (4xx)
* **Scenario:** The mocked `RestTemplate` throws an `HttpClientErrorException` (e.g., 400 Bad Request).
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_onHttpClientError`


* **Test Case ID:** SSCR_007
* **Description:** Verify that the client handles HTTP 5xx (Server Error) responses by returning an empty list and not attempting retries (implicit by `RestTemplate` behavior).
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Error Handling (5xx)
* **Scenario:** The mocked `RestTemplate` throws an `HttpServerErrorException` (e.g., 500 Internal Server Error).
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_onHttpServerError`


* **Test Case ID:** SSCR_008
* **Description:** Verify that the client handles `RestClientException` (e.g., network issues) by returning an empty list.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Error Handling (Network)
* **Scenario:** The mocked `RestTemplate` throws a `RestClientException` (e.g., connection refused).
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_onRestClientException`

---

### Implemented Test Cases in `ServusSpeedClientWebClientTest.java`

* **Test Case ID:** SSCWC_001
* **Description:** Verify that the client successfully fetches detailed product information using `WebClient` and maps it to an `InternetOffer`.
* **Component/Module:** Detailed Offer Fetching (`WebClient`) - Happy Path
* **Scenario:** The mocked `WebClient` chain returns a valid `DetailedResponseData` object.
* **Expected Behavior:** The `fetchProductDetails` method should return a `Flux` emitting a correctly mapped `InternetOffer` object, and the offer should be added to the cache.
* **Test Code Reference:** `fetchProductDetails_shouldReturnOffer_whenApiCallIsSuccessful`


* **Test Case ID:** SSCWC_002
* **Description:** Verify that the client returns an empty `Flux` if the `WebClient` response data cannot be mapped to a valid `InternetOffer` (e.g., missing essential fields).
* **Component/Module:** Detailed Offer Fetching (`WebClient`) - Mapping Failure
* **Scenario:** The mocked `WebClient` chain returns a `DetailedResponseData` object that, when processed by `mapToInternetOffer`, results in `null`.
* **Expected Behavior:** The `fetchProductDetails` method should return an empty `Flux`.
* **Test Code Reference:** `fetchProductDetails_shouldReturnEmptyFlux_forInvalidOfferMapping`


* **Test Case ID:** SSCWC_003
* **Description:** Verify the correct mapping of all fields from `DetailedResponseData` to `InternetOffer`.
* **Component/Module:** Data Transformation (`mapToInternetOffer`)
* **Scenario:** A `DetailedResponseData` object with all fields populated is provided for mapping.
* **Expected Behavior:** The `mapToInternetOffer` method should produce an `InternetOffer` with all corresponding fields correctly set.
* **Test Code Reference:** `mapToInternetOffer_shouldMapAllFieldsCorrectly`


* **Test Case ID:** SSCWC_004
* **Description:** Verify that `mapToInternetOffer` returns `null` if the `ProductInfo` is missing from the `DetailedResponseData`.
* **Component/Module:** Data Transformation (`mapToInternetOffer`) - Missing Data
* **Scenario:** A `DetailedResponseData` object where `ProductInfo` is `null` is provided.
* **Expected Behavior:** The `mapToInternetOffer` method should return `null`.
* **Test Code Reference:** `mapToInternetOffer_shouldReturnNull_whenProductInfoIsMissing`


* **Test Case ID:** SSCWC_005
* **Description:** Verify that `mapToInternetOffer` returns `null` if the `PricingDetails` is missing from the `DetailedResponseData`.
* **Component/Module:** Data Transformation (`mapToInternetOffer`) - Missing Data
* **Scenario:** A `DetailedResponseData` object where `PricingDetails` is `null` is provided.
* **Expected Behavior:** The `mapToInternetOffer` method should return `null`.
* **Test Code Reference:** `mapToInternetOffer_shouldReturnNull_whenPricingDetailsIsMissing`


* **Test Case ID:** SSCWC_006
* **Description:** Verify that `mapToInternetOffer` returns `null` if the `ServusSpeedProduct` is missing from the `DetailedResponseData`.
* **Component/Module:** Data Transformation (`mapToInternetOffer`) - Missing Data
* **Scenario:** A `DetailedResponseData` object where `ServusSpeedProduct` is `null` is provided.
* **Expected Behavior:** The `mapToInternetOffer` method should return `null`.
* **Test Code Reference:** `mapToInternetOffer_shouldReturnNull_whenServusSpeedProductIsNull`


* **Test Case ID:** SSCWC_007
* **Description:** Verify that the `putOfferInCache` method successfully adds a valid `InternetOffer` to the internal cache.
* **Component/Module:** Caching
* **Scenario:** A valid `InternetOffer` object is passed to `putOfferInCache`.
* **Expected Behavior:** The offer should be present in the client's cache under its `productId`.
* **Test Code Reference:** `putOfferInCache_shouldAddOfferToCache`


* **Test Case ID:** SSCWC_008
* **Description:** Verify that the `putOfferInCache` method does not add `null` offers or offers with `null` `productId` to the cache.
* **Component/Module:** Caching - Invalid Input
* **Scenario:** `null` or an `InternetOffer` with a `null` `productId` is passed to `putOfferInCache`.
* **Expected Behavior:** The cache should remain empty or unchanged.
* **Test Code Reference:** `putOfferInCache_shouldNotAddNullOrInvalidOfferToCache`


* **Test Case ID:** SSCWC_009
* **Description:** Verify that the main `getOffers` method correctly combines offers retrieved from the cache and newly fetched offers from the API.
* **Component/Module:** Overall Offer Retrieval (Integration of Caching & Fetching)
* **Scenario:** The client is configured with some cached offers, and `getAvailableProductIds` returns a mix of cached and new product IDs.
* **Expected Behavior:** The `getOffers` method should return a `Flux` containing both the pre-cached and the newly fetched `InternetOffer` objects.
* **Test Code Reference:** `getOffers_shouldCombineCachedAndFetchedOffers`


* **Test Case ID:** SSCWC_010
* **Description:** Verify that the main `getOffers` method returns an empty `Flux` when no product IDs are found by the API.
* **Component/Module:** Overall Offer Retrieval - No Product IDs
* **Scenario:** The `getAvailableProductIds` method (mocked via `RestTemplate`) returns an empty list of product IDs.
* **Expected Behavior:** The `getOffers` method should return an empty `Flux`, and no `WebClient` calls for detailed offers should be made.
* **Test Code Reference:** `getOffers_shouldReturnEmptyFlux_whenNoProductIdsFound`


* **Test Case ID:** SSCWC_011
* **Description:** Verify that the main `getOffers` method correctly fetches offers when there are no cached offers.
* **Component/Module:** Overall Offer Retrieval - No Initial Cache
* **Scenario:** The client's cache is empty, and `getAvailableProductIds` returns a list of product IDs.
* **Expected Behavior:** The `getOffers` method should return a `Flux` containing all newly fetched `InternetOffer` objects.
* **Test Code Reference:** `getOffers_shouldHandleEmptyCachedOffersCorrectly`


* **Test Case ID:** SSCWC_012
* **Description:** Verify that the `getOffers` method utilizes concurrency for fetching detailed offers, improving performance for multiple offers.
* **Component/Module:** Overall Offer Retrieval - Concurrency
* **Scenario:** The client fetches a relatively large number of offers, with each fetch simulated to take a short duration, and the client's `parallel` setting (implicitly `PARALLEL` constant) is active.
* **Expected Behavior:** The total time taken to fetch all offers should be significantly less than if they were fetched sequentially, demonstrating parallel execution.
* **Test Code Reference:** `getOffers_shouldFetchOffersConcurrentlyUsingParallelSetting`


* **Test Case ID:** SSCWC_013
* **Description:** Verify that `getAvailableProductIds` returns an empty list when the API returns a non-2xx HTTP status.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Non-2xx Response
* **Scenario:** The mocked `RestTemplate` returns a `ResponseEntity` with a non-2xx status code (e.g., 500 Internal Server Error).
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsNon2xxStatus`


* **Test Case ID:** SSCWC_014
* **Description:** Verify that `getAvailableProductIds` returns an empty list when the API returns an empty response body (even with 2xx status).
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Empty Body
* **Scenario:** The mocked `RestTemplate` returns a `ResponseEntity` with an HTTP 200 OK status but an empty body.
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsEmptyBody`


* **Test Case ID:** SSCWC_015
* **Description:** Verify that `getAvailableProductIds` returns an empty list when the API returns a response body that lacks the expected `availableProducts` field.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Malformed JSON (Missing Field)
* **Scenario:** The mocked `RestTemplate` returns a JSON object that is missing the `availableProducts` field.
* **Expected Behavior:** The `getAvailableProductIds` method should return an empty `List`.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsBodyWithoutAvailableProducts`


* **Test Case ID:** SSCWC_016
* **Description:** Verify that `getAvailableProductIds` correctly handles `HttpClientErrorException` (4xx errors) from `RestTemplate`.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Error Handling (4xx)
* **Scenario:** The `RestTemplate` throws an `HttpClientErrorException` (e.g., `HttpStatus.NOT_FOUND`).
* **Expected Behavior:** The method should catch the exception and return an empty list.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_onHttpClientErrorException`


* **Test Case ID:** SSCWC_017
* **Description:** Verify that `getAvailableProductIds` correctly handles `HttpServerErrorException` (5xx errors) from `RestTemplate`.
* **Component/Module:** Product ID Fetching (`RestTemplate`) - Error Handling (5xx)
* **Scenario:** The `RestTemplate` throws an `HttpServerErrorException` (e.g., `HttpStatus.BAD_GATEWAY`).
* **Expected Behavior:** The method should catch the exception and return an empty list.
* **Test Code Reference:** `getAvailableProductIds_shouldReturnEmptyList_onHttpServerErrorException`


