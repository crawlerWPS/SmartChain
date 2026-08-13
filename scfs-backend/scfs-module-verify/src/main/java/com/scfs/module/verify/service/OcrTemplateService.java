package com.scfs.module.verify.service;

import com.scfs.module.verify.entity.OcrRecognitionTemplate;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrTemplateService {
    private final VerifyMapper verifyMapper;

    public List<OcrRecognitionTemplate> list(String materialType) { return verifyMapper.selectOcrTemplates(materialType); }
    public OcrRecognitionTemplate get(Long id) {
        OcrRecognitionTemplate value = verifyMapper.selectOcrTemplateById(id);
        if (value == null) throw new IllegalArgumentException("OCR识别模板不存在");
        return value;
    }
    @Transactional public Long create(OcrRecognitionTemplate value) {
        normalize(value); verifyMapper.insertOcrTemplate(value); return value.getId();
    }
    @Transactional public void update(Long id, OcrRecognitionTemplate value) {
        get(id); value.setId(id); normalize(value);
        if (verifyMapper.updateOcrTemplate(value) == 0) throw new IllegalArgumentException("OCR识别模板不存在");
    }
    @Transactional public void delete(Long id) {
        if (verifyMapper.deleteOcrTemplate(id) == 0) throw new IllegalArgumentException("OCR识别模板不存在");
    }
    private void normalize(OcrRecognitionTemplate value) {
        if (value.getTemplateName() == null || value.getTemplateName().isBlank()) throw new IllegalArgumentException("模板名称不能为空");
        if (!List.of("CONTRACT", "INVOICE").contains(value.getMaterialType())) throw new IllegalArgumentException("当前仅支持合同和发票模板");
        if (value.getPriority() == null) value.setPriority(0);
        if (value.getEnabled() == null) value.setEnabled(true);
        if (value.getMatchAnchors() == null) value.setMatchAnchors(List.of());
        if (value.getFieldRules() == null) value.setFieldRules(List.of());
    }
}
