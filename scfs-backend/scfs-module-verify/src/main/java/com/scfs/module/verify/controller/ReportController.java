package com.scfs.module.verify.controller;

import com.scfs.common.security.RequirePermission;
import com.scfs.module.verify.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 核验报告导出接口。 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final VerifyService verifyService;

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/{reportNo}/export-pdf")
    public ResponseEntity<byte[]> exportReportPdf(@PathVariable String reportNo) {
        byte[] pdf = verifyService.exportReportPdf(reportNo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"verify_report_" + reportNo + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
