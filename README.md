# GenDev7 - Frontend

This frontend application, "NetOffer Navigator," allows users to search, compare, filter, and share internet service offers. It is built with Next.js, React, TypeScript, and utilizes ShadCN UI components with Tailwind CSS for styling.

## Core Functionality

*   **Address Input:** Users can enter their address (street, house number (optional), city, PLZ, and country) to search for available internet offers.
*   **Real-time Offer Streaming:** Offers are fetched from a backend service in real-time using Server-Sent Events (SSE) as they become available from different providers.
*   **Offer Display:** Fetched offers are displayed in a responsive card layout, showing key details like provider name, speed, monthly cost, and connection type.
*   **Filtering:** Users can filter the displayed offers by various criteria:
    *   Provider
    *   Maximum price
    *   Minimum speed
    *   Connection type
    *   Young tariffs
    *   Contract duration
    *   TV included
*   **Sorting:** Offers can be sorted by:
    *   Default order (as received)
    *   Price (ascending/descending)
    *   Speed (descending/ascending)
    *   Provider name (ascending/descending)
*   **Offer Details Modal:** Clicking on an offer card opens a modal with comprehensive details about the selected offer.
*   **Shareable Links:** Users can generate a shareable link that includes the currently displayed (and filtered/sorted) offers, along with the active filter and sort settings. This data is compressed and encoded into the URL.
*   **Theme Toggle:** Supports light and dark themes, with user preference persisted in local storage.

## Technology Stack

*   **Framework:** Next.js 15 (App Router)
*   **Language:** TypeScript
*   **UI Library:** React 18
*   **UI Components:** ShadCN UI
*   **Styling:** Tailwind CSS
*   **Form Management:** React Hook Form with Zod for validation
*   **Data Fetching (Client-Side):** Native `EventSource` API for Server-Sent Events.
*   **State Management:**
    *   React Hooks (`useState`, `useEffect`, `useCallback`, `useMemo`, `useRef`)
    *   React Context API (for `ThemeProvider`)
    *   URL Search Parameters (for persisting filters, sort criteria, and shared offer data)
*   **Utilities:**
    *   `pako` for data compression/decompression (for sharing links)
    *   `clsx`, `tailwind-merge` for class name utilities
    *   `date-fns` (if date operations were more prominent, currently minimal usage)
    *   `lucide-react` for icons

## Key Components

*   **`src/app/page.tsx`:** The main page component. Manages overall application state (filters, sort criteria, fetched offers, selected country), handles URL parameter parsing and updates, and orchestrates interactions between child components.
*   **`src/components/address-form.tsx`:** Handles user input for the address, including validation using Zod and React Hook Form. Triggers offer search upon submission.
*   **`src/components/offer-list.tsx`:**
    *   Manages the Server-Sent Event (SSE) connection to the backend to fetch offers.
    *   Displays loading states, error messages, or "no offers found" messages.
    *   Renders `OfferCard` components for each offer.
    *   Applies client-side filtering and sorting to the received offers.
    *   Dynamically updates the list of available connection types for the filter dropdown.
*   **`src/components/offer-card.tsx`:** Displays a summary of a single internet offer in a card format.
*   **`src/components/filter-sort-controls.tsx`:** Provides UI elements (selects, inputs, checkboxes) for users to define filter criteria and sort preferences.
*   **`src/components/offer-details-modal.tsx`:** A dialog component that shows detailed information about a selected offer.
*   **`src/components/share-button.tsx`:** Allows users to copy a shareable link containing the current offer list and settings.
*   **`src/components/site-header.tsx`:** The main application header, including the title and theme toggle button.
*   **`src/contexts/ThemeContext.tsx`:** Provides theme state (light/dark) and persistence logic.
*   **`src/components/ui/`:** Contains ShadCN UI components.

## Data Flow & State Management

1.  **Initial Load & URL Parsing:**
    *   The main page (`page.tsx`) initializes default filters and sort criteria.
    *   It parses URL search parameters to override defaults if present (e.g., from a shared link or previous session). This includes filters, sort settings, and potentially a `data` parameter with compressed shared offers.
2.  **Address Input:**
    *   User enters address details into `AddressForm`.
    *   On submission, `AddressForm` validates the input and calls `onAddressConfirmed` in `page.tsx`.
3.  **Offer Fetching (`OfferList`):**
    *   `page.tsx` updates its `filters` state with the confirmed address, triggering a re-render and a `searchTrigger` update.
    *   `OfferList` detects changes in address filters or `searchTrigger`.
    *   If a valid address is present (and not in shared link mode), it initiates an SSE connection to the backend (`/api/offers`) with address parameters (including country code).
    *   A request is also sent to `/api/user-activity/log-search` to log the search event.
    *   Offers are received one by one via the SSE stream. Each new offer is added to the local `offers` state in `OfferList`.
4.  **Filtering and Sorting:**
    *   The raw `offers` array in `OfferList` is memoized and re-processed whenever `offers`, `filters`, or `sortCriteria` change.
    *   Client-side logic applies the current filters and sort criteria to produce `filteredAndSortedOffers`.
5.  **Display:**
    *   `filteredAndSortedOffers` are mapped to `OfferCard` components for display.
    *   Available connection types for the filter dropdown are dynamically derived from the full set of received offers.
6.  **URL Updates:**
    *   Changes to `filters` or `sortCriteria` (not initiated by URL parsing itself) in `page.tsx` trigger an update to the browser's URL search parameters (without a full page reload), allowing for bookmarking and back/forward navigation.
7.  **Sharing:**
    *   `ShareButton` gets the `filteredAndSortedOffers` from `OfferList` (via a ref).
    *   These offers are stringified, compressed, and encoded into a URL-safe Base64 string.
    *   This encoded data, along with current filters and sort criteria from `page.tsx`'s URL state, forms the shareable URL.

## Error Handling

*   **API Connection Errors:** `OfferList` displays an error message if the SSE connection to the backend fails.
*   **No Offers Found:** If the backend stream completes without sending any offers, or if filters result in an empty list, appropriate messages are shown.
*   **Invalid Shared Data:** If the `data` parameter in a shared link is malformed, a warning is shown.
*   **Form Validation:** `AddressForm` uses Zod and React Hook Form to provide inline validation messages.

## Project Structure

*   `src/app/`: Contains the main Next.js page (`page.tsx`) and layout (`layout.tsx`).
*   `src/components/`:
    *   `ui/`: Auto-generated ShadCN UI primitive components.
    *   Other custom, reusable React components for the application.
*   `src/contexts/`: React Context providers (e.g., `ThemeContext.tsx`).
*   `src/hooks/`: Custom React hooks (e.g., `useToast.ts`, `useDebounce.ts`, `useMobile.ts`).
*   `src/lib/`: Utility functions (e.g., `utils.ts` for `cn`, compression; `user-tracking.ts`).
*   `src/types/`: TypeScript type definitions (`index.ts`).
*   `public/`: Static assets.

## Getting Started

1.  **Prerequisites:**
    *   Node.js (version specified in project or latest LTS)
    *   npm or yarn

2.  **Installation:**
    ```bash
    npm install
    # or
    yarn install
    ```

3.  **Environment Variables:**
    *   The application currently doesn't rely on a `.env` file for frontend-specific critical variables. Backend URL is hardcoded in `OfferList.tsx`. For production, consider moving this to an environment variable.

4.  **Running the Development Server:**
    ```bash
    npm run dev
    # or
    yarn dev
    ```
    The application will typically be available at `http://localhost:9002`.

5.  **Building for Production:**
    ```bash
    npm run build
    # or
    yarn build
    ```

6.  **Starting the Production Server:**
    ```bash
    npm run start
    # or
    yarn start
    ```

## Backend Interaction

The frontend interacts with a backend service (expected to be running separately) for two main purposes:

1.  **Fetching Offers:**
    *   **Endpoint:** `GET /api/offers` (base URL configurable in `OfferList.tsx`)
    *   **Parameters:** `street`, `houseNumber`, `city`, `plz`, `land` (country code: DE, AT, CH)
    *   **Response:** `text/event-stream` (Server-Sent Events). Each event is a JSON string representing an `InternetOffer` object.
2.  **Logging User Search Activity:**
    *   **Endpoint:** `POST /api/user-activity/log-search`
    *   **Body:** A JSON payload (`SearchLogPayload`) containing an anonymous user ID, timestamp, address details, filters, and sort criteria.


# GenDev7 - Backend

The backend application for "NetOffer Navigator" is a Spring Boot service designed to aggregate internet service offers from various external providers. It exposes a single real-time streaming API endpoint to the frontend, handling complex data fetching, transformation, and error handling with retries.

## Architecture & Core Design Principles

The backend follows a service-oriented architecture with a clear separation of concerns, built around Spring Boot's capabilities for rapid development and reactive programming.

*   **API Gateway (Controller Layer):** A single, reactive REST endpoint (`/api/offers`) acts as the entry point for frontend requests, managing the Server-Sent Events (SSE) stream.
*   **Provider-Specific Services:** Each external internet offer provider has its own dedicated service implementation (e.g., `ByteMeService`, `PingPerfectService`, `ServusSpeedClient`, `VerbynDichService`, `WebWunderService`). This encapsulates provider-specific logic, API contracts, and data transformation.
*   **Reactive Programming with Project Reactor:** The core offer aggregation logic utilizes Project Reactor's `Flux` to enable non-blocking, real-time streaming of offers to the frontend. This allows offers from different providers to arrive as soon as they are ready, enhancing perceived responsiveness.
*   **Robustness & Resilience:** Each service incorporates retry mechanisms with exponential backoff and jitter for transient errors (e.g., network issues, server-side errors, rate limits) when calling external APIs.
*   **Data Models:** Clear, concise DTOs and domain models (`InternetOffer`, `RequestAddress`, `SearchRequests`) ensure consistent data representation across the application.
*   **Logging:** Comprehensive logging is implemented to monitor application flow, API call statuses, data parsing warnings, and errors, aiding in debugging and operational insights.
*   **Configuration Management:** External API keys and URLs are managed via Spring's `@Value` annotation, allowing for easy configuration through `application.properties` or environment variables.

## Technology Stack

*   **Framework:** Spring Boot 3.x
*   **Language:** Java 17+
*   **Reactive Programming:** Project Reactor
*   **Web Framework:** Spring WebFlux (for reactive endpoint and `WebClient`) and Spring Web (for `RestTemplate`).
*   **HTTP Clients:**
    *   `WebClient`: Preferred for reactive, non-blocking HTTP requests to external APIs (e.g., ServusSpeed, VerbynDich).
    *   `RestTemplate`: Used for synchronous HTTP calls where a reactive flow isn't strictly necessary or for legacy integrations (e.g., ByteMe, PingPerfect for specific calls).
*   **SOAP Client:** Spring Web Services (`WebServiceTemplate`) for interacting with the legacy WebWunder SOAP endpoint.
*   **JSON Processing:** Jackson (`ObjectMapper`, `JsonNode`) for flexible JSON manipulation and parsing.
*   **CSV Parsing:** Apache Commons CSV for parsing CSV responses from providers.
*   **Logging:** SLF4J with Logback.
*   **Security (HMAC):** Standard Java Cryptography Architecture (JCA) for HMAC-SHA256 signature generation (e.g., for PingPerfect authentication).
*   **Utility Libraries:** Lombok (for boilerplate reduction in DTOs), various Spring utilities.

## Key Backend Components

### `com.SimonMk116.gendev.controller.OfferController`

*   **Role:** The primary REST API endpoint for the frontend.
*   **Endpoint:** `GET /api/offers`
*   **Functionality:**
    *   Receives address parameters from the frontend (street, house number, city, PLZ, country code).
    *   Acts as an orchestrator, invoking `getOffers` methods on all registered `OfferController.InternetOfferService` implementations (i.e., all provider services).
    *   Merges the `Flux` streams from each service using `Flux.merge`, ensuring offers are streamed to the frontend as Server-Sent Events (`text/event-stream`) in real-time.
    *   Performs request validation using `jakarta.validation` annotations.

### `com.SimonMk116.gendev.model.*` (Data Models)

*   **`InternetOffer`:** The core domain model representing a standardized internet service offer, aggregating data from various provider-specific formats.
*   **`RequestAddress`:** A simple DTO for carrying address information passed from the frontend to the backend services.
*   **`SearchRequests`:** A DTO specifically used by the `PingPerfectClient` and `WebWunderClient` to structure their API requests.

### `com.SimonMk116.gendev.service.*` (Provider Services)

Each service implements the `OfferController.InternetOfferService` interface, defining the contract for fetching offers for a given address.

*   **`bytemeservice.ByteMeService`:**
    *   Integration: Synchronous REST API via `RestTemplate`.
    *   Data Format: CSV response, parsed using Apache Commons CSV.
    *   Features: Includes robust error handling and retry logic for API calls.
*   **`pingperfectservice.PingPerfectService`:**
    *   Integration: Reactive REST API via `WebClient`, utilizing `PingPerfectClient` for the actual HTTP calls.
    *   Data Format: JSON response.
    *   Features: Handles HMAC-SHA256 signature generation for API authentication. Maps raw JSON (`JsonNode`) responses to `InternetOffer` objects, performing validation of mandatory fields.
*   **`servusspeedservice.ServusSpeedClient`:**
    *   Integration: Hybrid approach. Uses `RestTemplate` for synchronous fetching of initial product IDs and `WebClient` for reactive fetching of detailed product information.
    *   Data Format: JSON response.
    *   Features: Implements sophisticated pagination by fetching product IDs first, then concurrently fetching details for multiple products in parallel using a custom `OfferLoader` and `Flux.merge`. Includes an in-memory cache to reduce redundant API calls for common offers.
*   **`verbyndichservice.VerbynDichService`:**
    *   Integration: Reactive REST API via `WebClient`.
    *   Data Format: Custom text-based response requiring regex parsing.
    *   Features: Implements reactive pagination by fetching pages concurrently. Extracts offer details from a free-text "description" field using a comprehensive set of regular expressions.
*   **`webwunderservice.WebWunderService`:**
    *   Integration: SOAP Web Service via `WebWunderClient` (which uses Spring's `WebServiceTemplate`).
    *   Data Format: XML (SOAP) response, automatically marshaled/unmarshaled by Spring WS.
    *   Features: Orchestrates calls across different internet `ConnectionTypes` (DSL, CABLE, FIBER, MOBILE) in parallel. Maps the structured SOAP response objects to `InternetOffer`, including complex voucher logic.

## Logging User Activity

*   **`com.SimonMk116.gendev.controller.UserActivityController` (Implied from frontend description):**
    *   **Endpoint:** `POST /api/user-activity/log-search`
    *   **Functionality:** Receives search event payloads from the frontend, enabling the logging of user search activities for analytical purposes. (Detailed implementation not provided in reviewed code, but its API is clear from frontend usage).

## Getting Started

To run the backend application, ensure you have the necessary environment configured.

### Prerequisites:

*   Java 21+ (JDK)
*   Maven (or Gradle if configured differently)
*   An IDE (IntelliJ IDEA, Eclipse, VS Code) is recommended.

1. **Clone the Repository:**
```bash
   git clone [your-repo-url]
   cd GenDev7
```

2. **Configure Application Properties:**
    The application uses `application.properties` as its primary configuration file, and it imports additional properties from `api-keys.properties` and `provider-config.properties` for better organization and separation of concerns.

    **`src/main/resources/application.properties`:**
    ```properties
    # Import additional config files (paths are relative to classpath)
    spring.config.import=classpath:api-keys.properties,classpath:provider-config.properties

    # General Application Settings
    spring.application.name=gendev
    server.port=3001
    server.forward-headers-strategy=NATIVE # Important for proxy environments (e.g., Cloud Workstations)

    # CORS Configuration for Frontend Integration
    cors.allowed-origins=[https://6000-firebase-studio-1747694501106.cluster-6vyo4gb53jczovun3dxslzjahs.cloudworkstations.dev](https://6000-firebase-studio-1747694501106.cluster-6vyo4gb53jczovun3dxslzjahs.cloudworkstations.dev)

    # Asynchronous Request Timeout (e.g., for SSE)
    spring.mvc.async.request-timeout=50000
    ```

    **`src/main/resources/api-keys.properties`:**
    *This file should contain your sensitive API keys and secrets.*
    ```properties
    # ByteMe API Key
    provider.byteme.api-key=YOUR_API_KEY_BYTEME

    # PingPerfect API Key
    provider.pingperfect.client-id=YOUR_CLIENT_ID_PINGPERFECT
    provider.pingperfect.signature-secret=YOUR_SIGNATURE_SECRET_PINGPERFECT

    # VerbynDich API Key
    provider.verbyndich.api-key=YOUR_API_KEY_VERBYNDICH

    # ServusSpeed Credentials
    provider.servus.username=YOUR_SERVUSSPEED_USERNAME
    provider.servus.password=YOUR_SERVUSSPEED_PASSWORD

    # WebWunder API Key
    provider.webwunder.api-key=YOUR_WEBWUNDER_API_KEY
    ```

    **`src/main/resources/provider-config.properties`:**
    *This file should contain the API URLs and endpoints for each provider.*
    ```properties
    # ByteMe API URL
    provider.byteme.api-url=[https://byteme.gendev7.check24.fun/app/api/products/data](https://byteme.gendev7.check24.fun/app/api/products/data)

    # PingPerfect API URL
    provider.pingperfect.api-url=[https://pingperfect.gendev7.check24.fun/internet/angebote/data](https://pingperfect.gendev7.check24.fun/internet/angebote/data)

    # ServusSpeed Base URL
    provider.servus.base-url=[https://servus-speed.gendev7.check24.fun](https://servus-speed.gendev7.check24.fun)

    # VerbynDich Service
    # Note: VerbynDich base URL is hardcoded in VerbynDichService for WebClient builder.
    #       No external URL property is needed here.

    # WebWunder Endpoint (SOAP WSDL Location)
    provider.webwunder.endpoint=[https://webwunder.gendev7.check24.fun/endpunkte/soap/ws](https://webwunder.gendev7.check24.fun/endpunkte/soap/ws)
    ```

3.  **Build the Project:**
```bash
mvn clean install
```

4. **Run the Application:**
```bash
java -jar target/gendev-0.0.1-SNAPSHOT.jar # Adjust version as needed
```
Alternatively, run from your IDE by executing the Application class.

The backend service will typically start on port 8080 by default.