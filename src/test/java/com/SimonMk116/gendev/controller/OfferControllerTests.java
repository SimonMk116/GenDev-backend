package com.SimonMk116.gendev.controller;

import com.SimonMk116.gendev.Application;
import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.bytemeservice.ByteMeService;
import com.SimonMk116.gendev.service.pingperfectservice.PingPerfectService;
import com.SimonMk116.gendev.service.servusspeedservice.ServusSpeedClient;
import com.SimonMk116.gendev.service.verbyndichservice.VerbynDichService;
import com.SimonMk116.gendev.service.webwunderservice.WebWunderService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class OfferControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ByteMeService byteMeService;
	@MockBean
	private WebWunderService webWunderService;
	@MockBean
	private PingPerfectService pingPerfectService;
	@MockBean
	private VerbynDichService verbynDichService;
	@MockBean
	private ServusSpeedClient servusSpeedClient;

	@BeforeEach
	void setup() {
		// Default behavior for all mocks: return empty flux to prevent NullPointerExceptions
		// This makes sure tests where service interaction isn't the focus don't fail
		when(byteMeService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
		when(webWunderService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
		when(pingPerfectService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
		when(verbynDichService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
		when(servusSpeedClient.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
	}

	@Test
	void contextLoads() {
		// Simple test to ensure the Spring context loads correctly
	}

	@Test
	void testGetAllOffersWithValidAddress() throws Exception {
		// Arrange
		// For mocks, we typically use any(RequestAddress.class) unless we specifically
		// need to match on the exact address object, which requires RequestAddress
		// to implement equals() and hashCode().
		// For this test, we just want to ensure some offers are returned.
		InternetOffer offer1 = new InternetOffer();
		offer1.setProviderName("ByteMe");
		offer1.setSpeed(100);
		offer1.setMonthlyCostInCent(2500); // 25.00 EUR
		offer1.setDurationInMonths(24);

		InternetOffer offer2 = new InternetOffer();
		offer2.setProviderName("WebWunder");
		offer2.setSpeed(50);
		offer2.setMonthlyCostInCent(1999); // 19.99 EUR
		offer2.setDurationInMonths(12);

		List<InternetOffer> dummyOffers = new ArrayList<>();
		dummyOffers.add(offer1);
		dummyOffers.add(offer2);
		Flux<InternetOffer> dummyOffersFlux = Flux.fromIterable(dummyOffers);

		// Stub the mocked services to return the dummy offers for ANY RequestAddress
		when(byteMeService.getOffers(any(RequestAddress.class))).thenReturn(dummyOffersFlux);
		when(webWunderService.getOffers(any(RequestAddress.class))).thenReturn(dummyOffersFlux);
		// We can choose to mock only one service to return offers for simplicity if the goal is just "happy path"
		// For a full integration test, you might want distinct mocks per service.

		// Perform the GET request
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Byte"))) // Assert content for reactive stream
				.andExpect(content().string(org.hamcrest.Matchers.containsString("WebWunder")));
	}

	@Test
	void testGetAllOffersWithMissingRequiredAddressParams_shouldReturnBadRequest() throws Exception {
		// Perform the GET request with only 'street' provided.
		// 'city', 'plz', and 'land' are still @NotBlank and required.
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet") // Only street is provided
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect MissingServletRequestParameterException for the *other* missing required params
					assertInstanceOf(MissingServletRequestParameterException.class, resolvedException, "Expected MissingServletRequestParameterException");
					String errorMessage = resolvedException.getMessage();
					// Adjust the assertion to check for *any* of the missing required parameters
					assertTrue(errorMessage.contains("Required request parameter 'city' for method parameter type String is not present") ||
									errorMessage.contains("Required request parameter 'plz' for method parameter type String is not present") ||
									errorMessage.contains("Required request parameter 'land' for method parameter type String is not present"),
							"Expected message for missing 'city', 'plz', or 'land' parameter");
				});
	}


	@Test
	void testGetAllOffersWithInvalidCharactersInStreet() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet@#$") // Invalid characters
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.street: Invalid characters in street name"), "Expected 'Invalid characters in street name' message");
				});
	}

	@Test
	void testGetAllOffersWithEmptyStreet() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "") // Empty street
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.street: must not be blank"), "Expected 'must not be blank' message for street");
				});
	}

	@Test
	void testGetAllOffersWithStreetTooLong() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "a".repeat(101)) // Max size is 100
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.street: size must be between 0 and 100"), "Expected 'size must be between 0 and 100' message for street");
				});
	}

	@Test
	void testGetAllOffersWithInvalidHouseNumber() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1@#") // Invalid characters
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.houseNumber: Invalid characters in house number"), "Expected 'Invalid characters in house number' message");
				});
	}

	@Test
	void testGetAllOffersWithEmptyHouseNumber_shouldReturnOk() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "") // Empty house number, which is now allowed
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk()); // Expect 200 OK now
		// No exception expected for empty houseNumber anymore
	}

	@Test
	void testGetAllOffersWithHouseNumberTooLong() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1".repeat(11)) // Max size is 10
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.houseNumber: size must be between 0 and 10"),
							"Expected 'size must be between 0 and 10' message for house number");
					String responseBody = result.getResponse().getContentAsString();
					assertTrue(responseBody.contains("Validation error: getOffers.houseNumber: size must be between 0 and 10"),
							"Expected response body to contain the validation error message.");
				});
	}

	@Test
	void testGetAllOffersWithInvalidCity() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity@#$") // Invalid characters
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.city: Invalid characters in city name"), "Expected 'Invalid characters in city name' message");
				});
	}


	@Test
	void testGetAllOffersWithEmptyCity() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "") // Empty city
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.city: must not be blank"), "Expected 'must not be blank' message for city");
				});
	}

	@Test
	void testGetAllOffersWithCityTooLong() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "a".repeat(101)) // Max size is 100
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.city: size must be between 0 and 100"), "Expected 'size must be between 0 and 100' message for city");
				});
	}

	@Test
	void testGetAllOffersWithInvalidPlz() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "123") // Invalid format
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.plz: Invalid characters in postal code"), "Expected 'Invalid characters in postal code' message");
				});
	}

	@Test
	void testGetAllOffersWithEmptyPlz() throws Exception {
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "") // Empty plz
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.plz: must not be blank"), "Expected 'must not be blank' message for plz");
				});
	}

	@Test
	void testGetAllOffersWithMalformedJson() throws Exception {
		// This test is ONLY relevant for a POST endpoint accepting @RequestBody JSON
		// Your /api/offers endpoint is a GET endpoint with @RequestParam.
		// Therefore, this test will correctly fail with a 405 Method Not Allowed if you call it with POST.
		// If you were to have a POST endpoint, you would remove the get() and use post() and specify content type correctly.
		String malformedJson = "{ \"street\": \"somestreet\", \"houseNumber\": \"1\", \"city\": \"somecity\", \"plz\": \"12345\", "; // Malformed JSON

		mockMvc.perform(post("/api/offers") // Using POST as this test implies a request body
						.contentType(MediaType.APPLICATION_JSON)
						.content(malformedJson))
				.andExpect(status().isMethodNotAllowed()); // Expecting 405 since it's a GET endpoint
	}

	@Test
	void testGetAllOffersWithInvalidJsonStructure() throws Exception {
		// This test is ONLY relevant for a POST endpoint accepting @RequestBody JSON
		// Your /api/offers endpoint is a GET endpoint with @RequestParam.
		String invalidStructureJson = "{ \"address\": { \"streetName\": \"somestreet\", \"number\": \"1\", \"cityName\": \"somecity\", \"postalCode\": \"12345\" } }"; // Valid JSON but incorrect keys/structure

		mockMvc.perform(post("/api/offers") // Using POST as this test implies a request body
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidStructureJson))
				.andExpect(status().isMethodNotAllowed()); // Expecting 405 since it's a GET endpoint
	}


	@Test
	void testGetAllOffersWithStreetSqlInjectionAttempt() throws Exception {
		// SQL injection attempt in street: contains ' and ; which are not allowed by @Pattern
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet'; DROP TABLE users;--")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.street: Invalid characters in street name"), "Expected 'Invalid characters in street name' message");
				});
	}

	@Test
	void testGetAllOffersWithHouseNumberSqlInjectionAttempt() throws Exception {
		// SQL injection attempt in houseNumber: contains ; which is not allowed by @Pattern
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1; DROP TABLE users;")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.houseNumber: Invalid characters in house number"), "Expected 'Invalid characters in house number' message");
				});
	}

	@Test
	void testGetAllOffersWithCitySqlInjectionAttempt() throws Exception {
		// SQL injection attempt in city: contains ' and ; which are not allowed by @Pattern
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity' OR '1'='1") // SQL injection attempt in city
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.city: Invalid characters in city name"), "Expected 'Invalid characters in city name' message");
				});
	}

	@Test
	void testGetAllOffersWithPlzSqlInjectionAttempt() throws Exception {
		// SQL injection attempt in plz: contains ; and space which are not allowed by @Pattern
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345'; DELETE FROM offers;")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.plz: Invalid characters in postal code"), "Expected 'Invalid characters in postal code' message");
				});
	}

	@Test
	void testGetAllOffersWithStreetXssAttempt() throws Exception {
		// XSS attempt: contains < and > which are not allowed by @Pattern
		mockMvc.perform(get("/api/offers")
						.param("street", "<script>alert('XSS')</script>")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					Throwable resolvedException = result.getResolvedException();
					// Expect ConstraintViolationException directly
					assertInstanceOf(ConstraintViolationException.class, resolvedException,
							"Expected ConstraintViolationException to be resolved directly.");
					String errorMessage = resolvedException.getMessage();
					assertTrue(errorMessage.contains("getOffers.street: Invalid characters in street name"), "Expected 'Invalid characters in street name' message");
				});
	}

	@Test
	void testGetAllOffersWhenNoOffersFound() throws Exception {
		// This test's setup is fine as it uses the default empty Flux return from mocks
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk())
				.andExpect(content().string("")); // Expect an empty response body for no offers
	}

	@Test
	void testGetAllOffersWhenServiceReturnsError() throws Exception {
		// Arrange - specifically mock one service to throw an error
		when(byteMeService.getOffers(any(RequestAddress.class))).thenReturn(Flux.error(new RuntimeException("Simulated service error")));
        when(webWunderService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
        when(pingPerfectService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
        when(verbynDichService.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());
        when(servusSpeedClient.getOffers(any(RequestAddress.class))).thenReturn(Flux.empty());


        MvcResult result = mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk()) // Still expect 200 OK, as per SSE error handling
                .andReturn();
        // Assert that the body is effectively empty or contains no valid SSE events
        // For an immediate error, it's likely to be empty.
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.isEmpty(), "Expected an empty response body on service error for SSE");
	}

	@Test
	void testGetAllOffersPerformanceWithLargeResponse() throws Exception {
		// Arrange
		int numberOfOffers = 1000;
		List<InternetOffer> largeDummyOffers = new ArrayList<>();
		for (int i = 0; i < numberOfOffers; i++) {
			InternetOffer offer = new InternetOffer();
			offer.setProviderName("Provider" + i);
			offer.setSpeed(100 + i);
			offer.setMonthlyCostInCent(2000 + i);
			offer.setDurationInMonths(12 + (i % 24));
			largeDummyOffers.add(offer);
		}
		Flux<InternetOffer> largeDummyOffersFlux = Flux.fromIterable(largeDummyOffers);

		// Stub ONE mocked service to return the large number of dummy offers
		// This simulates a large response from one provider.
		when(byteMeService.getOffers(any(RequestAddress.class))).thenReturn(largeDummyOffersFlux);

		// Measure the time taken for the request
		long startTime = System.currentTimeMillis();
		mockMvc.perform(get("/api/offers")
						.param("street", "somestreet")
						.param("houseNumber", "1")
						.param("city", "somecity")
						.param("plz", "12345")
						.param("land", "DE")
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk()); // Expect OK status
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Assert that the response time is within an acceptable limit (e.g., 5000 ms or 5 seconds)
		long acceptableResponseTimeMillis = 5000; // 5 seconds
		assertTrue(duration < acceptableResponseTimeMillis, "Request with large response took too long: " + duration + "ms");
	}

}