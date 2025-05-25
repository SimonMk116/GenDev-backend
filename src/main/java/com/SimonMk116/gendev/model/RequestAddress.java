package com.SimonMk116.gendev.model;


import java.util.Objects;

public class RequestAddress {
    private String strasse;
    private String hausnummer;
    private String postleitzahl;
    private String stadt;
    private String land;


    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getHausnummer() {
        return hausnummer;
    }

    public void setHausnummer(String hausnummer) {
        this.hausnummer = hausnummer;
    }

    public String getPostleitzahl() {
        return postleitzahl;
    }

    public void setPostleitzahl(String postleitzahl) {
        this.postleitzahl = postleitzahl;
    }

    public String getStadt() {
        return stadt;
    }

    public void setStadt(String stadt) {
        this.stadt = stadt;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestAddress that)) return false;
        return Objects.equals(strasse, that.strasse) &&
               Objects.equals(hausnummer, that.hausnummer) &&
               Objects.equals(postleitzahl, that.postleitzahl) &&
               Objects.equals(stadt, that.stadt) &&
               Objects.equals(land, that.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strasse, hausnummer, postleitzahl, stadt, land);
    }
}
