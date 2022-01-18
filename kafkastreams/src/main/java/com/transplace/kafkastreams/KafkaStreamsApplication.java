package com.transplace.kafkastreams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.time.Duration;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

@EnableKafka
@EnableKafkaStreams
@SpringBootApplication
public class KafkaStreamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamsApplication.class, args);
	}

	@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
		return new KafkaStreamsConfiguration(Map.of(
				APPLICATION_ID_CONFIG, "tpKafkaStreams",
				BOOTSTRAP_SERVERS_CONFIG, "http://localhost:9092",
				DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getClass(),
				DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName(),
				DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName()
		));
	}

	@Bean
	public KStream<Integer, String> kStream(StreamsBuilder kStreamBuilder) {
		KStream<Integer, String> stream = kStreamBuilder.stream("check-call-src");
		stream.mapValues((ValueMapper<String, String>) String::toUpperCase)
				.groupByKey()
				.windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(1000)))
				.reduce((String value1, String value2) -> value1 + value2, Named.as("windowStore"))
				.toStream()
				.map((windowId, value) -> new KeyValue<>(windowId.key(), value))
//				.filter((i, s) -> s.length() > 40)
				.to("check-call-des");
		stream.print(Printed.toSysOut());
		return stream;
	}
}
