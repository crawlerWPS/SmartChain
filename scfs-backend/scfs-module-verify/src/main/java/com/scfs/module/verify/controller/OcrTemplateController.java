package com.scfs.module.verify.controller;

import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.verify.entity.OcrRecognitionTemplate;
import com.scfs.module.verify.service.OcrTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.scfs.module.verify.service.OcrRecognitionService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ocr-templates")
@RequiredArgsConstructor
public class OcrTemplateController {
    private final OcrTemplateService service;
    private final OcrRecognitionService recognitionService;
    @RequirePermission(module="RULE", permission="view")
    @GetMapping public Result<List<OcrRecognitionTemplate>> list(@RequestParam(required=false) String materialType) { return Result.success(service.list(materialType)); }
    @RequirePermission(module="RULE", permission="create")
    @PostMapping public Result<Long> create(@RequestBody OcrRecognitionTemplate value) { return Result.success(service.create(value)); }
    @RequirePermission(module="RULE", permission="update")
    @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @RequestBody OcrRecognitionTemplate value) { service.update(id,value); return Result.success(); }
    @RequirePermission(module="RULE", permission="delete")
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.success(); }
    @RequirePermission(module="RULE", permission="create")
    @PostMapping("/sample/analyze")
    public Result<Map<String, Object>> analyzeSample(@RequestParam("file") MultipartFile file) {
        return Result.success(recognitionService.analyzeTemplateSample(file));
    }
    @RequirePermission(module="RULE", permission="create")
    @PostMapping("/sample/test")
    public Result<Map<String, Object>> testSample(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked") List<Map<String, Object>> rules = (List<Map<String, Object>>) body.get("fieldRules");
        @SuppressWarnings("unchecked") Map<String, Object> sample = (Map<String, Object>) body.get("sample");
        return Result.success(recognitionService.testTemplateRules(rules, sample));
    }
}
