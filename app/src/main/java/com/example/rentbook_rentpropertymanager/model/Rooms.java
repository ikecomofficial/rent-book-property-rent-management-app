package com.example.rentbook_rentpropertymanager.model;

public class Rooms {

    public String room_id, room_name, tenant_name, tenant_phone, thumb_tenant_url, tenant_id, last_rent_month;
    public int room_rent, room_no, last_unit_paid;
    public Boolean is_occupied, is_room, is_rent_advance;

    public Rooms(){

    }

    public Rooms(String room_id, String room_name, int room_no, String tenant_id, String tenant_name,
                 String tenant_phone, String thumb_tenant_url, int last_unit_paid,
                 int room_rent, boolean is_occupied, boolean is_room, String last_rent_month, Boolean is_rent_advance) {
        this.room_id = room_id;
        this.room_name = room_name;
        this.tenant_name = tenant_name;
        this.tenant_phone = tenant_phone;
        this.thumb_tenant_url = thumb_tenant_url;
        this.room_rent = room_rent;
        this.tenant_id = tenant_id;
        this.is_occupied = is_occupied;
        this.is_room = is_room;
        this.room_no = room_no;
        this.last_unit_paid = last_unit_paid;
        this.last_rent_month = last_rent_month;
        this.is_rent_advance = is_rent_advance;
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public String getRoom_name() {
        return room_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    public int getRoom_no() {
        return room_no;
    }

    public void setRoom_no(int room_no) {
        this.room_no = room_no;
    }

    public String getTenant_id() {
        return tenant_id;
    }

    public void setTenant_id(String tenant_id) {
        this.tenant_id = tenant_id;
    }

    public String getTenant_name() {
        return tenant_name;
    }

    public void setTenant_name(String tenant_name) {
        this.tenant_name = tenant_name;
    }

    public String getTenant_phone() {
        return tenant_phone;
    }

    public void setTenant_phone(String tenant_phone) {
        this.tenant_phone = tenant_phone;
    }

    public String getThumb_tenant_url() {
        return thumb_tenant_url;
    }

    public void setThumb_tenant_url(String thumb_tenant_url) {
        this.thumb_tenant_url = thumb_tenant_url;
    }

    public int getRoom_rent() {
        return room_rent;
    }

    public void setRoom_rent(int room_rent) {
        this.room_rent = room_rent;
    }

    public boolean isIs_occupied() {
        return is_occupied;
    }

    public void setIs_occupied(boolean is_occupied) {
        this.is_occupied = is_occupied;
    }

    public int getLast_unit_paid() {
        return last_unit_paid;
    }

    public void setLast_unit_paid(int last_unit_paid) {
        this.last_unit_paid = last_unit_paid;
    }

    public boolean isIs_room() {
        return is_room;
    }

    public void setIs_room(boolean is_room) {
        this.is_room = is_room;
    }

    public String getLast_rent_month() {
        return last_rent_month;
    }

    public void setLast_rent_month(String last_rent_month) {
        this.last_rent_month = last_rent_month;
    }

    public Boolean isIs_rent_advance() {
        return is_rent_advance;
    }

    public void setIs_rent_advance(Boolean is_rent_advance) {
        this.is_rent_advance = is_rent_advance;
    }
}
