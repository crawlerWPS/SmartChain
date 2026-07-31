package com.scfs.common.controller;

import com.scfs.common.core.Result;
import com.scfs.common.entity.FileObject;
import com.scfs.common.security.SecurityContextHelper;
import com.scfs.common.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件 Controller - 对应 RFC 3.x /api/files
 *
 * <p>对应 RFC S2-4：MinIO 文件上传/下载 + SHA-256 查重</p>
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping("/upload")
    public Result<Long> upload(@RequestParam("file") MultipartFile file) {
        Long userId = securityContextHelper.getCurrentUserIdOrThrow();
        Long fileObjectId = fileStorageService.upload(file, userId);
        return Result.success(fileObjectId);
    }

    @GetMapping("/{id}/info")
    public Result<FileObject> getInfo(@PathVariable Long id) {
        return Result.success(fileStorageService.getFileInfo(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        InputStream stream = fileStorageService.download(id);
        FileObject info = fileStorageService.getFileInfo(id);
        String fileName = URLEncoder.encode(info.getFileName(), StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(info.getFileSize()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(stream));
    }
}
