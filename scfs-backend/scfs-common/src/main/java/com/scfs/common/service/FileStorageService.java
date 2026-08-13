package com.scfs.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.core.BusinessException;
import com.scfs.common.core.ErrorCode;
import com.scfs.common.entity.FileObject;
import com.scfs.common.mapper.FileObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * MinIO 文件存储服务 - 对应 RFC 4.1.3 FileStorageService
 *
 * <p>关键策略：</p>
 * <ul>
 *   <li>上传后计算 SHA-256 查重（同内容只存一次）</li>
 *   <li>file_object 表映射 MinIO bucket + objectKey</li>
 *   <li>不返回文件内容，仅返回 presigned URL 或原始流</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileObjectMapper fileObjectMapper;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${scfs.minio.bucket-materials:scfs-materials}")
    private String defaultBucket;

    /**
     * 上传文件到 MinIO
     *
     * @param file Spring 上传文件
     * @param uploaderId 上传用户 ID
     * @return 文件对象 ID（已存在则返回旧 ID）
     */
    public Long upload(MultipartFile file, Long uploaderId) {
        try {
            byte[] bytes = file.getBytes();
            validateFileContent(file.getOriginalFilename(), bytes);
            String contentHash = sha256(bytes);

            // 查重
            FileObject existing = fileObjectMapper.selectByContentHash(contentHash);
            if (existing != null) {
                log.info("[MinIO] 文件已存在（contentHash={}），复用 fileObjectId={}", contentHash, existing.getId());
                return existing.getId();
            }

            // 上传到 MinIO
            ensureBucketExists();
            String objectKey = Instant.now().toEpochMilli() + "_" + file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 持久化元数据
            FileObject fileObject = new FileObject();
            fileObject.setFileName(file.getOriginalFilename());
            fileObject.setFileType(getFileExtension(file.getOriginalFilename()));
            fileObject.setFileSize(file.getSize());
            fileObject.setMinioBucket(defaultBucket);
            fileObject.setMinioObjectKey(objectKey);
            fileObject.setContentHash(contentHash);
            fileObject.setUploadedBy(uploaderId);
            fileObject.setCreatedAt(Instant.now());

            fileObjectMapper.insert(fileObject);
            log.info("[MinIO] 上传成功：fileObjectId={}, objectKey={}", fileObject.getId(), objectKey);
            return fileObject.getId();
        } catch (Exception e) {
            log.error("[MinIO] 上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件流
     */
    public InputStream download(Long fileObjectId) {
        try {
            FileObject fileObject = fileObjectMapper.selectById(fileObjectId);
            if (fileObject == null) {
                throw new IllegalArgumentException("文件不存在: id=" + fileObjectId);
            }
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(fileObject.getMinioBucket())
                    .object(fileObject.getMinioObjectKey())
                    .build());
        } catch (ErrorResponseException e) {
            if ("NoSuchBucket".equals(e.errorResponse().code()) || "NoSuchKey".equals(e.errorResponse().code())
                    || "NoSuchObject".equals(e.errorResponse().code())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "文件内容不存在或已被清理", e);
            }
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    public FileObject getFileInfo(Long fileObjectId) {
        return fileObjectMapper.selectById(fileObjectId);
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
            log.info("[MinIO] 创建 bucket: {}", defaultBucket);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private static void validateFileContent(String fileName, byte[] bytes) {
        if ("pdf".equals(getFileExtension(fileName))) {
            byte[] signature = "%PDF-".getBytes(StandardCharsets.US_ASCII);
            if (bytes.length < signature.length) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_REJECTED, "PDF 文件内容无效");
            }
            for (int i = 0; i < signature.length; i++) {
                if (bytes[i] != signature[i]) {
                    throw new BusinessException(ErrorCode.FILE_UPLOAD_REJECTED, "PDF 文件格式与扩展名不匹配");
                }
            }
        }
    }
}
