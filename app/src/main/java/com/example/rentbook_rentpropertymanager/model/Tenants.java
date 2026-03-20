package com.example.rentbook_rentpropertymanager.model;

public class Tenants {

    public String tenant_name, thumb_tenant_url, tenant_phone, tenant_start_date, tenant_end_date;

    public Tenants(){}

    public Tenants(String tenant_name, String thumb_tenant_url, String tenant_phone, String tenant_start_date, String tenant_end_date) {
        this.tenant_name = tenant_name;
        this.thumb_tenant_url = thumb_tenant_url;
        this.tenant_phone = tenant_phone;
        this.tenant_start_date = tenant_start_date;
        this.tenant_end_date = tenant_end_date;
    }

    public String getTenant_name() {
        return tenant_name;
    }

    public void setTenant_name(String tenant_name) {
        this.tenant_name = tenant_name;
    }

    public String getThumb_tenant_url() {
        return thumb_tenant_url;
    }

    public void setThumb_tenant_url(String thumb_tenant_url) {
        this.thumb_tenant_url = thumb_tenant_url;
    }

    public String getTenant_phone() {
        return tenant_phone;
    }

    public void setTenant_phone(String tenant_phone) {
        this.tenant_phone = tenant_phone;
    }

    public String getTenant_start_date() {
        return tenant_start_date;
    }

    public void setTenant_start_date(String tenant_start_date) {
        this.tenant_start_date = tenant_start_date;
    }

    public String getTenant_end_date() {
        return tenant_end_date;
    }

    public void setTenant_end_date(String tenant_end_date) {
        this.tenant_end_date = tenant_end_date;
    }
}
