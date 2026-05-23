package com.mylive.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {
    OK(200, "Request successful"),
    NOT_FOUND(404, "Request resource not found"),
    BAD_REQUEST(600, "Invalid request parameters"),
    ALREADY_EXISTS(601, "Information already exists"),
    EMAIL_ALREADY_EXISTS(601, "Email already exists"),
    NICKNAME_ALREADY_EXISTS(601, "Nickname already exists"),
    INTERNAL_ERROR(500, "Server error, please contact the administrator"),

    NOT_LOGGED_IN(901, "Not logged in, please log in first");

    private final Integer code;
    private final String msg;
}