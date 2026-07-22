package com.scfs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SCFS 供应链金融智能风控与尽调辅助平台 - 启动类
 *
 * <p>聚合 6 个 Maven 子模块：scfs-common / scfs-module-graph / scfs-module-verify
 * / scfs-module-preaudit / scfs-module-risk / scfs-app</p>
 *
 * <p>对应 RFC 6.2 阶段 0 S0-3：创建 scfs-app 启动模块</p>
 */
@SpringBootApplication(scanBasePackages = "com.scfs")
@MapperScan(basePackages = "com.scfs", annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableAsync
@EnableScheduling
public class ScfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScfsApplication.class, args);
    }
}
