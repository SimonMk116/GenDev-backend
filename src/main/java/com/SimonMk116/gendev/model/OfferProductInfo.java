package com.SimonMk116.gendev.model;


public class OfferProductInfo {
    private int speed;
    private int contractDurationInMonths;
    private String connectionType;
    private String tv;
    private Integer limitFrom;
    private Integer maxAge;



    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
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

    public String getTv() {
        return tv;
    }

    public void setTv(String tv) {
        this.tv = tv;
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

    @Override
    public String toString() {
        return "OfferProductInfo{" +
                "speed=" + speed +
                ", contractDurationInMonths=" + contractDurationInMonths +
                ", connectionType='" + connectionType + '\'' +
                ", tv='" + tv + '\'' +
                ", limitFrom=" + limitFrom +
                ", maxAge=" + maxAge +
                '}';
    }
}
