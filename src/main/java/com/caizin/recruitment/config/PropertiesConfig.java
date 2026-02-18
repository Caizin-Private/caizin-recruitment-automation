package com.caizin.recruitment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AtsProperties.class,
        OpenAiProperties.class,
        ZohoProperties.class
})
public class PropertiesConfig {
}

