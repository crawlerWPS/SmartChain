package com.scfs.module.verify.entity;

import lombok.Data;

/** 融资申请可选客户。 */
@Data
public class ApplicationCustomer {
    /** 企业主键直接作为客户号。 */
    private Long enterpriseId;
    private String name;
    private String uscc;
    private String industry;
    private String legalPerson;
    private String address;
}
