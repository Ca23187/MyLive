package com.mylive.infra.jpa.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class TokenInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -6910208948981307451L;
    private Long userId;
    private String nickname;
    private String avatar;
}
