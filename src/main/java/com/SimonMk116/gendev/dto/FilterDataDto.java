package com.SimonMk116.gendev.dto;

public class FilterDataDto {
    private String provider;
    private Integer minPrice; // In Cent
    private Integer maxPrice; // In Cent
    private Integer minSpeed; // In Mbps
    private String connectionType;
    private Boolean isYoungTariff;
    private String contractDuration;
    private Boolean tvIncluded;
    private Boolean freeInstallation;
    private String street;
    private String houseNumber;
    private String city;
    private String plz;

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Integer getMinPrice() { return minPrice; }
    public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }
    public Integer getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Integer maxPrice) { this.maxPrice = maxPrice; }
    public Integer getMinSpeed() { return minSpeed; }
    public void setMinSpeed(Integer minSpeed) { this.minSpeed = minSpeed; }
    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    public Boolean getIsYoungTariff() { return isYoungTariff; }
    public void setIsYoungTariff(Boolean youngTariff) { isYoungTariff = youngTariff; }
    public String getContractDuration() { return contractDuration; }
    public void setContractDuration(String contractDuration) { this.contractDuration = contractDuration; }
    public Boolean getTvIncluded() { return tvIncluded; }
    public void setTvIncluded(Boolean tvIncluded) { this.tvIncluded = tvIncluded; }
    public Boolean getFreeInstallation() { return freeInstallation; }
    public void setFreeInstallation(Boolean freeInstallation) { this.freeInstallation = freeInstallation; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPlz() { return plz; }
    public void setPlz(String plz) { this.plz = plz; }

    @Override
    public String toString() {
        return "FiltersDto{" +
                "provider='" + provider + '\'' +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", minSpeed=" + minSpeed +
                ", connectionType='" + connectionType + '\'' +
                ", isYoungTariff=" + isYoungTariff +
                ", contractDuration='" + contractDuration + '\'' +
                ", tvIncluded=" + tvIncluded +
                ", freeInstallation=" + freeInstallation +
                ", street='" + street + '\'' +
                ", houseNumber='" + houseNumber + '\'' +
                ", city='" + city + '\'' +
                ", plz='" + plz + '\'' +
                '}';
    }
}
