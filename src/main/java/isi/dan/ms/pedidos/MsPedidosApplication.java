package isi.dan.ms.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "isi.dan.ms.pedidos.feignClients")
@EnableAspectJAutoProxy
@EnableMongoRepositories(basePackages = "isi.dan.ms.pedidos.dao")
public class MsPedidosApplication {

   public static void main(String[] args) {
      SpringApplication.run(MsPedidosApplication.class, args);
   }

   @Bean
   TimedAspect timedAspect(MeterRegistry registry) {
      return new TimedAspect(registry);
   }
}
