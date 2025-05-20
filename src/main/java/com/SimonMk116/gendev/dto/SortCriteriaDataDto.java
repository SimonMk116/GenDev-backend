package com.SimonMk116.gendev.dto;

public class SortCriteriaDataDto {
    private String key;
    private String order;

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    @Override
    public String toString() {
        return "SortCriteriaDto{" +
                "key='" + key + '\'' +
                ", order='" + order + '\'' +
                '}';
    }
}
