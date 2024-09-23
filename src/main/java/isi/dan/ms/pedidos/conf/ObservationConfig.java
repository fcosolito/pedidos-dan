package isi.dan.ms.pedidos.conf;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservationConfig {
   @Bean
   public ObservationRegistry observationRegistry() {
      return ObservationRegistry.create();
   }
}
