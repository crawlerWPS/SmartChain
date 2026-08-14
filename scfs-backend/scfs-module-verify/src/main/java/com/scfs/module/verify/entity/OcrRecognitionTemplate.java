package com.scfs.module.verify.entity;

import com.scfs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class OcrRecognitionTemplate extends BaseEntity {
    private String templateCode;
    private String templateName;
    private String materialType;
    private Long enterpriseId;
    private Integer priority;
    private Boolean enabled;
    private List<String> matchAnchors;
    private List<Map<String, Object>> fieldRules;
}
