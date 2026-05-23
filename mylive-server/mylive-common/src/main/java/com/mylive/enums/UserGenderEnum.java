package com.mylive.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserGenderEnum {

    FEMALE(0, "female"),
    MALE(1, "male"),
    SECRECY(2, "secrecy");;

    private final Integer type;
    private final String desc;
}
