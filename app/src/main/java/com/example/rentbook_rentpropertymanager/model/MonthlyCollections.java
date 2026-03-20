package com.example.rentbook_rentpropertymanager.model;

import com.google.firebase.database.Exclude;

public class MonthlyCollections {

    public int total_rent, total_elc_bill, total_units_used;
    @Exclude
    public String collection_month_year;

    public MonthlyCollections(){}

    public MonthlyCollections(int total_rent, int total_elc_bill, int total_units_used){
        this.total_rent = total_rent;
        this.total_elc_bill = total_elc_bill;
        this.total_units_used = total_units_used;
    }

    public int getTotal_rent() {
        return total_rent;
    }

    public void setTotal_rent(int total_rent) {
        this.total_rent = total_rent;
    }

    public int getTotal_elc_bill() {
        return total_elc_bill;
    }

    public void setTotal_elc_bill(int total_elc_bill) {
        this.total_elc_bill = total_elc_bill;
    }

    public int getTotal_units_used() {
        return total_units_used;
    }

    public void setTotal_units_used(int total_units_used) {
        this.total_units_used = total_units_used;
    }

    public String getCollection_month_year() {
        return collection_month_year;
    }

    public void setCollection_month_year(String collection_month_year) {
        this.collection_month_year = collection_month_year;
    }
}
