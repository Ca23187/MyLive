package com.mylive.infra.jpa.entity.po.id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayHistoryId {
    private Long userId;
    private String videoId;
}
