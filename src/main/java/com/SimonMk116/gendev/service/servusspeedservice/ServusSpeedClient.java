package com.SimonMk116.gendev.service.servusspeedservice;

import com.SimonMk116.gendev.controller.OfferController;
import com.SimonMk116.gendev.dto.DetailedResponseData;
import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.model.ServusSpeedProduct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Service client for integrating with the "ServusSpeed" internet offer provider.
 * This class implements {@link OfferController.InternetOfferService} and is responsible for
 * fetching internet offers from ServusSpeed using both synchronous (RestTemplate) and
 * reactive (WebClient) HTTP calls. It includes logic for fetching available product IDs,
 * fetching detailed product information in parallel, and an in-memory caching mechanism
 * for fetched offers.
 */
@Service
@RequiredArgsConstructor
public class ServusSpeedClient implements OfferController.InternetOfferService {

    /**
     * {@link WebClient} instance, qualified for ServusSpeed, used for reactive HTTP calls
     * to fetch detailed product information.
     */
    @Autowired
    @Qualifier("servusSpeedWebClient")
    private final WebClient webClient;

    /**
     * {@link RestTemplate} instance, qualified for ServusSpeed, used for synchronous HTTP calls
     * to fetch available product IDs.
     */
    @Autowired
    @Qualifier("servusSpeedRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${provider.servus.base-url}")
    private String baseUrl;

    /**
     * {@link ObjectMapper} instance used for JSON serialization and deserialization.
     * Automatically injected by Spring.
     */
    @Autowired
    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(ServusSpeedClient.class);

    public static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 300;
    static final int PARALLEL = 3;

    /**
     * Stores an {@link InternetOffer} in the in-memory cache, using its product ID as the key.
     * This reduces redundant API calls for frequently requested offers.
     *
     * <p>Note: While product IDs are (assumed to be) unique identifiers for offers,
     * different product IDs might sometimes resolve to identical offer details.
     * Therefore, pre-filling the cache with a full list of product IDs isn't practical,
     * as it could lead to redundant entries for the same underlying offer.
     *
     * @param offer The InternetOffer to cache.
     */
    public void putOfferInCache(InternetOffer offer) {
        if (offer != null && offer.getProductId() != null) {
            cache.put(offer.getProductId(), offer);
            logger.info("Manually cached offer with productId {}", offer.getProductId());
        } else {
            logger.warn("Attempted to cache null or invalid offer: {}", offer);
        }
    }

    private final Cache<String, InternetOffer> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)  // Configures the LRU policy: evicts least recently used when size exceeds 1000
            .build();

    /**
     * Fetches the list of available product IDs for a given address.
     * This method uses RestTemplate for a synchronous call.
     *
     * @param address The request address containing street, house number, postcode, city, and country.
     * @return A list of product IDs, or an empty list if an error occurs or no IDs are found.
     */
    private List<String> getAvailableProductIds(RequestAddress address) {
        String url = baseUrl + "/api/external/available-products";
        logger.info("Fetching available product IDs from URL: {}", url);
        logger.debug("Sending request for available products with address: {}", address);

        // Build the request body using ObjectMapper
        ObjectNode addressNode = objectMapper.createObjectNode()
                .put("strasse", address.getStrasse())
                .put("hausnummer", address.getHausnummer())
                .put("postleitzahl", address.getPostleitzahl())
                .put("stadt", address.getStadt())
                .put("land", address.getLand());    //only DE is supported

        ObjectNode requestBody = objectMapper.createObjectNode()
                .set("address", addressNode);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Make the POST request using the pre-configured RestTemplate
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, requestEntity, JsonNode.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // Assuming the response body is a JSON array of strings
                    JsonNode responseBody = response.getBody();
                    if (responseBody.isObject() && response.getBody().has("availableProducts")) {
                        JsonNode productsArrayNode = responseBody.get("availableProducts");
                        if (productsArrayNode.isArray()) {
                            List<String> productIds = StreamSupport.stream(productsArrayNode.spliterator(), false)
                                    .map(JsonNode::asText)
                                    .collect(Collectors.toList());
                            logger.info("Successfully fetched {} available product IDs.", productIds.size());
                            logger.debug("Available product IDs: {}", productIds);
                            return productIds;
                        } else {
                            logger.warn("Value of 'availableProducts' is not an array: {}", productsArrayNode.getNodeType());
                            return Collections.emptyList();
                        }
                    } else {
                        logger.warn("Response body is not an object or does not contain 'availableProducts' field: {}", responseBody.getNodeType());
                        return Collections.emptyList();
                    }
                } else {
                    logger.error("Failed to fetch available product IDs. Status: {} Body: {}", response.getStatusCode(), response.getBody());
                    return Collections.emptyList();
                }
            } catch (HttpClientErrorException e) {
                logger.error("Client error on attempt {}/{}: {} - {}", attempt, MAX_RETRIES, e.getStatusCode(), e.getResponseBodyAsString());
                break; // Don't retry on 4xx errors
            } catch (HttpServerErrorException e) {
                logger.warn("Server error on attempt {}/{}: {}. Retrying in {}ms...", attempt, MAX_RETRIES, e.getMessage(), RETRY_DELAY_MS);
            } catch (RestClientException e) {
                logger.warn("Rest client error on attempt {}/{}: {}. Retrying in {}ms...", attempt, MAX_RETRIES, e.getMessage(), RETRY_DELAY_MS);
            } catch (Exception e) {
                logger.error("Unexpected error on attempt {}/{}: {}", attempt, MAX_RETRIES, e.getMessage(), e);
                break;
            }
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry sleep interrupted", ie);
                    break;
                }
            } else {
                logger.error("Retries exhausted while fetching product IDs.");
            }
        }

        return Collections.emptyList();
    }


    /**
     * A custom iterable that generates Flux streams for each page of results.
     */
    class OfferLoader implements Iterable<Flux<InternetOffer>> {
        private final Queue<String> productIdsQueue = new ConcurrentLinkedQueue<>();
        private final RequestAddress address;

        public OfferLoader(RequestAddress address, List<String> productIdsToFetch) {
            this.address = address;
            this.productIdsQueue.addAll(productIdsToFetch);
        }

        @Override
        public Iterator<Flux<InternetOffer>> iterator() {
            return new Iterator<Flux<InternetOffer>>() {
                @Override
                public boolean hasNext() {
                    return !productIdsQueue.isEmpty();
                }

                @Override
                public synchronized Flux<InternetOffer> next() {
                    String productId = productIdsQueue.poll();
                    if (productId == null) {
                        return Flux.empty();
                    }
                    return fetchProductDetails(productId, address);
                }
            };
        }
    }

    /**
     * Fetches details for a single product ID.
     * Applies retry and timeout logic.
     */
    Flux<InternetOffer> fetchProductDetails(String productId, RequestAddress address) {

        String url = baseUrl + "/api/external/product-details/" + productId;
        Map<String, RequestAddress> requestBody = Collections.singletonMap("address", address);

        logger.info("Fetching offer for product {} from URL: {}", productId, url);
        logger.debug("Sending request for product {} with address: {}", productId, address);

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> {
                            HttpStatusCode statusCode = response.statusCode();
                            if (statusCode == HttpStatus.BAD_REQUEST || statusCode == HttpStatus.TOO_MANY_REQUESTS || statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
                                logger.warn("Received {} for product {}. Assuming no offers for this address.", statusCode, productId);
                                return Mono.empty(); // Return an empty Mono to signal no data for this request
                            } else {
                                logger.error("WebClient error for product {}: Status={}", productId, statusCode);
                                return Mono.error(new RuntimeException("WebClient error for product " + productId + ", status code: " + statusCode));
                            }
                        })
                .bodyToFlux(DetailedResponseData.class)
                .timeout(Duration.ofSeconds(50))
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                        .jitter(0.5)
                        .filter(throwable -> throwable instanceof ReadTimeoutException || (throwable instanceof WebClientResponseException ex && (ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429)))
                        .doBeforeRetry(retrySignal -> logger.warn("Retrying Product {} due to {}", productId, retrySignal.failure().toString()))
                        .onRetryExhaustedThrow((spec, signal) -> new RuntimeException("Retries exhausted for Offer " + productId, signal.failure()))
                )
                .doOnError(error -> logger.error("Error fetching Product {}: {}", productId, error.getMessage()))
                .flatMap(detailed -> Mono.justOrEmpty(mapToInternetOffer(detailed, productId)))
                .doOnNext(offer -> {
                    logger.trace("Mapped offer from id {}: {}", productId, offer);
                    putOfferInCache(offer);
                    logger.debug("Cached offer for productId {}", offer.getProductId());
                });
    }

    /**
     * Fetches all available internet offers for the given address using concurrent paginated requests.
     *
     * @param address The user's address
     * @return A Flux stream of InternetOffer objects
     */
    @Override
    public Flux<InternetOffer> getOffers(RequestAddress address) {
        //Servus speed currently only has support for Germany
        Instant startTime = Instant.now();
        if (!address.getLand().equals("DE")) {
            return Flux.empty();
        }

        List<String> availableIds = getAvailableProductIds(address);

        if (availableIds.isEmpty()) {
            logger.warn("No available product IDs found for the given address.");
            return Flux.empty();
        }

        List<InternetOffer> cachedOffers = new ArrayList<>();
        List<String> idsToFetch = new ArrayList<>();

        for (String id : availableIds) {
            InternetOffer cached = cache.getIfPresent(id);
            if (cached != null) {
                cachedOffers.add(cached);
            } else {
                idsToFetch.add(id);
            }
        }
        Flux<InternetOffer> cachedOffersFlux = Flux.fromIterable(cachedOffers);

        OfferLoader offerLoader = new OfferLoader(address, idsToFetch);
        List<Flux<InternetOffer>> parallelPageReaders = new ArrayList<>();
        for (int i = 0; i < PARALLEL; i++) {
            parallelPageReaders.add(Flux.concat(offerLoader));
        }
        Flux<InternetOffer> fetchedOffersFlux = Flux.merge(parallelPageReaders);

        return Flux.merge(cachedOffersFlux, fetchedOffersFlux)
                .doOnComplete(() -> {
                    Instant endTime = Instant.now();
                    long totalDuration = Duration.between(startTime, endTime).toMillis();
                    logger.info("Total time to fetch all ServusSpeed offers: {} ms with {} parallel requests.", totalDuration, PARALLEL);
                });
    }

    /**
     * Maps a {@link DetailedResponseData} object (representing a raw product detail from ServusSpeed)
     * to a standardized {@link InternetOffer} domain object.
     * Includes null checks to ensure that required nested objects and fields are present before mapping.
     *
     * @param product The {@link DetailedResponseData} received from the ServusSpeed API.
     * @param productId The ID of the product being mapped, used to set the {@code productId} in the {@link InternetOffer}.
     * @return An {@link InternetOffer} object if mapping is successful and all mandatory data is present,
     * otherwise {@code null} if essential data is missing.
     */
    private InternetOffer mapToInternetOffer(DetailedResponseData product, String productId) {

        if (product == null || product.getServusSpeedProduct() == null || product.getServusSpeedProduct().getProductInfo() == null || product.getServusSpeedProduct().getPricingDetails() == null) {
            logger.warn("Could not map product {} due to missing data.", productId);
            return null;
        }
        ServusSpeedProduct productDetails = product.getServusSpeedProduct();

        // Set to 0 or null if not available from this 'productDetails' source
        //logger.info("InternetOffer created: {}", offer);
        return InternetOffer.builder()
                .productId(productId)
                .providerName(productDetails.getProviderName())
                .speed(productDetails.getProductInfo().getSpeed())
                .monthlyCostInCent(productDetails.getPricingDetails().getMonthlyCostInCent())
                .afterTwoYearsMonthlyCost(0) // Set to 0 or null if not available from this 'productDetails' source
                .durationInMonths(productDetails.getProductInfo().getContractDurationInMonths())
                .connectionType(productDetails.getProductInfo().getConnectionType())
                .tv(productDetails.getProductInfo().getTv())
                .limitFrom(productDetails.getProductInfo().getLimitFrom())
                .maxAge(productDetails.getProductInfo().getMaxAge())
                .installationService(productDetails.getPricingDetails().isInstallationService())
                .discount(productDetails.getDiscount())
                .build();
    }
}
