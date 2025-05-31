package com.SimonMk116.gendev.service;

import com.SimonMk116.gendev.model.RequestAddress;
import com.SimonMk116.gendev.service.servusspeedservice.ServusSpeedClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServusSpeedClientRestTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock // WebClient needs to be mocked even if not directly used in this test method, due to @Autowired in client
    private WebClient webClient;

    @InjectMocks
    private ServusSpeedClient servusSpeedClient;

    private ObjectMapper objectMapper;
    private RequestAddress testAddress;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Manually inject ObjectMapper since @Autowired is on the field and @InjectMocks won't do it automatically for ObjectMapper
        ReflectionTestUtils.setField(servusSpeedClient, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(servusSpeedClient, "baseUrl", "http://test-servusspeed-api.com");

        testAddress = new RequestAddress("TestStreet", "10", "TestCity", "12345", "DE");
    }

    private ObjectNode createProductIdsResponse(List<String> productIds) {
        ObjectNode responseNode = objectMapper.createObjectNode();
        ArrayNode productsArray = objectMapper.createArrayNode();
        productIds.forEach(productsArray::add);
        responseNode.set("availableProducts", productsArray);
        return responseNode;
    }

    @Test
    void getAvailableProductIds_shouldReturnProductIds_whenApiCallIsSuccessful() {
        // Arrange
        List<String> expectedIds = List.of("prod1", "prod2", "prod3");
        ObjectNode dummyResponse = createProductIdsResponse(expectedIds);
        ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(dummyResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(successResponse);

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertEquals(expectedIds.size(), result.size());
        assertTrue(result.containsAll(expectedIds));
        verify(restTemplate, times(1)).postForEntity(
                eq("http://test-servusspeed-api.com/api/external/available-products"), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsEmptyProductsArray() {
        // Arrange
        ObjectNode dummyResponse = createProductIdsResponse(List.of());
        ResponseEntity<JsonNode> successResponse = new ResponseEntity<>(dummyResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(successResponse);

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsNullBody() {
        // Arrange
        ResponseEntity<JsonNode> nullBodyResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(nullBodyResponse);

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsNonObjectResponse() {
        // Arrange (e.g., just a string or an array directly)
        JsonNode nonObjectResponse = objectMapper.createArrayNode().add("id1").add("id2"); // Not an object with "availableProducts"
        ResponseEntity<JsonNode> response = new ResponseEntity<>(nonObjectResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(response);

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_whenApiReturnsObjectWithoutAvailableProducts() {
        // Arrange (e.g., just a string or an array directly)
        ObjectNode missingFieldResponse = objectMapper.createObjectNode().put("someOtherField", "value");
        ResponseEntity<JsonNode> response = new ResponseEntity<>(missingFieldResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(response);

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_onHttpClientError() {
        // Arrange (4xx error)
        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_onHttpServerError() {
        // Arrange (5xx error)
        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProductIds_shouldReturnEmptyList_onRestClientException() {
        // Arrange (network error, etc.)
        when(restTemplate.postForEntity(
                any(String.class), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act
        List<String> result = ReflectionTestUtils.invokeMethod(servusSpeedClient, "getAvailableProductIds", testAddress);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
