package com.SimonMk116.gendev.model;


public class OfferPricingDetails    {
    private int monthlyCostInCent;
    private boolean installationService;

    public int getMonthlyCostInCent() {
        return monthlyCostInCent;
    }

    public void setMonthlyCostInCent(int monthlyCostInCent) {
        this.monthlyCostInCent = monthlyCostInCent;
    }

    public boolean isInstallationService() {
        return installationService;
    }

    public void setInstallationService(boolean installationService) {
        this.installationService = installationService;
    }

    @Override
    public String toString() {
        return "OfferPricingDetails{" +
                "monthlyCostInCent=" + monthlyCostInCent +
                ", installationService=" + installationService +
                '}';
    }
}
