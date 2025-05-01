package com.SimonMk116.gendev.dto;

public class VerbynDichResponse {
    private String product;
    private String description;
    private boolean last;
    private boolean valid;

    // Getters and Setters
    public String getProduct() {
        return product;
    }
    public void setProduct(String product) {
        this.product = product;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public boolean isLast() {
        return last;
    }
    public void setLast(boolean last) {
        this.last = last;
    }
    public boolean isValid() {
        return valid;
    }
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "VerbynDichResponse{" +
                "product='" + product + '\'' +
                ", description='" + description + '\'' +
                ", last=" + last +
                ", valid=" + valid +
                '}';
    }

}
