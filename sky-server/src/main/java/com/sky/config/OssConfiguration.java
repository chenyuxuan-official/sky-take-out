package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，注册阿里云OSS相关组件
 */
@Configuration
@Slf4j
public class OssConfiguration {

    /**
     * 使用配置文件中的OSS参数创建AliOssUtil对象，并交给Spring容器管理
     *
     * @param aliOssProperties OSS配置属性
     * @return AliOssUtil
     */
    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云文件上传工具类对象...");
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
