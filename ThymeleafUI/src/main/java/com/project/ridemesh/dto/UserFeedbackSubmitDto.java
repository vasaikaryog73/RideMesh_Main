package com.project.ridemesh.dto;

public class UserFeedbackSubmitDto {
    private String type;
    private String subType;
    private String content;
    private String mediaUrl;

    public UserFeedbackSubmitDto() {
    }

    public UserFeedbackSubmitDto(String type, String subType, String content, String mediaUrl) {
        this.type = type;
        this.subType = subType;
        this.content = content;
        this.mediaUrl = mediaUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}
