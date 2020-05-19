package com.monta.awesum.model;

public class Post {
    public static final int IMAGE_TYPE_ITEM = 1;
    public static final int VIDEO_TYPE_ITEM = 2;
    private String publisherId;
    private String description;
    private long postId;
    private int type;

    public Post() {
    }

    public Post(String publisherId, String description, long postId, int type) {
        this.publisherId = publisherId;
        this.description = description;
        this.postId = postId;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getDescription() {
        return description;
    }

    public long getPostId() {
        return postId;
    }

}
