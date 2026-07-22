package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 文件对象实体 - 对应 RFC 表5 file_object（schema_common）
 *
 * <p>MinIO 存储映射 + SHA-256 查重</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileObject extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 原始文件名 */
    private String fileName;
    /** 文件后缀：pdf/jpg/png/docx/xlsx */
    private String fileType;
    /** 字节 */
    private Long fileSize;
    /** MinIO bucket */
    private String minioBucket;
    /** MinIO 对象 key */
    private String minioObjectKey;
    /** SHA-256 内容哈希（用于查重） */
    private String contentHash;
    /** 上传用户 */
    private Long uploadedBy;
}
