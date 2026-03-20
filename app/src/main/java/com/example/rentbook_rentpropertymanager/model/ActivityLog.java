package com.example.rentbook_rentpropertymanager.model;

public class ActivityLog {

    private String log_title, log_desc, log_entity, log_type;
    private long log_ts;

    public ActivityLog(){

    }

    public ActivityLog(String log_title, String log_desc, String log_entity, String log_type, long log_ts) {
        this.log_title = log_title;
        this.log_desc = log_desc;
        this.log_entity = log_entity;
        this.log_type = log_type;
        this.log_ts = log_ts;
    }

    public String getLog_title() {
        return log_title;
    }

    public void setLog_title(String log_title) {
        this.log_title = log_title;
    }

    public String getLog_desc() {
        return log_desc;
    }

    public void setLog_desc(String log_desc) {
        this.log_desc = log_desc;
    }

    public String getLog_entity() {
        return log_entity;
    }

    public void setLog_entity(String log_entity) {
        this.log_entity = log_entity;
    }

    public String getLog_type() {
        return log_type;
    }

    public void setLog_type(String log_type) {
        this.log_type = log_type;
    }

    public long getLog_ts() {
        return log_ts;
    }

    public void setLog_ts(long log_ts) {
        this.log_ts = log_ts;
    }
}
