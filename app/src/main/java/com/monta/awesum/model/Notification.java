package com.monta.awesum.model;

public class Notification {
    public static final int FOLLOW_TYPE = 1;
    public static final int LIKE_TYPE = 2;
    public static final int COMMENT_TYPE = 3;
    public static final String FOLLOW_TYPE_CONTENT = "Follow you!";
    public static final String LIKE_TYPE_CONTENT = "Like your post!";
    public static final String COMMENT_TYPE_CONTENT = "Commented on your post: ";

    private long notificationId;
    private String userId;
    private String content;
    private String contentId;
    private String contentCombine;
    private boolean seen;
    private boolean ignore;
    private int type;

    public Notification() {
    }

    public Notification(int type, long notificationId, String userId, String content, String contentId, boolean seen, boolean ignore, String contentCombine) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.content = content;
        this.contentId = contentId;
        this.seen = seen;
        this.type = type;
        this.contentCombine = contentCombine;
    }

    public long getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public String getContentId() {
        return contentId;
    }

    public String getContentCombine() {
        return contentCombine;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public int getType() {
        return type;
    }

}
