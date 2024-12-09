# Wikimedia-Kafka-OpenSearch
Project utilizes Kafka to transfer data between two systems.

Wikimedia recent changes stream is processed and send to Kafka.
Records from Kafka are consumed and indexed using OpenSearch.

## Libs used:
- Java 17
- Kafka Clients v. 3.9.0
- Slf4j API v.2.0.16
- Slf4j Reload4j v.2.0.16
- Okhttp v.4.12.0
- Okhttp Eventsource v.4.1.1
- OpenSearch Rest High Level Client v.2.18.0
- GSon v.2.11.0

### How to run it using Docker
- First you must have a running Kafka broker. I'm using KRaft mode. You can run by `docker run -d -p 9092:9092 --name broker apache/kafka:latest`
- When Kafka broker is running then navigate in terminal to the location of the `../kafka-wikimedia-opensearch/kafka-opensearch-consumer/` 
- Next to the `docker-compose.yml` file create the `.env` file to store `OPENSEARCH_INITIAL_ADMIN_PASSWORD=<you_password_here>`. It is required by the OpenSearch >= 2.12.
- To run containers with OpenSearch and OpenSearch Dashboards where you can play with data from dev tools console, you need to run `docker-compose.yml` file with command `docker-compose up` and wait for services initialization.
- You can check if OpenSearch is up and running by launching `curl localhost:9092` in command line or type the `localhost:9092` in web browser. Regardless of the chosen method you should see output similar to this:
```
{
  "name" : "de3dea97bf8a",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "Z8TFh28BR967XGtPLtKptg",
  "version" : {
    "number" : "7.10.2",
    "build_type" : "tar",
    "build_hash" : "99a9a81da366173b0c2b963b26ea92e15ef34547",
    "build_date" : "2024-10-31T19:08:39.157471098Z",
    "build_snapshot" : false,
    "lucene_version" : "9.12.0",
    "minimum_wire_compatibility_version" : "7.10.0",
    "minimum_index_compatibility_version" : "7.0.0"
  },
  "tagline" : "The OpenSearch Project: https://opensearch.org/"
}
```
- In web browser navigate to the `http://localhost:5601/app/dev_tools#/console` to see the OpenSearch dashboard and play with data indexed by `OpenSearchConsumer`

### Stop the Docker containers
- To stop OpenSearch containers run `docker-compose down`
- To stop Kafka broker run `docker stop broker`