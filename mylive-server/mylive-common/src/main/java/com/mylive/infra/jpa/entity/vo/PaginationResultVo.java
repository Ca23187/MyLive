package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaginationResultVo<T> implements Serializable {
    private Integer totalCount;
    private Integer pageSize;
    private Integer pageNo;
    private Integer pageTotal;
    private List<T> list;

    public static <T> PaginationResultVo<T> fromPage(Page<T> page) {
        PaginationResultVo<T> result = new PaginationResultVo<>();
        result.setTotalCount((int) page.getTotalElements());
        result.setPageSize(page.getSize());
        result.setPageNo(page.getNumber() + 1);
        result.setPageTotal(page.getTotalPages());
        result.setList(page.getContent());
        return result;
    }
}