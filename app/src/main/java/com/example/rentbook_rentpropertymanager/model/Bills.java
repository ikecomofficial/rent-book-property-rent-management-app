package com.example.rentbook_rentpropertymanager.model;

public class Bills {
    public String ebill_date, ebill_time, payment_mode, ebill_timestamp, elc_bill_month_year;
    public int paid_upto, units_used, ebill_amount;

    public Bills(){

    }

    public Bills(String ebill_timestamp, String ebill_date, String payment_mode, String ebill_time,
                 int paid_upto, int units_used, int ebill_amount, String elc_bill_month_year) {
        this.ebill_date = ebill_date;
        this.ebill_time = ebill_time;
        this.paid_upto = paid_upto;
        this.units_used = units_used;
        this.ebill_amount = ebill_amount;
        this.payment_mode = payment_mode;
        this.ebill_timestamp = ebill_timestamp;
        this.elc_bill_month_year = elc_bill_month_year;
    }

    public String getEbill_timestamp() {
        return ebill_timestamp;
    }

    public void setEbill_timestamp(String ebill_timestamp) {
        this.ebill_timestamp = ebill_timestamp;
    }

    public String getEbill_date() {
        return ebill_date;
    }

    public void setEbill_date(String ebill_date) {
        this.ebill_date = ebill_date;
    }

    public String getEbill_time() {
        return ebill_time;
    }

    public void setEbill_time(String ebill_time) {
        this.ebill_time = ebill_time;
    }

    public int getPaid_upto() {
        return paid_upto;
    }

    public void setPaid_upto(int paid_upto) {
        this.paid_upto = paid_upto;
    }

    public int getUnits_used() {
        return units_used;
    }

    public void setUnits_used(int units_used) {
        this.units_used = units_used;
    }

    public int getEbill_amount() {
        return ebill_amount;
    }

    public void setEbill_amount(int ebill_amount) {
        this.ebill_amount = ebill_amount;
    }

    public String getPayment_mode() {
        return payment_mode;
    }

    public void setPayment_mode(String payment_mode) {
        this.payment_mode = payment_mode;
    }

    public String getElc_bill_month_year() {
        return elc_bill_month_year;
    }

    public void setElc_bill_month_year(String elc_bill_month_year) {
        this.elc_bill_month_year = elc_bill_month_year;
    }
}
