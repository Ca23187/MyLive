package com.mylive.infra.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "es")
@Component
@Getter
@Setter
public class ElasticSearchProperties {
    private String indexName;
}
