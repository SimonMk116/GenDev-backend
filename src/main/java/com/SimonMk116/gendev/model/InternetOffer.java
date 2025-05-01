package com.SimonMk116.gendev.model;

import java.util.Objects;

public class InternetOffer {
    private int productId;
    private String providerName;
    private int speed;
    private int monthlyCostInCent;
    private int afterTwoYearsMonthlyCost;
    private int contractDurationInMonths;
    private String connectionType;

    //Constructors
    public InternetOffer() {}

    public InternetOffer(int productId, String providerName, int speed, int monthlyCostInCent, int afterTwoYearsMonthlyCost) {
        this.productId = productId;
        this.providerName = providerName;
        this.speed = speed;
        this.monthlyCostInCent = monthlyCostInCent;
        this.afterTwoYearsMonthlyCost = afterTwoYearsMonthlyCost;
        this.contractDurationInMonths = 24;
        this.connectionType = "DSL";
    }
    // Constructor with all fields
    public InternetOffer(int productId, String providerName, int speed, int monthlyCostInCent, int afterTwoYearsMonthlyCost, int contractDurationInMonths, String connectionType) {
        this.productId = productId;
        this.providerName = providerName;
        this.speed = speed;
        this.monthlyCostInCent = monthlyCostInCent;
        this.afterTwoYearsMonthlyCost = afterTwoYearsMonthlyCost;
        this.contractDurationInMonths = contractDurationInMonths;
        this.connectionType = connectionType;
    }

    //Getters & Setters

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProviderName() {
        return providerName;
    }
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    public int getSpeed() {
        return speed;
    }
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    public int getMonthlyCostInCent() {
        return monthlyCostInCent;
    }
    public void setMonthlyCostInCent(int price) {
        this.monthlyCostInCent = price;
    }
    public int getAfterTwoYearsMonthlyCost() {
        return afterTwoYearsMonthlyCost;
    }
    public void setAfterTwoYearsMonthlyCost(int afterTwoYearsMonthlyCost) {
        this.afterTwoYearsMonthlyCost = afterTwoYearsMonthlyCost;
    }
    public int getContractDurationInMonths() {
        return contractDurationInMonths;
    }
    public void setContractDurationInMonths(int contractDurationInMonths) {
        this.contractDurationInMonths = contractDurationInMonths;
    }
    public String getConnectionType() {
        return connectionType;
    }
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public String toString() {
        return '\'' + "InternetOffer{" +
                "productId='" + productId + '\'' +
                ", providerName='" + providerName + '\'' +
                ", speed=" + speed +
                ", monthlyCostInCent=" + monthlyCostInCent +
                ", afterTwoYearsMonthlyCost=" + afterTwoYearsMonthlyCost +
                '}' + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternetOffer that)) return false;
        return speed == that.speed &&
                Objects.equals(providerName, that.providerName) &&
                Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, providerName, speed);
    }
}
