package com.example.myweatherapplication;

import java.io.Serializable;

public class SearchedCity implements Serializable {
    private String id;
    private String name;
    private String adm1; // 一级行政区 (省)
    private String adm2; // 二级行政区 (市)
    private String country;

    public SearchedCity(String id, String name, String adm1, String adm2, String country) {
        this.id = id;
        this.name = name;
        this.adm1 = adm1;
        this.adm2 = adm2;
        this.country = country;
    }

    public String getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public String getAdm1() { return adm1 == null ? "" : adm1; }
    public String getAdm2() { return adm2 == null ? "" : adm2; }
    public String getCountry() { return country == null ? "" : country; }

    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        String adminDistrict2 = getAdm2();
        String adminDistrict1 = getAdm1();
        boolean hasAdm2 = !adminDistrict2.isEmpty() && !adminDistrict2.equals(getName());
        boolean hasAdm1 = !adminDistrict1.isEmpty() && !adminDistrict1.equals(getName()) && !adminDistrict1.equals(adminDistrict2);

        if (hasAdm2 || hasAdm1) {
            sb.append(" (");
            boolean appended = false;
            if (hasAdm2) {
                sb.append(adminDistrict2);
                appended = true;
            }
            if (hasAdm1) {
                if (appended) sb.append(", ");
                sb.append(adminDistrict1);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedLocation();
    }
}