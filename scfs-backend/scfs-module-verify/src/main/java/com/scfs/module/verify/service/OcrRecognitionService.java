package com.scfs.module.verify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.entity.FileObject;
import com.scfs.common.service.FileStorageService;
import com.scfs.module.verify.entity.MaterialRecognitionResult;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Comparator;

/** PaddleOCR HTTP adapter. It never falls back to fabricated recognition data. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrRecognitionService {

    private static final Pattern USCC = Pattern.compile("[0-9A-HJ-NPQRTUWXY]{18}");
    private static final Pattern AMOUNT = Pattern.compile("(?:价税合计|合同金额|总金额|金额)[：:\\s￥¥]*([0-9,]+(?:\\.[0-9]{1,2})?)");
    private static final Pattern TRANSACTION_NO = Pattern.compile("(?:合同编号|订单编号|发票号码|单据编号)[：:\\s]*([A-Za-z0-9_-]{4,})");
    private static final Pattern INVOICE_DATE = Pattern.compile("(?:开票日期|开具日期|发票日期)[：:\\s]*(\\d{4}[年./-]\\d{1,2}[月./-]\\d{1,2}日?)");

    private final VerifyMapper verifyMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Value("${scfs.ocr.endpoint:http://localhost:9003}")
    private String endpoint;

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeTemplateSample(MultipartFile file) {
        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return file.getOriginalFilename(); }
            });
            form.add("includePages", "true");
            Map<String, Object> response = RestClient.create(endpoint).post().uri("/ocr/recognize")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new IllegalArgumentException("标准样本未识别到有效文字");
            }
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("标准样本分析失败：" + e.getMessage(), e);
        }
    }

    public Map<String, Object> testTemplateRules(List<Map<String, Object>> rules, Map<String, Object> sample) {
        return extractByTemplate(rules == null ? List.of() : rules, sample);
    }

    @Async
    public void recognizeAsync(Long materialId, FileObject fileObject) {
        try (var stream = fileStorageService.download(fileObject.getId())) {
            byte[] bytes = stream.readAllBytes();
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", new ByteArrayResource(bytes) {
                @Override public String getFilename() { return fileObject.getFileName(); }
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> response = RestClient.create(endpoint).post()
                    .uri("/ocr/recognize")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new IllegalStateException("PaddleOCR 未识别到有效文字");
            }
            saveResult(materialId, response);
        } catch (Exception e) {
            verifyMapper.updateMaterialRecognitionStatus(materialId, "OCR_FAILED", BigDecimal.ZERO);
            log.error("[PaddleOCR] 识别失败: materialId={}, error={}", materialId, e.getMessage(), e);
        }
    }

    private void saveResult(Long materialId, Map<String, Object> response) {
        String text = String.valueOf(response.getOrDefault("text", ""));
        double confidence = response.get("confidence") instanceof Number n ? n.doubleValue() : 0D;
        MaterialRecognitionResult result = new MaterialRecognitionResult();
        result.setApplicationMaterialId(materialId);
        result.setRecognizedAt(Instant.now());
        result.setRawOcrResult(response);
        result.setFieldPositions(Map.of("items", response.getOrDefault("items", List.of())));

        var material = verifyMapper.selectMaterialById(materialId);
        if (material == null) {
            log.info("[PaddleOCR] 材料已删除，忽略识别结果: materialId={}", materialId);
            return;
        }
        var template = material.getOcrTemplateId() == null
                ? verifyMapper.selectOcrTemplates(material.getMaterialType()).stream()
                    .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                    .filter(t -> t.getMatchAnchors() == null || t.getMatchAnchors().isEmpty()
                            || t.getMatchAnchors().stream().allMatch(text::contains))
                    .findFirst().orElse(null)
                : verifyMapper.selectOcrTemplateById(material.getOcrTemplateId());
        Map<String, Object> extracted = template == null ? Map.of() : extractByTemplate(template.getFieldRules(), response);

        Matcher uscc = USCC.matcher(text.replace(" ", "").toUpperCase());
        if (uscc.find()) result.setBuyerUscc(uscc.group());
        if (uscc.find()) result.setSellerUscc(uscc.group());
        Matcher amount = AMOUNT.matcher(text);
        if (amount.find()) result.setAmount(new BigDecimal(amount.group(1).replace(",", "")));
        Matcher transactionNo = TRANSACTION_NO.matcher(text);
        if (transactionNo.find()) result.setTransactionNo(transactionNo.group(1));
        if ("INVOICE".equalsIgnoreCase(material.getMaterialType())) {
            Matcher invoiceDate = INVOICE_DATE.matcher(text);
            if (invoiceDate.find()) result.setInvoiceDate(parseDate(invoiceDate.group(1)));
        }
        applyExtracted(result, extracted);

        Map<String, Object> fieldConfidence = new HashMap<>();
        fieldConfidence.put("overall", confidence * 100D);
        if (template != null) {
            fieldConfidence.put("templateId", template.getId());
            fieldConfidence.put("templateCode", template.getTemplateCode());
        }
        result.setFieldConfidence(fieldConfidence);
        verifyMapper.insertRecognitionResult(result);
        verifyMapper.updateMaterialRecognitionStatus(materialId, "IDENTIFIED",
                BigDecimal.valueOf(confidence * 100D));
        log.info("[PaddleOCR] 识别完成: materialId={}, confidence={}", materialId, confidence);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractByTemplate(List<Map<String, Object>> rules, Map<String, Object> response) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getOrDefault("items", List.of());
        Map<String, Object> values = new HashMap<>();
        for (Map<String, Object> rule : rules) {
            String code = String.valueOf(rule.get("fieldCode"));
            String mode = String.valueOf(rule.get("extractMode"));
            String value = null;
            if ("FULL_TEXT".equals(mode) && rule.get("pattern") != null) {
                Matcher matcher = Pattern.compile(String.valueOf(rule.get("pattern"))).matcher(String.valueOf(response.getOrDefault("text", "")));
                if (matcher.find()) value = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
            } else if ("ABSOLUTE_REGION".equals(mode)) {
                value = textInRegion(items, number(rule.get("page"), 1), null, (Map<String,Object>) rule.get("region"));
            } else if ("ANCHOR_REGION".equals(mode)) {
                List<String> anchors = (List<String>) rule.getOrDefault("anchors", List.of());
                Map<String,Object> anchor = items.stream().filter(i -> anchors.stream().anyMatch(a -> String.valueOf(i.get("text")).contains(a))).findFirst().orElse(null);
                value = textInRegion(items, number(rule.get("page"), 1), anchor, (Map<String,Object>) rule.get("region"));
            }
            if (value != null && !value.isBlank()) values.put(code, value.trim());
        }
        return values;
    }

    private String textInRegion(List<Map<String,Object>> items, int page, Map<String,Object> anchor, Map<String,Object> region) {
        if (region == null) return null;
        List<Map<String,Object>> pageItems = items.stream().filter(i -> number(i.get("page"), 1) == page && i.get("box") instanceof List).toList();
        if (pageItems.isEmpty()) return null;
        double maxX = numberD(pageItems.get(0).get("imageWidth"),
                pageItems.stream().mapToDouble(i -> box(i,2)).max().orElse(1));
        double maxY = numberD(pageItems.get(0).get("imageHeight"),
                pageItems.stream().mapToDouble(i -> box(i,3)).max().orElse(1));
        double x = numberD(region.get("x"),0) * maxX, y = numberD(region.get("y"),0) * maxY;
        if (anchor != null) { x += box(anchor,2); y += box(anchor,1); }
        double w = numberD(region.get("width"),.3) * maxX, h = numberD(region.get("height"),.05) * maxY;
        final double left = x, top = y, right = x + w, bottom = y + h;
        List<Map<String,Object>> selected = new ArrayList<>(pageItems.stream().filter(i -> box(i,2)>=left && box(i,0)<=right && box(i,3)>=top && box(i,1)<=bottom).toList());
        selected.sort(Comparator.comparingDouble((Map<String,Object> i)->box(i,1)).thenComparingDouble(i->box(i,0)));
        return String.join(" ", selected.stream().filter(i -> i != anchor).map(i -> String.valueOf(i.get("text"))).toList());
    }

    private double box(Map<String,Object> item, int index) { return numberD(((List<?>)item.get("box")).get(index),0); }
    private int number(Object value, int fallback) { return value instanceof Number n ? n.intValue() : fallback; }
    private double numberD(Object value, double fallback) { return value instanceof Number n ? n.doubleValue() : fallback; }
    private void applyExtracted(MaterialRecognitionResult result, Map<String,Object> values) {
        if (values.containsKey("buyerName")) result.setBuyerName(String.valueOf(values.get("buyerName")));
        if (values.containsKey("sellerName")) result.setSellerName(String.valueOf(values.get("sellerName")));
        if (values.containsKey("buyerUscc")) result.setBuyerUscc(String.valueOf(values.get("buyerUscc")));
        if (values.containsKey("sellerUscc")) result.setSellerUscc(String.valueOf(values.get("sellerUscc")));
        if (values.containsKey("commodity")) result.setCommodity(String.valueOf(values.get("commodity")));
        if (values.containsKey("amountInWords")) result.setAmountInWords(String.valueOf(values.get("amountInWords")));
        if (values.containsKey("transactionNo")) result.setTransactionNo(String.valueOf(values.get("transactionNo")));
        if (values.containsKey("contractPeriod")) result.setContractPeriod(String.valueOf(values.get("contractPeriod")));
        if (values.containsKey("paymentTerm")) result.setPaymentTerm(String.valueOf(values.get("paymentTerm")));
        if (values.containsKey("amount")) try { result.setAmount(new BigDecimal(String.valueOf(values.get("amount")).replace(",", ""))); } catch (NumberFormatException ignored) { }
        result.setContractDate(parseDate(values.get("contractDate")));
        result.setOrderDate(parseDate(values.get("orderDate")));
        LocalDate invoiceDate = parseDate(values.get("invoiceDate"));
        if (invoiceDate != null) result.setInvoiceDate(invoiceDate);
        result.setLogisticsDate(parseDate(values.get("logisticsDate")));
        result.setAcceptanceDate(parseDate(values.get("acceptanceDate")));
        result.setPaymentDate(parseDate(values.get("paymentDate")));
    }

    private LocalDate parseDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        String text = String.valueOf(value).trim().replace('年', '-').replace('月', '-').replace("日", "").replace('/', '-').replace('.', '-');
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("yyyyMMdd"))) {
            try { return LocalDate.parse(text, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        return null;
    }
}
