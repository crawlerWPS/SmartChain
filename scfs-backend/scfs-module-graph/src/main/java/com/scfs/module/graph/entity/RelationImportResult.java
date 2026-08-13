package com.scfs.module.graph.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RelationImportResult {
    private int total;
    private int createdEnterprises;
    private int createdRelations;
    private int updatedRelations;
    private int skippedDuplicates;
    private final List<String> errors = new ArrayList<>();
}
