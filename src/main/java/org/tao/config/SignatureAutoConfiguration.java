package org.tao.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tao.aspect.SignatureAspect;


/**
 * @author: handsometaoa
 * @description
 * @date: 2025/3/29 11:00
 */


@Configuration
@EnableConfigurationProperties(SignatureProperties.class)
public class SignatureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SignatureAspect signatureAspect() {
        return new SignatureAspect();
    }


}

