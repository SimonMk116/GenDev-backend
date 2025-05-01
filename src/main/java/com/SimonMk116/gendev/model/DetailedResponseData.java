package com.SimonMk116.gendev.model;


public class DetailedResponseData {
    private ServusSpeedProduct servusSpeedProduct;

    public ServusSpeedProduct getServusSpeedProduct() {
        return servusSpeedProduct;
    }

    public void setServusSpeedProduct(ServusSpeedProduct servusSpeedProduct) {
        this.servusSpeedProduct = servusSpeedProduct;
    }

    @Override
    public String toString() {
        return "DetailedResponseData{" +
                "servusSpeedProduct=" + servusSpeedProduct +
                '}';
    }

}
