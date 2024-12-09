package com.dawidg90;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static com.dawidg90.Utils.*;

public class OpenSearchConsumer {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());
        RestHighLevelClient openSearchClient = createOpenSearchClient();
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Detected shutdown, let's exit by calling consumer.wakeup()");
            consumer.wakeup();

            try {
                mainThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        try (openSearchClient; consumer) {
            handleIndexCreation(openSearchClient, logger);
            consumer.subscribe(List.of("wikimedia.recentchange"));
            processReceivedRecords(consumer, logger, openSearchClient);
        } catch (WakeupException wakeupException) {
            logger.info("Consumer is starting to shut down.");
        } catch (Exception e) {
            logger.error("Exception happened in consumer.", e);
        } finally {
            logger.info("OpenSearch is now gracefully shut down.");
            logger.info("Consumer is now gracefully shut down.");
        }

    }

    private static void processReceivedRecords(KafkaConsumer<String, String> consumer, Logger logger,
                                               RestHighLevelClient openSearchClient) throws IOException {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            int recordCount = records.count();
            logger.info("Received " + recordCount + " records.");

            BulkRequest bulkRequest = new BulkRequest();
            populateBulkRequest(logger, records, bulkRequest);
            if (bulkRequest.numberOfActions() > 0) {
                processBulkRequest(logger, openSearchClient, bulkRequest);

                //at-least-once approach - commit only after batch was processed
                consumer.commitSync();
                logger.info("Offsets has been commited.");
            }
        }
    }

    private static void processBulkRequest(Logger logger, RestHighLevelClient openSearchClient, BulkRequest bulkRequest) throws IOException {
        BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        logger.info("Inserted: " + bulkResponse.getItems().length + " records.");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Exception during thread execution.", e);
        }
    }

    private static void populateBulkRequest(Logger logger, ConsumerRecords<String, String> records, BulkRequest bulkRequest) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                String id = extractIdFromRecord(record.value());
                IndexRequest indexRequest = new IndexRequest("wikimedia")
                        .source(record.value(), XContentType.JSON)
                        .id(id);
                bulkRequest.add(indexRequest);
            } catch (Exception e) {
                logger.error("Exception during processing.", e);
            }
        }
    }

    private static void handleIndexCreation(RestHighLevelClient openSearchClient, Logger logger) throws IOException {
        boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);
        if (!indexExists) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
            openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            logger.info("Index 'wikimedia' created.");
        } else {
            logger.info("Index 'wikimedia' already exists.");
        }
    }
}
