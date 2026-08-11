package com.scfs.common.mapper;

import com.scfs.common.entity.CodeDictionary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeDictionaryMapper {

    List<CodeDictionary> selectEnabled(@Param("codeType") String codeType);

    CodeDictionary selectByKey(@Param("codeKey") String codeKey);

    List<String> selectTypes();
}
