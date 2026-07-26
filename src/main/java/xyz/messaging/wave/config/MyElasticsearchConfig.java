package xyz.messaging.wave.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class MyElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
            new HttpHost("elasticsearch", 9200, "http")
        ).build();

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper(mapper));

        return new ElasticsearchClient(transport);
    }
}
