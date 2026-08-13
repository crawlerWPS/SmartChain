package com.scfs.module.verify.controller;

import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.verify.entity.OcrRecognitionTemplate;
import com.scfs.module.verify.service.OcrTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ocr-templates")
@RequiredArgsConstructor
public class OcrTemplateController {
    private final OcrTemplateService service;
    @RequirePermission(module="RULE", permission="view")
    @GetMapping public Result<List<OcrRecognitionTemplate>> list(@RequestParam(required=false) String materialType) { return Result.success(service.list(materialType)); }
    @RequirePermission(module="RULE", permission="create")
    @PostMapping public Result<Long> create(@RequestBody OcrRecognitionTemplate value) { return Result.success(service.create(value)); }
    @RequirePermission(module="RULE", permission="update")
    @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @RequestBody OcrRecognitionTemplate value) { service.update(id,value); return Result.success(); }
    @RequirePermission(module="RULE", permission="delete")
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.success(); }
}
