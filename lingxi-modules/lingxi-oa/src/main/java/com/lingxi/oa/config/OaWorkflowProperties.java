package com.lingxi.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "lingxi.oa.workflow")
public class OaWorkflowProperties {

    /**
     * Keep workflow integration switchable so the OA module can run even when
     * the process engine is intentionally disabled in some environments.
     */
    private boolean enabled = true;
}
