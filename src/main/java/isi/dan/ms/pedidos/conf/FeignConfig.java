package isi.dan.ms.pedidos.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import isi.dan.ms.pedidos.feignClients.FeignClientInterceptor;

@Configuration
public class FeignConfig {

   @Bean
   public FeignClientInterceptor feignClientInterceptor() {
      return new FeignClientInterceptor();
   }
}
