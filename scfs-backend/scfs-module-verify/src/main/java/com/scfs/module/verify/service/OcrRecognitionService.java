package com.scfs.module.verify.service;

import com.scfs.common.entity.FileObject;
import com.scfs.module.verify.entity.MaterialRecognitionResult;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OCR 识别服务（Mock 实现） - 对应 RFC 4.2.2 OcrRecognitionService
 *
 * <p>实际生产环境对接外部 OCR 服务，此处 Mock 实现：</p>
 * <ul>
 *   <li>根据 file_object.file_type 生成模拟数据</li>
 *   <li>随机置信度 75-95</li>
 *   <li>异步执行（@Async）</li>
 * </ul>
 *
 * <p>对应 RFC：Mock 数据源适配器（V1 不对接真实 CIF）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrRecognitionService {

    private final VerifyMapper verifyMapper;

    @Async
    public void recognizeAsync(Long applicationMaterialId, FileObject fileObject) {
        try {
            log.info("[OCR] 开始识别: materialId={}, fileName={}", applicationMaterialId, fileObject.getFileName());
            Thread.sleep(500);  // 模拟 OCR 耗时

            MaterialRecognitionResult result = mockRecognize(fileObject);
            result.setApplicationMaterialId(applicationMaterialId);
            result.setRecognizedAt(Instant.now());
            verifyMapper.insertRecognitionResult(result);

            log.info("[OCR] 识别完成: materialId={}, confidence={}",
                    applicationMaterialId, result.getFieldConfidence());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[OCR] 识别中断: materialId={}", applicationMaterialId);
        } catch (Exception e) {
            log.error("[OCR] 识别失败: materialId={}, error={}", applicationMaterialId, e.getMessage(), e);
        }
    }

    /**
     * Mock 识别：根据文件类型生成结构化数据
     */
    private MaterialRecognitionResult mockRecognize(FileObject fileObject) {
        MaterialRecognitionResult result = new MaterialRecognitionResult();
        String fileType = fileObject.getFileType() == null ? "" : fileObject.getFileType().toLowerCase();

        // 字段级置信度
        Map<String, Object> fieldConfidence = new HashMap<>();
        double baseConfidence = 75 + ThreadLocalRandom.current().nextDouble(20);
        fieldConfidence.put("buyer_name", baseConfidence);
        fieldConfidence.put("seller_name", baseConfidence);
        fieldConfidence.put("amount", baseConfidence + 5);
        result.setFieldConfidence(fieldConfidence);

        // Mock 主体数据
        result.setBuyerName("采购方有限公司");
        result.setBuyerUscc("91310000MOCKBUY001");
        result.setSellerName("供应商有限公司");
        result.setSellerUscc("91310000MOCKSELL01");
        result.setCommodity("电子产品");
        result.setAmount(BigDecimal.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000)));
        result.setAmountInWords("壹拾万元整");
        result.setTransactionNo("TXN-" + System.currentTimeMillis());

        // 原始 OCR 结果
        Map<String, Object> raw = new HashMap<>();
        raw.put("source", "MOCK_OCR");
        raw.put("file_type", fileType);
        raw.put("recognized_at", Instant.now().toString());
        result.setRawOcrResult(raw);

        return result;
    }
}
