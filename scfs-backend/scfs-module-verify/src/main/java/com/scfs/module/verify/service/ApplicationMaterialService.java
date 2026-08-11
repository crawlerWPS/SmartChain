package com.scfs.module.verify.service;

import com.scfs.common.entity.FileObject;
import com.scfs.common.service.FileStorageService;
import com.scfs.common.security.SecurityContextHelper;
import com.scfs.module.verify.entity.ApplicationMaterial;
import com.scfs.module.verify.entity.MaterialRecognitionResult;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申请材料管理服务 - 对应 RFC 4.2.2 ApplicationMaterialService
 *
 * <p>关键能力：</p>
 * <ul>
 *   <li>上传材料 → MinIO + file_object</li>
 *   <li>触发 OCR 异步识别（Mock 实现）</li>
 *   <li>人工修订识别结果</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationMaterialService {

    private final VerifyMapper verifyMapper;
    private final FileStorageService fileStorageService;
    private final OcrRecognitionService ocrRecognitionService;
    private final SecurityContextHelper securityContextHelper;

    public List<ApplicationMaterial> listByApplication(Long applicationId) {
        return verifyMapper.selectMaterialsByApplication(applicationId);
    }

    public ApplicationMaterial getById(Long id) {
        return verifyMapper.selectMaterialById(id);
    }

    public MaterialRecognitionResult getRecognitionResult(Long applicationMaterialId) {
        ApplicationMaterial material = requireMaterial(applicationMaterialId);
        return verifyMapper.selectRecognitionResult(material.getId());
    }

    /**
     * 上传材料文件
     */
    @Transactional
    public Long uploadMaterial(Long applicationId, MultipartFile file, String materialType) {
        if (verifyMapper.selectApplicationById(applicationId) == null) {
            throw new IllegalArgumentException("融资申请不存在");
        }
        if (materialType == null || materialType.isBlank()) {
            throw new IllegalArgumentException("请选择材料类型");
        }
        Long fileObjectId = fileStorageService.upload(file, securityContextHelper.getCurrentUserIdOrThrow());
        FileObject fileObject = fileStorageService.getFileInfo(fileObjectId);

        ApplicationMaterial material = new ApplicationMaterial();
        material.setApplicationId(applicationId);
        material.setFileObjectId(fileObjectId);
        material.setMaterialType(materialType);
        material.setIdentifiedBy("AUTO");
        material.setConfidence(BigDecimal.ZERO);
        material.setStatus("PENDING_MANUAL");
        verifyMapper.insertMaterial(material);

        // 异步触发 OCR 识别
        try {
            ocrRecognitionService.recognizeAsync(material.getId(), fileObject);
        } catch (Exception e) {
            log.warn("[Material] OCR 异步识别启动失败: materialId={}, error={}", material.getId(), e.getMessage());
        }
        return material.getId();
    }

    @Transactional
    public void reRecognize(Long id) {
        ApplicationMaterial material = requireMaterial(id);
        FileObject fileObject = fileStorageService.getFileInfo(material.getFileObjectId());
        verifyMapper.deleteRecognitionResult(id);
        verifyMapper.updateMaterialType(id, material.getMaterialType(), "AUTO");
        ocrRecognitionService.recognizeAsync(id, fileObject);
    }

    private ApplicationMaterial requireMaterial(Long id) {
        ApplicationMaterial material = verifyMapper.selectMaterialById(id);
        if (material == null) {
            throw new IllegalArgumentException("材料不存在");
        }
        return material;
    }

    /**
     * 人工修订材料类型
     */
    @Transactional
    public void updateMaterialType(Long id, String materialType) {
        ApplicationMaterial material = verifyMapper.selectMaterialById(id);
        if (material == null) {
            throw new IllegalArgumentException("材料不存在");
        }
        verifyMapper.updateMaterialType(id, materialType, "MANUAL");
    }

    /**
     * 人工修订 OCR 识别结果
     */
    @Transactional
    public void updateRecognitionResult(Long applicationMaterialId, MaterialRecognitionResult result) {
        MaterialRecognitionResult existing = verifyMapper.selectRecognitionResult(applicationMaterialId);
        if (existing == null) {
            result.setApplicationMaterialId(applicationMaterialId);
            verifyMapper.insertRecognitionResult(result);
        } else {
            result.setApplicationMaterialId(applicationMaterialId);
            result.setId(existing.getId());
            verifyMapper.updateRecognitionResult(result);
        }
        // 更新材料状态为已识别
        ApplicationMaterial material = verifyMapper.selectMaterialById(applicationMaterialId);
        if (material != null) {
            verifyMapper.updateMaterialType(applicationMaterialId, material.getMaterialType(), "MANUAL");
        }
    }
}
