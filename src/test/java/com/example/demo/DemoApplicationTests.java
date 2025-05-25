package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // Import post for testing request body
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.SimonMk116.gendev.model.InternetOffer;
import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.controller.OfferController.InternetOfferService;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	// Mock all the services used by OfferController
	@MockBean
	private InternetOfferService byteMeService;
	@MockBean
	private InternetOfferService webWunderService;
	@MockBean
	private InternetOfferService pingPerfectService;
	@MockBean
	private InternetOfferService verbynDichService;
	@MockBean
	private InternetOfferService servusSpeedClient;

	@Test
	void contextLoads() {
	}

	@Test
	void testGetAllOffersWithValidAddress() throws Exception {
		// Arrange
		RequestAddress dummyAddress = new RequestAddress();
		dummyAddress.setStrasse("some_street");
		dummyAddress.setHausnummer("1");
		dummyAddress.setPostleitzahl("12345");
		dummyAddress.setStadt("some_city");
		dummyAddress.setLand("DE");
		// Create dummy offers to be returned by mocked services
		// Create dummy offers with more realistic data based on InternetOffer class
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

		// Stub the mocked services to return the dummy offers
		when(byteMeService.getOffers(eq(dummyAddress))).thenReturn(dummyOffersFlux);
		when(webWunderService.getOffers(eq(dummyAddress))).thenReturn(dummyOffersFlux);
		when(pingPerfectService.getOffers(eq(dummyAddress))).thenReturn(dummyOffersFlux);
		when(verbynDichService.getOffers(eq(dummyAddress))).thenReturn(dummyOffersFlux);
		when(servusSpeedClient.getOffers(eq(dummyAddress))).thenReturn(dummyOffersFlux);

		// Perform the GET request
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk());
		// Further assertions can be added here to check the response body
		// Using toString() output for basic content assertion
		// Note: A more robust approach would be to parse the SSE stream.
		// .andExpect(content().string(containsString("providerName='ByteMe'")))
		// .andExpect(content().string(containsString("providerName='WebWunder'")));
	}

	@Test
	void testGetAllOffersWithMissingAddressParams() throws Exception {
		// Perform the GET request with missing parameters
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				// houseNumber, city, plz are missing
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for missing parameters
	}

	@Test
	void testGetAllOffersWithInvalidCharactersInStreet() throws Exception {
		// Perform the GET request with invalid characters in street parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street@#$")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for invalid street
	}

	@Test
	void testGetAllOffersWithEmptyStreet() throws Exception {
		// Perform the GET request with an empty street parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for empty street
	}

	@Test
	void testGetAllOffersWithStreetTooLong() throws Exception {
		// Perform the GET request with a very long street parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "a".repeat(256)) // Assuming a max length, adjust if necessary
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for street too long
	}

	@Test
	void testGetAllOffersWithEmptyHouseNumber() throws Exception {
		// Perform the GET request with an empty house number parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for empty house number
	}

	@Test
	void testGetAllOffersWithHouseNumberTooLong() throws Exception {
		// Perform the GET request with a very long house number parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1".repeat(11)) // Assuming a max length, adjust if necessary
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for house number too long
	}

	@Test
	void testGetAllOffersWithEmptyCity() throws Exception {
		// Perform the GET request with an empty city parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for empty city
	}

	@Test
	void testGetAllOffersWithCityTooLong() throws Exception {
		// Perform the GET request with a very long city parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "a".repeat(256)) // Assuming a max length, adjust if necessary
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for city too long
	}

	@Test
	void testGetAllOffersWithEmptyPlz() throws Exception {
		// Perform the GET request with an empty plz parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for empty plz
	}

	@Test
	void testGetAllOffersWithMalformedJson() throws Exception {
		// Perform a POST request with malformed JSON in the request body
		// Note: Assuming the /api/offers endpoint accepts POST requests with JSON body for address data based on the context of testing malformed JSON.
		// If the endpoint only accepts GET with query parameters, this test might not be directly applicable or would need adjustment to test how
		// a malformed request *structure* is handled if not JSON. For the purpose of demonstrating malformed JSON handling, we assume a POST endpoint.
		String malformedJson = "{ \"street\": \"some_street\", \"houseNumber\": \"1\", \"city\": \"some_city\", \"plz\": \"12345\", "; // Malformed JSON

		mockMvc.perform(post("/api/offers") // Use post for sending a request body
				.contentType(MediaType.APPLICATION_JSON)
				.content(malformedJson))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for malformed JSON
	}

	@Test
	void testGetAllOffersWithInvalidJsonStructure() throws Exception {
		// Perform a POST request with valid JSON but incorrect structure for the expected DTO
		String invalidStructureJson = "{ \"address\": { \"streetName\": \"some_street\", \"number\": \"1\", \"cityName\": \"some_city\", \"postalCode\": \"12345\" } }"; // Valid JSON but incorrect keys/structure

		mockMvc.perform(post("/api/offers") // Use post for sending a request body
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidStructureJson))
				.andExpect(status().isBadRequest()); // Expecting a 400 bad request status for invalid JSON structure
	}

	@Test
	void testGetAllOffersWithStreetSqlInjectionAttempt() throws Exception {
		// Perform a GET request with a potential SQL injection attempt in the street parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "' OR '1'='1")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a bad request or other appropriate error status
	}

	@Test
	void testGetAllOffersWithHouseNumberSqlInjectionAttempt() throws Exception {
		// Perform a GET request with a potential SQL injection attempt in the houseNumber parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1; DROP TABLE users;")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a bad request or other appropriate error status
	}

	@Test
	void testGetAllOffersWithCitySqlInjectionAttempt() throws Exception {
		// Perform a GET request with a potential SQL injection attempt in the city parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city' OR '1'='1")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a bad request or other appropriate error status
	}

	@Test
	void testGetAllOffersWithPlzSqlInjectionAttempt() throws Exception {
		// Perform a GET request with a potential SQL injection attempt in the plz parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345'; DELETE FROM offers;")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a bad request or other appropriate error status
	}

	@Test
	void testGetAllOffersWithStreetXssAttempt() throws Exception {
		// Perform a GET request with a potential XSS attempt in the street parameter
		mockMvc.perform(get("/api/offers")
				.param("street", "<script>alert('XSS')</script>")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isBadRequest()); // Expecting a bad request or other appropriate error status
	}


	@Test
	void testGetAllOffersWhenNoOffersFound() throws Exception {
		// Arrange
		RequestAddress dummyAddress = new RequestAddress();
		dummyAddress.setStrasse("some_street");
		dummyAddress.setHausnummer("1");
		dummyAddress.setPostleitzahl("12345");
		dummyAddress.setStadt("some_city");
		dummyAddress.setLand("DE");
		// Stub the mocked services to return empty Fluxes
		when(byteMeService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(webWunderService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(pingPerfectService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(verbynDichService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(servusSpeedClient.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());

		// Perform the GET request
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk()) // Still expect 200 OK status even if no offers are found
				.andExpect(content().string("")); // Expect an empty response body for no offers
	}

	@Test
	void testGetAllOffersWhenServiceReturnsError() throws Exception {
		// Arrange
		RequestAddress dummyAddress = new RequestAddress();
		dummyAddress.setStrasse("some_street");
		dummyAddress.setHausnummer("1");
		dummyAddress.setPostleitzahl("12345");
		dummyAddress.setStadt("some_city");
		dummyAddress.setLand("DE");
		// Stub one of the mocked services to return an error Flux
		when(byteMeService.getOffers(eq(dummyAddress))).thenReturn(Flux.error(new RuntimeException("Simulated service error")));
		// Stub other services to return empty Fluxes or normal offers
		when(webWunderService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(pingPerfectService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(verbynDichService.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());
		when(servusSpeedClient.getOffers(eq(dummyAddress))).thenReturn(Flux.empty());

		// Perform the GET request
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().is5xxServerError()); // Expect a 500 server error status due to the service error
	}

	@Test
	void testGetAllOffersPerformanceWithLargeResponse() throws Exception {
		// Arrange
		RequestAddress dummyAddress = new RequestAddress();
		dummyAddress.setStrasse("some_street");
		dummyAddress.setHausnummer("1");
		dummyAddress.setPostleitzahl("12345");
		dummyAddress.setStadt("some_city");
		dummyAddress.setLand("DE");

		// Create a large number of dummy offers
		int numberOfOffers = 1000; // Adjust based on what's considered "large" for your application
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

		// Stub the mocked services to return the large number of dummy offers
		when(byteMeService.getOffers(eq(dummyAddress))).thenReturn(largeDummyOffersFlux);
		when(webWunderService.getOffers(eq(dummyAddress))).thenReturn(largeDummyOffersFlux);
		when(pingPerfectService.getOffers(eq(dummyAddress))).thenReturn(largeDummyOffersFlux);
		when(verbynDichService.getOffers(eq(dummyAddress))).thenReturn(largeDummyOffersFlux);
		when(servusSpeedClient.getOffers(eq(dummyAddress))).thenReturn(largeDummyOffersFlux);

		// Measure the time taken for the request
		long startTime = System.currentTimeMillis();
		mockMvc.perform(get("/api/offers")
				.param("street", "some_street")
				.param("houseNumber", "1")
				.param("city", "some_city")
				.param("plz", "12345")
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk()); // Expect OK status
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// Assert that the response time is within an acceptable limit (e.g., 5000 ms or 5 seconds)
		// This limit should be determined based on your performance requirements.
		long acceptableResponseTimeMillis = 5000; // 5 seconds
		assertTrue(duration < acceptableResponseTimeMillis, "Request with large response took too long: " + duration + "ms");
	}

}

