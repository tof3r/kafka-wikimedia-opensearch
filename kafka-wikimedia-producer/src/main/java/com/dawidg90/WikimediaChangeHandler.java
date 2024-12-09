package com.dawidg90;

import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikimediaChangeHandler implements BackgroundEventHandler {

    private final Logger logger = LoggerFactory.getLogger(WikimediaChangesProducer.class.getSimpleName());
    private KafkaProducer<String, String> producer;
    private String topic;

    public WikimediaChangeHandler(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void onOpen() {
        // no-op
    }

    @Override
    public void onClosed() {
        producer.close();
    }

    @Override
    public void onMessage(String s, MessageEvent messageEvent) throws Exception {
        logger.info(messageEvent.getData());
        producer.send(new ProducerRecord<>(topic, messageEvent.getData()));
    }

    @Override
    public void onComment(String s) {
        //no-op
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("Error in stream reading", throwable);
    }
}
