package com.mylive.infra.jpa.entity.po.id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVideoSeriesVideoId {
    private Long seriesId;
    private String videoId;
}
