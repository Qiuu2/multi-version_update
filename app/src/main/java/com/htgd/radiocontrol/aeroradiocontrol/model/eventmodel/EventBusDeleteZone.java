package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;

public class EventBusDeleteZone {
    private String message;

    public EventBusDeleteZone(String str) {
        this.message = str;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
