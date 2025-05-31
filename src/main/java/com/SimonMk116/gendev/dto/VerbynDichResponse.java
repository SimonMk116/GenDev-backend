package com.SimonMk116.gendev.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerbynDichResponse {
    private String product;
    private String description;
    private boolean last;
    private boolean valid;
}
