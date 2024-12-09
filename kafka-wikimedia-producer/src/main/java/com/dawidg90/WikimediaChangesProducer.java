package com.dawidg90;

import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import com.launchdarkly.eventsource.background.BackgroundEventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {

    public static void main(String[] args) throws InterruptedException {
        BackgroundEventHandler eventHandler = getBackgroundEventHandler();
        BackgroundEventSource eventSource = getBackgroundEventSource(eventHandler);

        try(eventSource) {
            eventSource.start();
            TimeUnit.MINUTES.sleep(1);
        }
    }

    private static BackgroundEventSource getBackgroundEventSource(BackgroundEventHandler eventHandler) {
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";
        BackgroundEventSource.Builder builder = new BackgroundEventSource.Builder(eventHandler, new EventSource.Builder(URI.create(url)));
        return builder.build();
    }

    @NotNull
    private static BackgroundEventHandler getBackgroundEventHandler() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        //properties for high throughput producer
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        String topic = "wikimedia.recentchange";

        return new WikimediaChangeHandler(producer, topic);
    }
}
