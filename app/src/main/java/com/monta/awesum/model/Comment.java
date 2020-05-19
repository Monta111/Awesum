package com.monta.awesum.model;

public class Comment {

    private String comment;
    private long id;
    private String publisherId;

    public Comment() {
    }

    public Comment(String comment, long id, String publisherId) {
        this.comment = comment;
        this.id = id;
        this.publisherId = publisherId;
    }

    public String getComment() {
        return comment;
    }

    public long getId() {
        return id;
    }

    public String getPublisherId() {
        return publisherId;
    }
}
