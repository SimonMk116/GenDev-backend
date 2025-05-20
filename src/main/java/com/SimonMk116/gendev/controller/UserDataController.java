package com.SimonMk116.gendev.controller;

import com.SimonMk116.gendev.dto.SearchLogPayloadDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-activity")
public class UserDataController {
    private static final Logger activityLogger = LoggerFactory.getLogger("UserActivityLogger");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/log-search")
    public ResponseEntity<Void> logSearchActivity(@RequestBody SearchLogPayloadDto payload) {
        try {
            // Convert the payload to a JSON string for structured logging
            String jsonPayload = objectMapper.writeValueAsString(payload);
            activityLogger.info(jsonPayload); // Log the JSON string
        } catch (Exception e) {
            // Log serialization error
            System.err.println("Error serializing payload for logging: " + e.getMessage());
        }
        System.out.println("Received user search activity (also to console): " + payload.toString());
        return ResponseEntity.ok().build();
    }
}
