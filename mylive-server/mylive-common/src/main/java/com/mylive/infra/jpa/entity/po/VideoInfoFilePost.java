package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


/**
 * 视频文件信息
 */
@Setter
@Getter
@Entity
public class VideoInfoFilePost implements Serializable {

	/**
	 * 自增唯一ID
	 */
	@Id
	private String fileId;

	/**
	 * 上传ID
	 */
	private String uploadId;

	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 视频ID
	 */
	private String videoId;

	/**
	 * 文件索引
	 */
	private Integer fileIndex;

	/**
	 * 文件名
	 */
	private String title;

	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 文件路径
	 */
	private String filePath;

	/**
	 * 0:无更新 1:有更新
	 */
	private Integer updateType;

	/**
	 * 0:转码中 1:转码成功 2:转码失败
	 */
	private Integer transcodeResult;

	/**
	 * 持续时间（秒）
	 */
	private Integer duration;
}
