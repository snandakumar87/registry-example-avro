package com.redhat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.reactivex.Flowable;
import io.smallrye.reactive.messaging.kafka.KafkaMessage;

@ApplicationScoped
public class RegistryExampleAvro {

    private Random random = new Random();
    private String[] country = new String[] { "US", "UK", "IR","FR"};
    private String[] merchantId = new String[] { "MERCH0001", "MERCH0002", "MERCH003", "MERCH004" };
    String schemaString="{\n" +
            "\t\"type\": \"record\",\n" +
            "\t\"name\": \"transaction\",\n" +
            "\t\"namespace\": \"com.redhat\",\n" +
            "\t\"fields\": [{\n" +
            "\t\t\t\"name\": \"id\",\n" +
            "\t\t\t\"type\": \"string\",\n" +
            "\t\t\t\"minimum\": 0\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"amount\",\n" +
            "\t\t\t\"type\": \"string\"\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"country\",\n" +
            "\t\t\t\"type\":\"string\"\n" +
            "\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"merchantId\",\n" +
            "\t\t\t\"type\": \"string\"\n" +
            "\t\t}\n" +
            "\t]\n" +
            "}";

    @Outgoing("transaction-out")
    public Flowable<KafkaMessage<Object, Record>> generate()  {
        try {
            Schema schema = new Schema.Parser().parse(schemaString
            );
            AtomicInteger counter = new AtomicInteger(-3);
            return Flowable.interval(1000, TimeUnit.MILLISECONDS)
                    .onBackpressureDrop()
                    .map(tick -> {
                        Record record = new GenericData.Record(schema);
                        record.put("id", String.valueOf(counter.getAndIncrement()));
                        record.put("country", country[random.nextInt(4)]);
                        record.put("merchantId", merchantId[random.nextInt(4)]);
                        record.put("amount", String.format("%.2f", random.nextDouble() * 100));
                        return KafkaMessage.of(record.get("transaction"), record);
                    });
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<Integer, Record> stations = new HashMap<>();

    @Outgoing("transaction-in")
    public Flowable<KafkaMessage<Object, Record>> generateWhiteList() {
        System.out.println("came here");
        Schema schema = new Schema.Parser().parse(schemaString
        );

        return Flowable.interval(500, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .map(tick -> {
                    Record record = stations.get(counter);
                    record.put("id", String.valueOf(record.get("id")));
                    record.put("country", stations.get("country"));
                    record.put("merchantId", stations.get("merchantId"));
                    record.put("amount", stations.get("amount"));
                    return KafkaMessage.of(record.get("transaction"), record);

                });
    }

    static int counter=0;
    @Incoming("transaction-out")
    public CompletionStage<Void> receive(KafkaMessage<Integer,Record> message) throws IOException {

        return CompletableFuture.runAsync(() -> {
            try {
                if(message.getPayload().get("merchantId").equals("MERCH0001") && message.getPayload().get("country") == "UK") {
                    System.out.println("Failed Country mismatch"+stations);
                } if(message.getPayload().get("merchantId").equals("MERCH0002") && message.getPayload().get("country") == "IR") {
                    System.out.println("Failed Country mismatch"+stations);
                } else {
                    stations.put(++counter,message.getPayload());
                }

            }catch(Exception e) {
                e.printStackTrace();
                System.out.println("Failed"+message.getPayload());

            }
          });
    }

}