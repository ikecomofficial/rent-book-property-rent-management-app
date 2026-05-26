package com.example.rentbook_rentpropertymanager.model;

public class Rents {

    public String payment_mode, tenant_name, rent_month_year, rent_period_start, rent_period_end, tenant_id;
    public long rent_timestamp;
    public int rent_amount;

    public Rents(){

    }

    public Rents(long rent_timestamp, String payment_mode, String tenant_name, int rent_amount, String rent_period_start,
                 String rent_period_end, String tenant_id) {
        this.payment_mode = payment_mode;
        this.tenant_name = tenant_name;
        this.rent_amount = rent_amount;
        this.rent_timestamp = rent_timestamp;
        this.rent_period_start = rent_period_start;
        this.rent_period_end = rent_period_end;
        this.tenant_id = tenant_id;
    }

    public long getRent_timestamp() {
        return rent_timestamp;
    }

    public String getPayment_mode() {
        return payment_mode;
    }

    public void setPayment_mode(String payment_mode) {
        this.payment_mode = payment_mode;
    }

    public String getTenant_name() {
        return tenant_name;
    }

    public void setTenant_name(String tenant_name) {
        this.tenant_name = tenant_name;
    }

    public int getRent_amount() {
        return rent_amount;
    }

    public void setRent_amount(int rent_amount) {
        this.rent_amount = rent_amount;
    }

    public String getRent_month_year() {
        return rent_month_year;
    }

    public void setRent_month_year(String rent_month_year) {
        this.rent_month_year = rent_month_year;
    }

    public String getRent_period_start() {
        return rent_period_start;
    }

    public void setRent_period_start(String rent_period_start) {
        this.rent_period_start = rent_period_start;
    }

    public String getRent_period_end() {
        return rent_period_end;
    }

    public void setRent_period_end(String rent_period_end) {
        this.rent_period_end = rent_period_end;
    }

    public String getTenant_id() {
        return tenant_id;
    }

    public void setTenant_id(String tenant_id) {
        this.tenant_id = tenant_id;
    }
}
