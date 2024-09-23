package isi.dan.ms.pedidos.controller;

import isi.dan.ms.pedidos.MessageSenderService;
import isi.dan.ms.pedidos.aspect.JwtUtility;
import isi.dan.ms.pedidos.conf.EmbeddedMongoConfig;
import isi.dan.ms.pedidos.feignClients.ClienteFeignClient;
import isi.dan.ms.pedidos.feignClients.ProductoFeignClient;
import isi.dan.ms.pedidos.modelo.Cliente;
import isi.dan.ms.pedidos.modelo.DetallePedido;
import isi.dan.ms.pedidos.modelo.Estado;
import isi.dan.ms.pedidos.modelo.EstadoCambioRequest;
import isi.dan.ms.pedidos.modelo.Pedido;
import isi.dan.ms.pedidos.modelo.Producto;
import isi.dan.ms.pedidos.servicio.PedidoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoConfigureMockMvc
@WebMvcTest(controllers = PedidoController.class)
@Import(EmbeddedMongoConfig.class)
@ActiveProfiles("test")
public class PedidoControllerTest {

   @Autowired
   private MockMvc mockMvc;

   @MockBean
   private PedidoService pedidoService;

   @MockBean
   private MessageSenderService messageSenderService;

   @MockBean
   private ProductoFeignClient productoFeignClient;

   @MockBean
   private ClienteFeignClient clienteFeignClient;

   @MockBean
   private MeterRegistry meterRegistry;

   @MockBean
   private JwtUtility jwtUtil;

   String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjQiLCJpYXQiOjE3MjE2NzgzOTAsImV4cCI6MTcyMjI4MzE5MH0.4GsTGu0Yc9-irygXLqg6cCh05IES4VVHzgsxCp-y4cE";

   @BeforeEach
   public void setUp() {
      mockMvc = MockMvcBuilders
            .standaloneSetup(new PedidoController(meterRegistry, pedidoService, messageSenderService)).build();

      Claims claims = new DefaultClaims();
      claims.setSubject("user");

      when(jwtUtil.validateToken(anyString())).thenReturn(claims);

   }

   @Test
   public void testCreatePedido() throws Exception {
      Pedido pedido = new Pedido();
      pedido.setId("123");
      Pedido savedPedido = new Pedido();
      savedPedido.setId("123");
      when(pedidoService.savePedido(any(Pedido.class))).thenReturn(savedPedido);

      mockMvc.perform(post("/api/pedidos")
            .header("Authorization", "Bearer "
                  + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(pedido)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists());
   }

   @Test
   public void testUpdatePedido() throws Exception {
      Pedido pedido = new Pedido();
      pedido.setId("123");
      Pedido updatedPedido = new Pedido();
      updatedPedido.setId("123");
      when(pedidoService.updatePedido(anyString(), any(Pedido.class))).thenReturn(updatedPedido);

      mockMvc.perform(put("/api/pedidos/{id}", "123")
            .header("Authorization", "Bearer " + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(pedido)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"));
   }

   // Nuevos test
   @Test
   public void testCreatePedido_ClienteSinSaldoSuficiente() throws Exception {
      // Crear cliente
      Cliente cliente = new Cliente();
      cliente.setId(1);

      // Crear detalles del pedido
      Producto producto = new Producto();
      producto.setId(1L);
      producto.setPrecio(BigDecimal.valueOf(100));

      DetallePedido detalle = new DetallePedido();
      detalle.setProducto(producto);
      detalle.setCantidad(2);
      detalle.setPrecioUnitario(BigDecimal.valueOf(100));

      // Crear pedido
      Pedido pedido = new Pedido();
      pedido.setCliente(cliente);
      pedido.setDetalle(List.of(detalle));

      // Simular que el cliente no tiene saldo suficiente
      when(clienteFeignClient.verificarSaldo(eq(1), anyDouble())).thenReturn(false);

      // Simular que el servicio lanza una RuntimeException
      doThrow(new RuntimeException("El cliente no tiene saldo suficiente para aceptar el pedido"))
            .when(pedidoService).savePedido(any(Pedido.class));

      mockMvc.perform(post("/api/pedidos")
            .header("Authorization", "Bearer " + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(pedido)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("El cliente no tiene saldo suficiente para aceptar el pedido"));
   }

   @Test
   public void testCreatePedido_ClienteConSaldoSuficienteYStockDisponible() throws Exception {
      // Crear cliente
      Cliente cliente = new Cliente();
      cliente.setId(1);

      // Crear detalles del pedido
      Producto producto = new Producto();
      producto.setId(1L);
      producto.setPrecio(BigDecimal.valueOf(100));

      DetallePedido detalle = new DetallePedido();
      detalle.setProducto(producto);
      detalle.setCantidad(2);
      detalle.setPrecioUnitario(BigDecimal.valueOf(100));

      // Crear pedido
      Pedido pedido = new Pedido();
      pedido.setCliente(cliente);
      pedido.setDetalle(List.of(detalle));

      when(clienteFeignClient.verificarSaldo(eq(1), anyDouble())).thenReturn(true);
      when(productoFeignClient.verificarStock(eq(1L), anyMap())).thenReturn(Map.of("stockDisponible", true));

      // Mockear savePedido para devolver el pedido con estado ACEPTADO
      when(pedidoService.savePedido(any(Pedido.class))).thenAnswer(invocation -> {
         Pedido pedidoGuardado = invocation.getArgument(0);
         pedidoGuardado.setEstado(Estado.ACEPTADO);
         return pedidoGuardado;
      });

      mockMvc.perform(post("/api/pedidos")
            .header("Authorization", "Bearer " + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(pedido)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value(Estado.ACEPTADO.name()));
   }

   @Test
   public void testCreatePedido_ClienteConSaldoSuficienteSinStockSuficiente() throws Exception {
      // Crear cliente
      Cliente cliente = new Cliente();
      cliente.setId(1);

      // Crear detalles del pedido
      Producto producto = new Producto();
      producto.setId(1L);
      producto.setPrecio(BigDecimal.valueOf(100));

      DetallePedido detalle = new DetallePedido();
      detalle.setProducto(producto);
      detalle.setCantidad(2);
      detalle.setPrecioUnitario(BigDecimal.valueOf(100));

      // Crear pedido
      Pedido pedido = new Pedido();
      pedido.setCliente(cliente);
      pedido.setDetalle(List.of(detalle));

      when(clienteFeignClient.verificarSaldo(eq(1), anyDouble())).thenReturn(true);
      when(productoFeignClient.verificarStock(eq(1L), anyMap())).thenReturn(Map.of("stockDisponible", false));

      // Mockear savePedido para devolver el pedido con estado EN_PREPARACION
      when(pedidoService.savePedido(any(Pedido.class))).thenAnswer(invocation -> {
         Pedido pedidoGuardado = invocation.getArgument(0);
         pedidoGuardado.setEstado(Estado.EN_PREPARACION);
         return pedidoGuardado;
      });

      mockMvc.perform(post("/api/pedidos")
            .header("Authorization", "Bearer " + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(pedido)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value(Estado.EN_PREPARACION.name()));
   }

   @Test
   public void testUpdatePedido_ClienteConSaldo_StockInsuficiente() throws Exception {
      // Crear cliente
      Cliente cliente = new Cliente();
      cliente.setId(1);

      // Crear detalles del pedido con producto
      Producto producto = new Producto();
      producto.setId(1L);
      producto.setPrecio(BigDecimal.valueOf(100));

      DetallePedido detalle = new DetallePedido();
      detalle.setProducto(producto);
      detalle.setCantidad(2);
      detalle.setPrecioUnitario(BigDecimal.valueOf(100));

      // Crear pedido con detalles
      Pedido pedido = new Pedido();
      pedido.setId("123");
      pedido.setCliente(cliente);
      pedido.setDetalle(List.of(detalle));

      // Configurar mocks
      when(clienteFeignClient.verificarSaldo(eq(1), anyDouble())).thenReturn(true);
      when(productoFeignClient.verificarStock(eq(1L), anyMap()))
            .thenReturn(Collections.singletonMap("stockDisponible", false));

      // Simular la lógica de actualización en el servicio
      when(pedidoService.updatePedido(eq("123"), any(Pedido.class))).thenAnswer(invocation -> {
         Pedido pedidoActualizado = invocation.getArgument(1);
         pedidoActualizado.setEstado(Estado.EN_PREPARACION);
         return pedidoActualizado;
      });

      mockMvc.perform(put("/api/pedidos/{id}", "123")
            .header("Authorization", "Bearer " + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(pedido)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value(Estado.EN_PREPARACION.name()));
   }

   // Fin de nuevos test

   @Test
   public void testGetAllPedidos() throws Exception {
      Pedido pedido = new Pedido();
      pedido.setId("123");
      List<Pedido> pedidos = Collections.singletonList(pedido);
      when(pedidoService.getAllPedidos()).thenReturn(pedidos);

      mockMvc.perform(get("/api/pedidos")
            .header("Authorization", "Bearer "
                  + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").exists());
   }

   @Test
   public void testGetPedidoById() throws Exception {
      Pedido pedido = new Pedido();
      pedido.setId("123");
      when(pedidoService.getPedidoById("123")).thenReturn(pedido);

      mockMvc.perform(get("/api/pedidos/123")
            .header("Authorization", "Bearer "
                  + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"));
   }

   @Test
   public void testDeletePedido() throws Exception {
      doNothing().when(pedidoService).deletePedido("123");

      mockMvc.perform(delete("/api/pedidos/123")
            .header("Authorization", "Bearer "
                  + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
   }

   @Test
   public void testUpdatePedidoEstado() throws Exception {
      Pedido pedido = new Pedido();
      pedido.setId("123");
      pedido.setEstado(Estado.EN_PREPARACION);
      pedido.setDetalle(new ArrayList<>());

      EstadoCambioRequest request = new EstadoCambioRequest();
      request.setNuevoEstado(Estado.CANCELADO);
      request.setUsuarioCambio("user1");

      Pedido updatedPedido = new Pedido();
      updatedPedido.setId("123");
      updatedPedido.setEstado(Estado.CANCELADO);

      when(pedidoService.getPedidoById("123")).thenReturn(pedido);
      when(pedidoService.updatePedido(any(Pedido.class))).thenReturn(updatedPedido);

      mockMvc.perform(put("/api/pedidos/123/estado")
            .header("Authorization", "Bearer "
                  + validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.estado").value("CANCELADO"));
   }

   private static String asJsonString(final Object obj) {
      try {
         return new ObjectMapper().writeValueAsString(obj);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}