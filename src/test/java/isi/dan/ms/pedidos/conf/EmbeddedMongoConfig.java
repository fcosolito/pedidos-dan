package isi.dan.ms.pedidos.conf;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.io.IOException;
import java.util.Arrays;

@Configuration
@Profile("test")
public class EmbeddedMongoConfig extends AbstractMongoClientConfiguration {

   private MongodExecutable mongodExecutable;
   private MongodProcess mongodProcess;

   @Override
   protected String getDatabaseName() {
      return "testdb";
   }

   @Override
   public MongoClient mongoClient() {
      return MongoClients.create(mongoClientSettings());
   }

   @Bean(destroyMethod = "stop")
   public MongodProcess mongodProcess() throws IOException {
      MongodStarter starter = MongodStarter.getDefaultInstance();
      mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new de.flapdoodle.embed.mongo.config.Net("localhost", 27017, Network.localhostIsIPv6()))
            .build());
      mongodProcess = mongodExecutable.start();
      return mongodProcess;
   }

   @Override
   protected void finalize() throws Throwable {
      if (mongodProcess != null) {
         mongodProcess.stop();
         mongodExecutable.stop();
      }
      super.finalize();
   }

   @Override
   protected MongoClientSettings mongoClientSettings() {
      return MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress("localhost", 27017))))
            .build();
   }

}
