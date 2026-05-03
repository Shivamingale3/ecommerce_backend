package com.shivamingale.ecom.enums;

public enum MediaType {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    DOCUMENT("document");

    private String key;

    MediaType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
