package isi.dan.ms.pedidos.conf;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestConfig {

   @Bean
   public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
   }
}
