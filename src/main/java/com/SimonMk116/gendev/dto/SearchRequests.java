package com.SimonMk116.gendev.dto;

public class SearchRequests {
    private String street;
    private String houseNumber;
    private String city;
    private String plz;
    private boolean wantsFibre;

    //Constructors
    public SearchRequests() {}


    public SearchRequests(String street, String houseNumber, String city, String plz) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.plz = plz;
    }

    public SearchRequests(String street, String houseNumber, String city, String plz, Boolean wantsFibre) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.plz = plz;
        this.wantsFibre = wantsFibre;
    }

    //Getters and setters
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }
    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getPlz() {
        return plz;
    }
    public void setPlz(String plz) {
        this.plz = plz;
    }

    public boolean isWantsFibre() {
        return wantsFibre;
    }

    public void setWantsFibre(boolean wantsFibre) {
        this.wantsFibre = wantsFibre;
    }
}
