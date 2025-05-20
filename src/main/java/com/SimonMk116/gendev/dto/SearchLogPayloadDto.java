package com.SimonMk116.gendev.dto;

public class SearchLogPayloadDto {
    private String anonymousUserId;
    private String timestamp;
    private AddressDataDto address;
    private FilterDataDto filters;
    private SortCriteriaDataDto sortCriteria;

    // Getters and Setters
    public String getAnonymousUserId() { return anonymousUserId; }
    public void setAnonymousUserId(String anonymousUserId) { this.anonymousUserId = anonymousUserId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public AddressDataDto getAddress() { return address; }
    public void setAddress(AddressDataDto address) { this.address = address; }
    public FilterDataDto getFilters() { return filters; }
    public void setFilters(FilterDataDto filters) { this.filters = filters; }
    public SortCriteriaDataDto getSortCriteria() { return sortCriteria; }
    public void setSortCriteria(SortCriteriaDataDto sortCriteria) { this.sortCriteria = sortCriteria; }

    @Override
    public String toString() {
        return "SearchLogPayloadDto{" +
                "anonymousUserId='" + anonymousUserId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", address=" + address +
                ", filters=" + filters +
                ", sortCriteria=" + sortCriteria +
                '}';
    }
}
