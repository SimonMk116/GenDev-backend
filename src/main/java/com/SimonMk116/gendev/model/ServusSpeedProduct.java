package com.SimonMk116.gendev.model;


public class ServusSpeedProduct {
    private String providerName;
    private OfferProductInfo productInfo;
    private OfferPricingDetails pricingDetails;
    private int discount;


    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public OfferProductInfo getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(OfferProductInfo productInfo) {
        this.productInfo = productInfo;
    }

    public OfferPricingDetails getPricingDetails() {
        return pricingDetails;
    }

    public void setPricingDetails(OfferPricingDetails pricingDetails) {
        this.pricingDetails = pricingDetails;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return "ServusSpeedProduct{" +
                "providerName='" + providerName + '\'' +
                ", productInfo=" + productInfo +
                ", pricingDetails=" + pricingDetails +
                ", discount=" + discount +
                '}';
    }
}
