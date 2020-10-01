package com.redhat;

import java.io.File;
import java.io.IOException;
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
    private String[] country = new String[] { "US", "UK", "NZ","CN","IN"};
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
            "\t\t\t\"type\": {\n" +
            "\t\t\t\t\"name\": \"EnumType\",\n" +
            "\t\t\t\t\"type\": \"enum\",\n" +
            "\t\t\t\t\"symbols\": [\"US\", \"UK\", \"IR\", \"FR\"]\n" +
            "\t\t\t}\n" +
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

    @Incoming("transaction-in")
    public CompletionStage<Void> receive(KafkaMessage<String,Record> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println(message.getPayload());
            }catch(Exception e) {
                System.out.println("Failed"+message.getPayload());
            }
          });
    }

}