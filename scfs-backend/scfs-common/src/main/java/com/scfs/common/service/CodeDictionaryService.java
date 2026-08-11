package com.scfs.common.service;

import com.scfs.common.entity.CodeDictionary;
import com.scfs.common.mapper.CodeDictionaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeDictionaryService {

    private final CodeDictionaryMapper mapper;

    public List<CodeDictionary> list(String codeType) {
        String normalized = codeType == null || codeType.isBlank()
                ? null
                : codeType.trim().toUpperCase();
        return mapper.selectEnabled(normalized);
    }

    public CodeDictionary getByKey(String codeKey) {
        return mapper.selectByKey(codeKey);
    }

    public List<String> listTypes() {
        return mapper.selectTypes();
    }
}
