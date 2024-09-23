package isi.dan.ms.pedidos.conf;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String STOCK_UPDATE_QUEUE = "stock-update-queue";

    @Bean
    Queue stockUpdateQueue() {
        return new Queue(STOCK_UPDATE_QUEUE, true);
    }
}

