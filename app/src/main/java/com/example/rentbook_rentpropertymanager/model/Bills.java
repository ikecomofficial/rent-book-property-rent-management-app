package com.example.rentbook_rentpropertymanager.model;

public class Bills {
    public String elc_bill_date, elc_bill_time, payment_mode, elc_bill_timestamp, elc_bill_month_year;
    public int paid_up_to, units_used, elc_bill_amount;

    public Bills(){

    }

    public Bills(String elc_bill_timestamp, String elc_bill_date, String payment_mode, String elc_bill_time,
                 int paid_up_to, int units_used, int elc_bill_amount, String elc_bill_month_year) {
        this.elc_bill_date = elc_bill_date;
        this.elc_bill_time = elc_bill_time;
        this.paid_up_to = paid_up_to;
        this.units_used = units_used;
        this.elc_bill_amount = elc_bill_amount;
        this.payment_mode = payment_mode;
        this.elc_bill_timestamp = elc_bill_timestamp;
        this.elc_bill_month_year = elc_bill_month_year;
    }

    public String getElc_bill_timestamp() {
        return elc_bill_timestamp;
    }

    public void setElc_bill_timestamp(String elc_bill_timestamp) {
        this.elc_bill_timestamp = elc_bill_timestamp;
    }

    public String getElc_bill_date() {
        return elc_bill_date;
    }

    public void setElc_bill_date(String elc_bill_date) {
        this.elc_bill_date = elc_bill_date;
    }

    public String getElc_bill_time() {
        return elc_bill_time;
    }

    public void setElc_bill_time(String elc_bill_time) {
        this.elc_bill_time = elc_bill_time;
    }

    public int getPaid_up_to() {
        return paid_up_to;
    }

    public void setPaid_up_to(int paid_up_to) {
        this.paid_up_to = paid_up_to;
    }

    public int getUnits_used() {
        return units_used;
    }

    public void setUnits_used(int units_used) {
        this.units_used = units_used;
    }

    public int getElc_bill_amount() {
        return elc_bill_amount;
    }

    public void setElc_bill_amount(int elc_bill_amount) {
        this.elc_bill_amount = elc_bill_amount;
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
