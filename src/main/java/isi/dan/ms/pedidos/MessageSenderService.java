package isi.dan.ms.pedidos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
public class MessageSenderService {

   @Autowired
   private AmqpTemplate amqpTemplate;

   public void sendMessage(String queueName, Object message) {
      try {
         String messageString = new ObjectMapper().writeValueAsString(message);
         amqpTemplate.convertAndSend(queueName, messageString);
      } catch (JsonProcessingException e) {
         e.printStackTrace();
      }
   }
}
