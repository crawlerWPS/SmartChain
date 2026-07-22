package com.scfs.common.mapper;

import com.scfs.common.entity.FileObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件对象 Mapper - 对应 RFC 表5 file_object
 */
@Mapper
public interface FileObjectMapper {

    FileObject selectById(@Param("id") Long id);

    FileObject selectByContentHash(@Param("contentHash") String contentHash);

    List<FileObject> selectByApplicationId(@Param("applicationId") Long applicationId);

    int insert(FileObject fileObject);
}
