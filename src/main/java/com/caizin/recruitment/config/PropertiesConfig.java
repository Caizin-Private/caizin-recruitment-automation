package com.caizin.recruitment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AtsProperties.class,
        KekaProperties.class,
        OpenAiProperties.class
})
public class PropertiesConfig {
}

