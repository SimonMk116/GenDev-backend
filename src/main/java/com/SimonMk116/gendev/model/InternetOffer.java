package com.SimonMk116.gendev.model;

import java.util.Objects;


public class InternetOffer {
    private String productId;
    private String providerName;
    private int speed;
    private int monthlyCostInCent;
    private int afterTwoYearsMonthlyCost;
    private String connectionType;
    private int durationInMonths;
    private boolean installationService;
    private String tv;
    private Integer limitFrom;
    private Integer maxAge;
    private String voucherType; // "PERCENTAGE" or "ABSOLUTE"
    private Integer voucherValue;
    private int discount;
    private int discountDuration;
    private int discountCap;
    //private String voucherType; //use above
    private Integer percentage; // only for percentageVoucher
    private Integer maxDiscountInCent; // only for percentageVoucher
    private Integer discountInCent; // only for absoluteVoucher
    private Integer minOrderValueInCent; // only for absoluteVoucher


    //Constructors
    //Default
    public InternetOffer() {
    }

    //ByteMe
    public InternetOffer(
            String productId,
            String providerName,
            int speed,
            int monthlyCostInCent,
            int afterTwoYearsMonthlyCost,
            int durationInMonths,
            String connectionType,
            boolean installationService,
            String tv,
            Integer limitFrom,
            Integer maxAge,
            String voucherType,
            Integer voucherValue
    ) {
        this.productId = productId;
        this.providerName = providerName;
        this.speed = speed;
        this.monthlyCostInCent = monthlyCostInCent;
        this.afterTwoYearsMonthlyCost = afterTwoYearsMonthlyCost;
        this.durationInMonths = durationInMonths;
        this.connectionType = connectionType;
        this.installationService = installationService;
        this.tv = tv;
        this.limitFrom = limitFrom;
        this.maxAge = maxAge;
        this.voucherType = voucherType;
        this.voucherValue = voucherValue;
    }

    //PingPerfect
    public InternetOffer(
            String providerName,
            String productId,
            int speed,
            int durationInMonths,
            String connectionType,
            String tv,
            Integer limitFrom,
            Integer maxAge,
            int monthlyCostInCent,
            boolean installationService
    ) {
        this.providerName = providerName;
        this.productId = productId;
        this.speed = speed;
        this.durationInMonths = durationInMonths;
        this.connectionType = connectionType;
        this.tv = tv;
        this.limitFrom = limitFrom;
        this.maxAge = maxAge;
        this.monthlyCostInCent = monthlyCostInCent;
        this.installationService = installationService;
    }

    //ServusSpeed
    public InternetOffer(
            String productId,
            String providerName,
            int speed,
            int durationInMonths,
            String connectionType,
            String tv,
            int limitFrom,
            int maxAge,
            int monthlyCostInCent,
            boolean installationService,
            int discount
    ) {
        this.productId = productId;
        this.providerName = providerName;
        this.speed = speed;
        this.durationInMonths = durationInMonths;
        this.connectionType = connectionType;
        this.tv = tv;
        this.limitFrom = limitFrom;
        this.maxAge = maxAge;
        this.monthlyCostInCent = monthlyCostInCent;
        this.installationService = installationService;
        this.discount = discount;
    }

    //VerbynDich
    public InternetOffer(
            String providerName,
            int speed,
            int monthlyCostInCent,
            int afterTwoYearsMonthlyCost,
            int durationInMonths,
            Integer maxAge,
            int discountPercentage,
            int discountDuration,
            int discountCap,
            String tv,
            String connectionType,
            int limitFrom
    ) {
        this.providerName = providerName;
        this.speed = speed;
        this.monthlyCostInCent = monthlyCostInCent;
        this.afterTwoYearsMonthlyCost = afterTwoYearsMonthlyCost;
        this.durationInMonths = durationInMonths;
        this.maxAge = maxAge;
        this.discount = discountPercentage;
        this.discountDuration = discountDuration;
        this.discountCap = discountCap;
        this.tv = tv;
        this.connectionType = connectionType;
        this.limitFrom = limitFrom;
    }

    //WebWunder
    public InternetOffer(String productId,
                         String providerName,
                         int speed,
                         int monthlyCostInCent,
                         int afterTwoYearsMonthlyCost,
                         Integer durationInMonths,
                         String connectionType,
                         String voucherType,
                         Integer percentage,
                         Integer maxDiscountInCent,
                         Integer discountInCent,
                         Integer minOrderValueInCent) {
        this.productId = productId;
        this.providerName = providerName;
        this.speed = speed;
        this.monthlyCostInCent = monthlyCostInCent;
        this.afterTwoYearsMonthlyCost = afterTwoYearsMonthlyCost;
        this.durationInMonths = durationInMonths;
        this.connectionType = connectionType;
        this.voucherType = voucherType;
        this.percentage = percentage;
        this.maxDiscountInCent = maxDiscountInCent;
        this.discountInCent = discountInCent;
        this.minOrderValueInCent = minOrderValueInCent;
    }

    //Getters & Setters
    public String getProductId() {
        return productId;
    }
    public void setProductId(String productId) {
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
    public String getConnectionType() {
        return connectionType;
    }
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
    public int getDurationInMonths() {
        return durationInMonths;
    }
    public void setDurationInMonths(int durationInMonths) {
        this.durationInMonths = durationInMonths;
    }
    public boolean isInstallationService() {
        return installationService;
    }
    public void setInstallationService(boolean installationService) {
        this.installationService = installationService;
    }
    public Integer getLimitFrom() {
        return limitFrom;
    }
    public void setLimitFrom(Integer limitFrom) {
        this.limitFrom = limitFrom;
    }
    public Integer getMaxAge() {
        return maxAge;
    }
    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }
    public String getVoucherType() {
        return voucherType;
    }
    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }
    public Integer getVoucherValue() {
        return voucherValue;
    }
    public void setVoucherValue(Integer voucherValue) {
        this.voucherValue = voucherValue;
    }
    public int getDiscount() {
        return discount;
    }
    public void setDiscount(int discount) {
        this.discount = discount;
    }
    public String getTv() {
        return tv;
    }
    public void setTv(String tv) {
        this.tv = tv;
    }
    public int getDiscountDuration() {
        return discountDuration;
    }
    public void setDiscountDuration(int discountDuration) {
        this.discountDuration = discountDuration;
    }
    public int getDiscountCap() {
        return discountCap;
    }
    public void setDiscountCap(int discountCap) {
        this.discountCap = discountCap;
    }
    public Integer getPercentage() {
        return percentage;
    }
    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }
    public Integer getMaxDiscountInCent() {
        return maxDiscountInCent;
    }
    public void setMaxDiscountInCent(Integer maxDiscountInCent) {
        this.maxDiscountInCent = maxDiscountInCent;
    }
    public Integer getDiscountInCent() {
        return discountInCent;
    }
    public void setDiscountInCent(Integer discountInCent) {
        this.discountInCent = discountInCent;
    }
    public Integer getMinOrderValueInCent() {
        return minOrderValueInCent;
    }
    public void setMinOrderValueInCent(Integer minOrderValueInCent) {
        this.minOrderValueInCent = minOrderValueInCent;
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
