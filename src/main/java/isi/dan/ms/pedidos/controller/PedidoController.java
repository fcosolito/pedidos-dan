package isi.dan.ms.pedidos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import isi.dan.ms.pedidos.MessageSenderService;
import isi.dan.ms.pedidos.aspect.TokenValidation;
import isi.dan.ms.pedidos.conf.RabbitMQConfig;
import isi.dan.ms.pedidos.dto.StockUpdateDTO;
import isi.dan.ms.pedidos.feignClients.ClienteFeignClient;
import isi.dan.ms.pedidos.feignClients.ProductoFeignClient;
import isi.dan.ms.pedidos.modelo.Cliente;
import isi.dan.ms.pedidos.modelo.DetallePedido;
import isi.dan.ms.pedidos.modelo.Estado;
import isi.dan.ms.pedidos.modelo.EstadoCambioRequest;
import isi.dan.ms.pedidos.modelo.Pedido;
import isi.dan.ms.pedidos.modelo.Producto;
import isi.dan.ms.pedidos.servicio.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;

import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "PedidoController", description = "Permite gestionar los pedidos - implementa Circuit Breaked")
public class PedidoController {

   @Autowired
   private PedidoService pedidoService;

   @Autowired
   private MessageSenderService messageSenderService;

   @Autowired
   private ProductoFeignClient productoFeignClient;

   @Autowired
   private ClienteFeignClient clienteFeignClient;

   private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

   private final AtomicInteger pedidosCount = new AtomicInteger();

   @Autowired
   public PedidoController(MeterRegistry meterRegistry, PedidoService pedidoService,

         MessageSenderService messageSenderService) {
      this.pedidoService = pedidoService;
      this.messageSenderService = messageSenderService;
      Gauge.builder("pedidos.count", pedidosCount, AtomicInteger::get)
            .description("Número de pedidos en el sistema")
            .register(meterRegistry);
   }

   @Timed(value = "pedidos.create.timed", description = "Tiempo de creación de pedidos")
   @PostMapping
   @TokenValidation
   @Operation(summary = "Crear un pedido", description = "Permite crear un nuevo pedido")
   @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Creado correctamente"),
         @ApiResponse(responseCode = "401", description = "No autorizado"),
         @ApiResponse(responseCode = "403", description = "Prohibido"),
         @ApiResponse(responseCode = "404", description = "Error en los datos proporcionados")
   })
   public ResponseEntity<?> createPedido(@RequestBody Pedido pedido) {
      try {
         // Guardar el pedido con los detalles que tienen suficiente stock
         Pedido savedPedido = pedidoService.savePedido(pedido);
         pedidosCount.incrementAndGet();
         log.info("Pedido creado: {} ", savedPedido);
         return ResponseEntity.ok(savedPedido);
      } catch (RuntimeException e) {
         log.info("Pedido no creado por falta de saldo del cliente: {} ");
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
               .body("El cliente no tiene saldo suficiente para aceptar el pedido");
      }
   }

   @PutMapping("/{id}")
   @TokenValidation
   @Operation(summary = "Actualizar un pedido", description = "Permite actualizar un pedido")
   @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
         @ApiResponse(responseCode = "401", description = "No autorizado"),
         @ApiResponse(responseCode = "403", description = "Prohibido"),
         @ApiResponse(responseCode = "404", description = "El ID no existe")
   })
   public ResponseEntity<?> updatePedido(@PathVariable String id, @RequestBody Pedido pedido) {
      try {
         Pedido updatedPedido = pedidoService.updatePedido(id, pedido);
         log.info("Pedido actualizado: {} ", updatedPedido);
         return ResponseEntity.ok(updatedPedido);
      } catch (RuntimeException e) {
         log.error("Error al actualizar el pedido: {}", e.getMessage());
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al actualizar el pedido: " + e.getMessage());
      }
   }

   @Timed(value = "pedidos.getAll.timed", description = "Tiempo de obtener todos los pedidos")
   @GetMapping
   @TokenValidation
   @Operation(summary = "Obtener todos los pedidos", description = "Permite obtener la lista de todos los pedidos")
   @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Obras obtenidos correctamente"),
         @ApiResponse(responseCode = "401", description = "No autorizado"),
         @ApiResponse(responseCode = "403", description = "Prohibido"),
         @ApiResponse(responseCode = "404", description = "No se encontraron obras")
   })
   public List<Pedido> getAllPedidos() {
      log.info("Listando todos los pedidos {}");
      return pedidoService.getAllPedidos();
   }

   @Timed(value = "pedidos.getById.timed", description = "Tiempo de obtener pedido por ID")
   @GetMapping("/{id}")
   @TokenValidation
   @Operation(summary = "Obtener Pedido por ID", description = "Permite obtener un pedido por su ID")
   @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Pedido obtenido correctamente"),
         @ApiResponse(responseCode = "401", description = "No autorizado"),
         @ApiResponse(responseCode = "403", description = "Prohibido"),
         @ApiResponse(responseCode = "404", description = "El ID no existe")
   })
   public ResponseEntity<Pedido> getPedidoById(@PathVariable String id) {
      Pedido pedido = pedidoService.getPedidoById(id);
      log.info("Pedido a buscar con el id: {} ", id);
      return pedido != null ? ResponseEntity.ok(pedido) : ResponseEntity.notFound().build();
   }

   @Timed(value = "pedidos.delete.timed", description = "Tiempo de eliminar pedido")
   @DeleteMapping("/{id}")
   @TokenValidation
   @Operation(summary = "Eliminar un pedido", description = "Permite eliminar un pedido por su ID")
   @ApiResponses(value = {
         @ApiResponse(responseCode = "204", description = "Eliminado correctamente"),
         @ApiResponse(responseCode = "401", description = "No autorizado"),
         @ApiResponse(responseCode = "403", description = "Prohibido"),
         @ApiResponse(responseCode = "404", description = "El ID no existe")
   })
   public ResponseEntity<Void> deletePedido(@PathVariable String id) {
      pedidoService.deletePedido(id);
      pedidosCount.decrementAndGet();
      log.info("Id del Pedido eliminado: {} ", id);
      return ResponseEntity.noContent().build();
   }

   // Método para actualizar el ESTADO del pedido
   @PutMapping("/{id}/estado")
   @TokenValidation
   @Operation(summary = "Actualizar el Estado de un  pedido", description = "Permite actualizar el estado de un pedido")
   @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Actualizado correctamente"),
         @ApiResponse(responseCode = "401", description = "No autorizado"),
         @ApiResponse(responseCode = "403", description = "Prohibido"),
         @ApiResponse(responseCode = "404", description = "El ID no existe")
   })
   public ResponseEntity<Pedido> updatePedidoEstado(@PathVariable String id, @RequestBody EstadoCambioRequest request) {
      Pedido pedido = pedidoService.getPedidoById(id);

      if (pedido != null) {
         pedido.setEstado(request.getNuevoEstado());
         pedido.addEstadoCambio(request.getNuevoEstado(), request.getUsuarioCambio());

         if (request.getNuevoEstado() == Estado.CANCELADO) {
            // Construir y enviar el DTO a RabbitMQ
            for (DetallePedido detalle : pedido.getDetalle()) {
               StockUpdateDTO stockUpdateDTO = new StockUpdateDTO();
               stockUpdateDTO.setIdProducto(detalle.getProducto().getId());
               stockUpdateDTO.setCantidad(detalle.getCantidad());

               // Enviar el mensaje a la cola de RabbitMQ
               messageSenderService.sendMessage(RabbitMQConfig.STOCK_UPDATE_QUEUE, stockUpdateDTO);
            }
         }

         Pedido updatedPedido = pedidoService.updatePedido(pedido);
         log.info("Pedido después de actualizarse: {} ", updatedPedido);
         return ResponseEntity.ok(updatedPedido);
      } else {
         return ResponseEntity.notFound().build();
      }
   }

   @Retry(name = "clientesRetry")
   @PostMapping("/{id}/cliente")
   @TokenValidation
   @CircuitBreaker(name = "clientesCB", fallbackMethod = "fallbackSaveCliente")
   public ResponseEntity<Pedido> addClienteToPedido(@PathVariable String id, @RequestBody Cliente cliente) {
      Cliente savedCliente = clienteFeignClient.guardarCliente(cliente);

      Pedido updatedPedido = pedidoService.addClienteToPedido(id, savedCliente);
      log.info("Cliente agregado al pedido: {} ", savedCliente);
      return ResponseEntity.ok(updatedPedido);
   }

   @Retry(name = "productosRetry")
   @PostMapping("/{id}/detalle")
   @TokenValidation
   @CircuitBreaker(name = "productosCB", fallbackMethod = "fallbackSaveProducto")
   public ResponseEntity<Pedido> addProductoToDetalle(@PathVariable String id, @RequestBody DetallePedido detalle) {
      Producto savedProducto = productoFeignClient.agregarProducto(detalle.getProducto());

      detalle.setProducto(savedProducto);
      Pedido updatedPedido = pedidoService.addProductoToDetalle(id, detalle);
      log.info("Pedido después de agregar detalle: {} ", updatedPedido);
      return ResponseEntity.ok(updatedPedido);
   }

   @Retry(name = "productosRetry")
   @GetMapping("/productos/{pedidoId}")
   @TokenValidation
   @CircuitBreaker(name = "productosCB", fallbackMethod = "fallbackGetProductos")
   public ResponseEntity<List<Producto>> getProductos(@PathVariable("pedidoId") String pedidoId) {
      Pedido pedido = pedidoService.getPedidoById(pedidoId);
      if (pedido == null) {
         log.error("Pedido no encontrado: {} ", pedidoId);
         return ResponseEntity.notFound().build();
      }
      List<Long> productoIds = pedido.getDetalle().stream()
            .map(detalle -> detalle.getProducto().getId())
            .collect(Collectors.toList());

      log.info("Lista de Productos productoIds: {}", productoIds.toString());

      List<Producto> productos = productoFeignClient.getProductosByIds(productoIds);
      return ResponseEntity.ok(productos);
   }

   @Retry(name = "clientesRetry")
   @GetMapping("/cliente/{pedidoId}")
   @CircuitBreaker(name = "clientesCB", fallbackMethod = "fallbackGetClientes")
   @TokenValidation
   public ResponseEntity<Cliente> getCliente(@PathVariable("pedidoId") String pedidoId) {
      Pedido pedido = pedidoService.getPedidoById(pedidoId);
      if (pedido == null) {
         return ResponseEntity.notFound().build();
      }
      Cliente cliente = clienteFeignClient.getCliente(pedido.getCliente().getId());
      return ResponseEntity.ok(cliente);
   }

   private ResponseEntity<Cliente> fallbackGetClientes(String pedidoId, Throwable e) {
      log.error("Error al obtener cliente para pedido {}: {}", pedidoId, e.getMessage());
      return new ResponseEntity("No se pudo obtener el cliente para el pedido", HttpStatus.OK);
   }

   private ResponseEntity<List<Producto>> fallbackGetProductos(String pedidoId, Throwable e) {
      log.error("Error al obtener productos para pedido {}: {}", pedidoId, e.getMessage());
      return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
   }

   private ResponseEntity<Cliente> fallbackSaveCliente(@PathVariable("id") String id, @RequestBody Cliente cliente,
         Throwable e) {
      log.error("Error al guardar cliente para pedido {}: {}", id, e.getMessage());
      return new ResponseEntity("No se pudo guardar el cliente para el pedido", HttpStatus.OK);
   }

   private ResponseEntity<Pedido> fallbackSaveProducto(@PathVariable("id") String id,
         @RequestBody DetallePedido detalle, Throwable e) {
      log.error("Error al guardar producto para pedido {}: {}", id, e.getMessage());
      return new ResponseEntity("No se pudo guardar los productos para el pedido", HttpStatus.OK);
   }

}