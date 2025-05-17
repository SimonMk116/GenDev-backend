        package com.SimonMk116.gendev.service.servusspeedservice;

        import com.SimonMk116.gendev.controller.OfferController;
        import com.SimonMk116.gendev.model.*;
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
        import java.util.concurrent.ConcurrentHashMap;

        @Service
        @RequiredArgsConstructor
        public class ServusSpeedClient implements OfferController.InternetOfferService {

            /*@Autowired
            @Qualifier("servusSpeedRestTemplate")
            private RestTemplate restTemplate;*/

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
            static final int PARALLEL = 5;

            private static List<String> productIds = Arrays.asList(
                    "cb41cc8ecc08f2bc",
                    "2e451145abd4bb2b",
                    "95fe97a55dbeffe6",
                    "9973cf0203fda810",
                    "419eae44025d5c2c",
                    "8ecb2ed91a2d8dc3",
                    "a82185d5bbe81d4c",
                    "63772dcfba0ed58c",
                    "46fcb27acee8eec4",
                    "8feca3d260d23013",
                    "e51ed833f573ec81",
                    "8ff69629249d61f3",
                    "04cba41b16902755",
                    "6baacf63e5b97905",
                    "1dd4e520c4653278"
            );

            private static final Map<String, InternetOffer> cache = new ConcurrentHashMap<>();

//new

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

//^^^new

            /**
             * A custom iterable that generates Flux streams for each page of results.
             */
            class PageProvider implements Iterable<Flux<InternetOffer>> {
                volatile int idx = 0;
                RequestAddress address;

                public PageProvider(RequestAddress address) {
                    this.address = address;
                }

                @Override
                public Iterator<Flux<InternetOffer>> iterator() {
                    return new Iterator<Flux<InternetOffer>>() {
                        @Override
                        public boolean hasNext() {
                            return idx < productIds.size();
                        }

                        @Override
                        public synchronized Flux<InternetOffer> next() {
                            return pageLoader(PageProvider.this, idx++);
                        }
                    };
                }
            }

            /**
             * Loads a single page of offers and returns them as a Flux.
             * Applies retry and timeout logic.
             */
            Flux<InternetOffer> pageLoader(PageProvider provider, int idx) {

                String url = baseUrl + "/api/external/product-details/" + productIds.get(idx);
                Map<String, RequestAddress> requestBody = Collections.singletonMap("address", provider.address);

                logger.info("Fetching offer {} from URL: {}", productIds.get(idx), url);
                logger.debug("Sending request for product {} with address: {}", productIds.get(idx), provider.address);

                return webClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                response -> {
                                    // Specific handling for 400 Bad Request
                                    if (response.statusCode() == HttpStatus.BAD_REQUEST || response.statusCode() == HttpStatus.TOO_MANY_REQUESTS || response.statusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                                        logger.warn("Received {} for product {}. Assuming no offers for this address.", response.statusCode(), productIds.get(idx));
                                        return Mono.empty(); // Return an empty Mono to signal no data for this request
                                    } else {
                                        logger.error("WebClient error for product {}: Status={}", productIds.get(idx), response.statusCode());
                                        return Mono.error(new RuntimeException("WebClient error for product " + productIds.get(idx) + ", status code: " + response.statusCode()));
                                    }
                                })
                        .bodyToFlux(DetailedResponseData.class)
                        .timeout(Duration.ofSeconds(50))
                        .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))    //TODO
                                .jitter(0.5)
                                .filter(throwable -> throwable instanceof ReadTimeoutException || (throwable instanceof WebClientResponseException ex && (ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429)))
                                .doBeforeRetry(retrySignal -> logger.warn("Retrying Product {} due to {}", productIds.get(idx), retrySignal.failure().toString()))
                                .onRetryExhaustedThrow((spec, signal) -> new RuntimeException("Retries exhausted for Offer " + productIds.get(idx), signal.failure()))
                        )
                        .doOnError(error -> logger.error("Error fetching Product {}: {}", productIds.get(idx), error.getMessage()))
                        //.doOnComplete(() -> logger.info("Successfully processed page {}", page))
                        .map(detailed -> mapToInternetOffer(detailed, productIds.get(idx)))
                        .doOnNext(offer -> logger.trace("Mapped offer from id {}: {}", productIds.get(idx), offer))
                        ;
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

                PageProvider pageProvider = new PageProvider(address);
                if (false) return pageProvider.iterator().next();

                if (false) return Flux.concat(pageProvider);

                List<Flux<InternetOffer>> parallelPageReaders = new ArrayList<>();
                for (int i = 0; i < PARALLEL; i++) {
                    parallelPageReaders.add(Flux.concat(pageProvider)); //remove null ,offers check i
                }
                return Flux.merge(parallelPageReaders)
                        .doOnComplete(() -> {
                            Instant endTime = Instant.now();
                            long totalDuration = Duration.between(startTime, endTime).toMillis();
                            logger.info("Total time to fetch all ServusSpeed offers: {} ms with {} parallel requests.", totalDuration, PARALLEL);
                        });
            }


            private InternetOffer mapToInternetOffer(DetailedResponseData product, String productId) {
                //logger.info("mapToInternetOffer {}", response.toString());
                ServusSpeedProduct productDetails = product.getServusSpeedProduct();

                // Initialize default values

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
                System.out.println("InternetOffer created: " + offer);
                return offer;
            }
        }

