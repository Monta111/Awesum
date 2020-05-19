package com.monta.awesum.model;

public class Story {
    public static final int TYPE_ITEM = 3;
    private String id;
    private String imageUrl;
    private String userId;

    public Story() {
    }

    public Story(String id, String imageUrl, String userId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUserId() {
        return userId;
    }
}
