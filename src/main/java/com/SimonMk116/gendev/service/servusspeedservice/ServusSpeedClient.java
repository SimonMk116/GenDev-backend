package com.SimonMk116.gendev.service.servusspeedservice;

import com.SimonMk116.gendev.controller.OfferController;
import com.SimonMk116.gendev.model.*;
import io.netty.handler.timeout.ReadTimeoutException;
import jakarta.annotation.PostConstruct;
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
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ServusSpeedClient implements OfferController.InternetOfferService {

    @Autowired
    @Qualifier("servusSpeedWebClient")
    private WebClient webClient;

    @Autowired
    @Qualifier("servusSpeedRestTemplate")
    private RestTemplate restTemplate;

    @Value("${provider.servus.base-url}")
    private String baseUrl;

    private static final Logger logger = LoggerFactory.getLogger(ServusSpeedClient.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 300;
    static final int PARALLEL = 3;

    @PostConstruct
    public void preloadCache() {
        List<InternetOffer> offers = List.of(
                new InternetOffer("cb41cc8ecc08f2bc", "Servus Basic 50", 50, 12, "DSL", "ServusTV Standard", 100, 30, 2540, false, 0),
                new InternetOffer("2e451145abd4bb2b", "Servus Basic 75", 75, 12, "DSL", "ServusTV Standard", 100, 31, 2740, false, 0),
                new InternetOffer("95fe97a55dbeffe6", "Servus Basic 100", 100, 12, "DSL", "ServusTV Standard", 100, 31, 2940, false, 0),
                new InternetOffer("9973cf0203fda810", "Servus Plus 100", 100, 12, "Cable", "ServusTV Plus", 100, 31, 3140, false, 0),
                //new InternetOffer("419eae44025d5c2c", "Servus Plus 125", 125, 12, "Cable", "ServusTV Plus", 100, 31, 3340, false, 0),
                new InternetOffer("8ecb2ed91a2d8dc3", "Servus Plus 150", 150, 12, "Cable", "ServusTV Plus", 100, 31, 3540, false, 0),
                new InternetOffer("a82185d5bbe81d4c", "Servus Premium 150", 150, 24, "Fiber", "ServusFlix Premium", 150, 31, 3740, false, 0),
                new InternetOffer("63772dcfba0ed58c", "Servus Premium 175", 175, 24, "Fiber", "ServusFlix Premium", 150, 31, 3940, false, 0),
                //new InternetOffer("46fcb27acee8eec4", "Servus Premium 200", 200, 24, "Fiber", "ServusFlix Premium", 150, 31, 4140, false, 0),
                new InternetOffer("8feca3d260d23013", "Servus Ultra 200", 200, 24, "Fiber", "ServusFlix Pro", 150, 31, 4340, false, 0),
                new InternetOffer("e51ed833f573ec81", "Servus Ultra 225", 225, 24, "Fiber", "ServusFlix Pro", 150, 31, 4540, false, 0),
                new InternetOffer("8ff69629249d61f3", "Servus Ultra 250", 250, 24, "Fiber", "ServusFlix Pro", 150, 31, 4740, false, 0),
                new InternetOffer("04cba41b16902755", "Servus Extreme 300", 300, 36, "Fiber", "ServusFlix Pro Max Ultra", 200, 31, 5040, false, 0),
                new InternetOffer("6baacf63e5b97905", "Servus Extreme 350", 350, 36, "Fiber", "ServusFlix Pro Max Ultra", 200, 31, 5340, false, 0),
                new InternetOffer("1dd4e520c4653278", "Servus Extreme 400", 400, 36, "Fiber", "ServusFlix Pro Max Ultra", 200, 31, 5640, false, 0)
        );
        offers.forEach(this::putOfferInCache);
    }


    public void putOfferInCache(InternetOffer offer) {
        if (offer != null && offer.getProductId() != null) {
            cache.put(offer.getProductId(), offer);
            logger.info("Manually cached offer with productId {}", offer.getProductId());
        } else {
            logger.warn("Attempted to cache null or invalid offer: {}", offer);
        }
    }

    private final Map<String, InternetOffer> cache = new ConcurrentHashMap<>();

    private List<String> getAvailableProductIds(RequestAddress address) {
        String url = baseUrl + "/api/external/available-products";
        InternetAngeboteRequestData request = new InternetAngeboteRequestData();
        if (address == null) {
            logger.error("Address is null - cannot fetch Servus Speed offers.");
            return Collections.emptyList();
        }
        request.setAddress(address);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InternetAngeboteRequestData> entity = new HttpEntity<>(request, headers);
        int retryCount = 0;
        List<String> productIds = null;

        while(retryCount < MAX_RETRIES) {
            try {
                logger.debug("Sending request to Servus Speed with address: {}", address);
                ResponseEntity<InternetOffersResponseDataList> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        InternetOffersResponseDataList.class
                );
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    productIds = response.getBody().getAvailableProducts();
                    break; // Successful response, exit retry loop
                } else {
                    logger.warn("Servus Speed - Failed to get available product IDs. Status: {}", response.getStatusCode());
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                logger.warn("Servus Speed - Error getting available product IDs (Attempt {}/" + MAX_RETRIES + "): {}", retryCount + 1, e.getStatusCode());
                if (shouldRetry(e.getStatusCode())) {
                    retryCount++;
                    sleepForRetry();
                } else {
                    throw e; // Re-throw non-recoverable errors
                }
            } catch (RestClientException e) {
                logger.warn("Servus Speed - Request failed for available product IDs (Attempt {}/" + MAX_RETRIES + "): {}", retryCount + 1, e.getMessage());
                retryCount++;
                sleepForRetry();
            }
        }

        if (productIds == null) {
            logger.warn("Servus Speed - Max retries reached for getting available product IDs.");
        }
        return productIds != null ? productIds : new ArrayList<>();
    }

    private boolean shouldRetry(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError() || statusCode == HttpStatus.TOO_MANY_REQUESTS;
    }

    private void sleepForRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * A custom iterable that generates Flux streams for each page of results.
     */
    class OfferLoader implements Iterable<Flux<InternetOffer>> {
        volatile int idx = 0;
        private final RequestAddress address;
        private final List<String> productIdsToFetch;

        public OfferLoader(RequestAddress address, List<String> productIdsToFetch) {
            this.address = address;
            this.productIdsToFetch = productIdsToFetch;
        }

        @Override
        public Iterator<Flux<InternetOffer>> iterator() {
            return new Iterator<Flux<InternetOffer>>() {
                @Override
                public boolean hasNext() {
                    return idx < productIdsToFetch.size();
                }

                @Override
                public synchronized Flux<InternetOffer> next() {
                    return fetchProductDetails(OfferLoader.this.productIdsToFetch.get(idx++), OfferLoader.this.address);
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
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))    //TODO
                        .jitter(0.5)
                        .filter(throwable -> throwable instanceof ReadTimeoutException || (throwable instanceof WebClientResponseException ex && (ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429)))
                        .doBeforeRetry(retrySignal -> logger.warn("Retrying Product {} due to {}", productId, retrySignal.failure().toString()))
                        .onRetryExhaustedThrow((spec, signal) -> new RuntimeException("Retries exhausted for Offer " + productId, signal.failure()))
                )
                .doOnError(error -> logger.error("Error fetching Product {}: {}", productId, error.getMessage()))
                .map(detailed -> mapToInternetOffer(detailed, productId))
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
        Instant startTime = Instant.now();

        List<String> availableIds = getAvailableProductIds(address);

        if (availableIds.isEmpty()) {
            logger.warn("No available product IDs found for the given address.");
            return Flux.empty();
        }

        List<InternetOffer> cachedOffers = new ArrayList<>();
        List<String> idsToFetch = new ArrayList<>();

        for (String id : availableIds) {
            InternetOffer cached = cache.get(id);
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


    private InternetOffer mapToInternetOffer(DetailedResponseData product, String productId) {

        if (product == null || product.getServusSpeedProduct() == null || product.getServusSpeedProduct().getProductInfo() == null || product.getServusSpeedProduct().getPricingDetails() == null) {
            logger.warn("Could not map product {} due to missing data.", productId);
            return null;
        }
        ServusSpeedProduct productDetails = product.getServusSpeedProduct();

        InternetOffer offer =  new InternetOffer(
                productId,
                productDetails.getProviderName(),
                productDetails.getProductInfo().getSpeed(),
                productDetails.getProductInfo().getContractDurationInMonths(),
                productDetails.getProductInfo().getConnectionType(),
                productDetails.getProductInfo().getTv(),
                productDetails.getProductInfo().getLimitFrom(),
                productDetails.getProductInfo().getMaxAge(),
                productDetails.getPricingDetails().getMonthlyCostInCent(),
                productDetails.getPricingDetails().isInstallationService(),
                productDetails.getDiscount()
        );
        logger.info("InternetOffer created: {}", offer);
        return offer;
    }
}
