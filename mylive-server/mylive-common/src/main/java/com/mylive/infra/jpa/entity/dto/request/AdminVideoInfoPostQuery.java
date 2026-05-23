package com.mylive.infra.jpa.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminVideoInfoPostQuery {
    private Integer pageNo;
    private Integer pageSize;
    private String videoTitleFuzzy;
    private Integer categoryId;
    private Integer parentCategoryId;
    private Integer recommendType;
}
