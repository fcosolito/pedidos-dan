package isi.dan.ms.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
      "spring.data.mongodb.host=localhost",
      "spring.data.mongodb.port=27017",
})
class PedidosApplicationTests {

   @Test
   void contextLoads() {

   }
}
