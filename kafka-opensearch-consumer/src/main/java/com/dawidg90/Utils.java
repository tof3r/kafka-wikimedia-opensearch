package com.dawidg90;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;

import java.net.URI;
import java.util.Properties;

public class Utils {
    public static RestHighLevelClient createOpenSearchClient() {
        RestHighLevelClient restHighLevelClient;
        URI connectionUri = URI.create("http://localhost:9200");
        String userInfo = connectionUri.getUserInfo();

        if (userInfo == null) {
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connectionUri.getHost(), connectionUri.getPort(), connectionUri.getScheme())));
        } else {
            String[] auth = userInfo.split(":");
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connectionUri.getHost(), connectionUri.getPort(), connectionUri.getScheme()))
                    .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));
        }
        return restHighLevelClient;
    }

    public static KafkaConsumer<String, String> createKafkaConsumer() {
        final String group = "consumer-opensearch";
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new KafkaConsumer<>(properties);
    }

    public static String extractIdFromRecord(String jsonRecord) {
        return JsonParser.parseString(jsonRecord)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }
}
