package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;


/**
 * 视频文件信息
 */
@Setter
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class VideoInfoFile implements Serializable {

	/**
	 * 唯一ID
	 */
	@Id
	private String fileId;

	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 视频ID
	 */
	private String videoId;

	/**
	 * 文件名
	 */
	private String title;

	/**
	 * 文件索引
	 */
	private Integer fileIndex;

	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 文件路径
	 */
	private String filePath;

	/**
	 * 持续时间（秒）
	 */
	private Integer duration;
}
