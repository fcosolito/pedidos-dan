package isi.dan.ms.pedidos.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@Configuration
public class MetricsConfig {

   @Bean
   PrometheusMeterRegistry prometheusMeterRegistry() {
      return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
   }
}
