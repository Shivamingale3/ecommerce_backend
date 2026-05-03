package com.shivamingale.ecom.enums;

public enum MediaRole {
    CATEGORY("category"),
    PRODUCT_MAIN("product_main"),
    PRODUCT_COVER("product_cover"),
    PRODUCT_GALLERY("product_gallery");

    private String prefix;

    MediaRole(String prefix) {
        this.prefix = prefix;
    }

    public String getKey() {
        return prefix;
    }
}