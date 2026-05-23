package com.mylive.infra.jpa.entity.po;

import com.mylive.infra.jpa.entity.po.id.StatisticInfoId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


/**
 * 数据统计
 */
@Entity
@Setter
@Getter
@IdClass(StatisticInfoId.class)
public class StatisticInfo implements Serializable {

	/**
	 * 统计日期
	 */
	@Id
	private String statisticDate;

	/**
	 * 用户ID
	 */
	@Id
	private Long userId;

	/**
	 * 数据统计类型
	 */
	@Id
	private Integer dataType;

	/**
	 * 统计数量
	 */
	private Integer statisticCount;

}
