package com.scfs.common.controller;

import com.scfs.common.core.Result;
import com.scfs.common.entity.CodeDictionary;
import com.scfs.common.service.CodeDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/code-dictionaries")
@RequiredArgsConstructor
public class CodeDictionaryController {

    private final CodeDictionaryService service;

    /** 按类型查询启用码值；不传 type 时返回全部启用码值。 */
    @GetMapping
    public Result<List<CodeDictionary>> list(@RequestParam(required = false) String type) {
        return Result.success(service.list(type));
    }

    @GetMapping("/types")
    public Result<List<String>> types() {
        return Result.success(service.listTypes());
    }
}
