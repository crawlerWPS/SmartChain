package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 统一码值字典。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeDictionary extends BaseEntity {

    private String codeType;
    private String codeKey;
    private String codeValue;
    private Integer sortOrder;
    private Short status;
    private String description;

    /** 业务表实际保存的原始码值。 */
    public String getCode() {
        String prefix = codeType + ".";
        return codeKey != null && codeKey.startsWith(prefix)
                ? codeKey.substring(prefix.length())
                : codeKey;
    }
}
