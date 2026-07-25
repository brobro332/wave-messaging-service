package xyz.messaging.wave.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import java.time.Duration;

@Configuration
public class MyElasticsearchConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("elasticsearch:9200")
                .withHeaders(() -> {
                    org.springframework.data.elasticsearch.support.HttpHeaders headers = new org.springframework.data.elasticsearch.support.HttpHeaders();
                    headers.add("Accept", "application/json");
                    return headers;
                })
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(3))
                .build();
    }
}
