package isi.dan.ms.pedidos.servicio;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import feign.FeignException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import isi.dan.ms.pedidos.dao.PedidoRepository;
import isi.dan.ms.pedidos.feignClients.ClienteFeignClient;
import isi.dan.ms.pedidos.feignClients.ProductoFeignClient;
import isi.dan.ms.pedidos.modelo.Cliente;
import isi.dan.ms.pedidos.modelo.DetallePedido;
import isi.dan.ms.pedidos.modelo.Estado;
import isi.dan.ms.pedidos.modelo.EstadoCambio;
import isi.dan.ms.pedidos.modelo.Pedido;
import isi.dan.ms.pedidos.modelo.Producto;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PedidoService {

   @Autowired
   private PedidoRepository pedidoRepository;

   @Autowired
   private RabbitTemplate rabbitTemplate;

   @Autowired
   private ClienteFeignClient clienteFeignClient;

   @Autowired
   private ProductoFeignClient productoFeignClient;

   @Autowired
   private RestTemplate restTemplate;

   @Autowired
   private SequenceGeneratorService sequenceGeneratorService;

   Logger log = LoggerFactory.getLogger(PedidoService.class);

   @Autowired
   private ObservationRegistry observationRegistry;

   @PostConstruct
   public void init() {
      log.info("Microservicio de Pedidos iniciado y enviando logs a Graylog");
   }

   public Pedido savePedido(Pedido pedido) {
      return Observation.createNotStarted("pedido.save", observationRegistry)
            .observe(() -> {
               validarYCalcularTotales(pedido);
               pedido.setNumeroPedido((int) sequenceGeneratorService.generateSequence(Pedido.SEQUENCE_NAME));
               pedido.setFecha(Instant.now());
               agregarEstadoInicial(pedido, Estado.ACEPTADO);
               return pedidoRepository.save(pedido);
            });
   }

   public Pedido updatePedido(String id, Pedido pedido) {
      return Observation.createNotStarted("pedido.update", observationRegistry)
            .observe(() -> {
               Optional<Pedido> optionalPedidoExistente = pedidoRepository.findById(id);

               if (!optionalPedidoExistente.isPresent()) {
                  throw new RuntimeException("Pedido no encontrado");
               }

               Pedido pedidoExistente = optionalPedidoExistente.get();
               actualizarCamposPermitidos(pedidoExistente, pedido);
               validarYCalcularTotales(pedidoExistente);
               agregarEstadoCambio(pedidoExistente, pedido.getEstado(), pedido.getUsuario());
               return pedidoRepository.save(pedidoExistente);
            });
   }

   // Método agregado para la actualización del ESTADO del pedido
   public Pedido updatePedido(Pedido pedido) {
      return Observation.createNotStarted("pedido.update", observationRegistry)
            .observe(() -> {
               return pedidoRepository.save(pedido);
            });
   }

   public List<Pedido> getAllPedidosCliente(Integer clienteId) {
      return pedidoRepository.findByClienteId(clienteId);
   }

   public List<Pedido> getAllPedidos() {
      return Observation.createNotStarted("pedido.getAll", observationRegistry)
            .observe(() -> pedidoRepository.findAll());
   }

   public Pedido getPedidoById(String id) {
      return Observation.createNotStarted("pedido.getById", observationRegistry)
            .observe(() -> pedidoRepository.findById(id).orElse(null));
   }

   public void deletePedido(String id) {
      Observation.createNotStarted("pedido.delete", observationRegistry)
            .observe(() -> pedidoRepository.deleteById(id));
   }

   public Pedido addClienteToPedido(String pedidoId, Cliente cliente) {
      return Observation.createNotStarted("pedido.addCliente", observationRegistry)
            .observe(() -> {
               Pedido pedido = getPedidoById(pedidoId);
               if (pedido != null) {
                  try {
                     Cliente savedCliente = clienteFeignClient.guardarCliente(cliente);
                     pedido.setCliente(savedCliente);
                     return pedidoRepository.save(pedido);
                  } catch (Exception e) {
                     log.error("Error al agregar cliente al pedido: {}", e.getMessage());
                  }
               }
               return null;
            });
   }

   public Pedido addProductoToDetalle(String pedidoId, DetallePedido detalle) {
      return Observation.createNotStarted("pedido.addProducto", observationRegistry)
            .observe(() -> {
               Pedido pedido = getPedidoById(pedidoId);
               log.info("DetallePedido recibido: {}", detalle);
               log.info("Pedido id: {}", pedidoId);
               log.info("Pedido: {}", pedido);
               if (pedido != null) {
                  try {
                     Producto savedProducto = productoFeignClient.agregarProducto(detalle.getProducto());
                     detalle.setProducto(savedProducto);
                     pedido.getDetalle().add(detalle);
                     Pedido updatedPedido = pedidoRepository.save(pedido);
                     log.info("Pedido después de agregar detalle: {}", updatedPedido);
                     return updatedPedido;
                  } catch (Exception e) {
                     log.error("Error al agregar producto al detalle del pedido: {}", e.getMessage());
                  }
               }
               return null;
            });
   }

   public Cliente obtenerClientePorPedidoId(Integer clienteId) {
      return Observation.createNotStarted("pedido.obtenerClientePorPedidoId", observationRegistry)
            .observe(() -> {
               String url = "http://ms-clientes/api/clientes/" + clienteId;
               try {
                  return restTemplate.getForObject(url, Cliente.class);
               } catch (Exception e) {
                  log.error("Error al obtener cliente por ID: {}", e.getMessage());
                  return null;
               }
            });
   }

   public Cliente obtenerClientePorNombre(String Nombre) {
      System.out.println("Este es un mensaje de la modificacion B.");
      return Observation.createNotStarted("pedido.obtenerClientePorNombre", observationRegistry)
            .observe(() -> {
               String url = "http://ms-clientes/api/clientes/" + nombreCliente;
               try {
                  return restTemplate.getForObject(url, Cliente.class);
               } catch (Exception e) {
                  log.error("Error al obtener cliente por nombre: {}", e.getMessage());
                  return null;
               }
            });
   }

   public List<Producto> obtenerProductosPorIds(List<String> productoIds) {
      return Observation.createNotStarted("pedido.obtenerProductosPorIds", observationRegistry)
            .observe(() -> {
               String ids = String.join(",", productoIds);
               String url = "http://ms-productos/api/productos?ids=" + ids;
               try {
                  ResponseEntity<List<Producto>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<Producto>>() {
                        });
                  log.info("Productos encontrados: {}", response.getBody());
                  return response.getBody();
               } catch (Exception e) {
                  log.error("Error al obtener productos por IDs: {}", e.getMessage());
                  return null;
               }
            });
   }

   // Refactorizando el código común de update y save
   private void validarYCalcularTotales(Pedido pedido) {
      double montoTotalPedidoActual = pedido.getDetalle().stream()
            .mapToDouble(dp -> dp.getProducto().getPrecio().doubleValue() * dp.getCantidad())
            .sum();
      BigDecimal totalBigDecimal = BigDecimal.valueOf(montoTotalPedidoActual);
      pedido.setTotal(totalBigDecimal);

      List<Pedido> pedidosCliente = this.getAllPedidosCliente(pedido.getCliente().getId());
      double montoTotalPedidosExistentes = pedidosCliente.stream()
            .filter(p -> p.getEstado() == Estado.ACEPTADO || p.getEstado() == Estado.EN_PREPARACION)
            .mapToDouble(p -> p.getTotal().doubleValue())
            .sum();

      double montoTotal = montoTotalPedidoActual + montoTotalPedidosExistentes;
      totalBigDecimal = BigDecimal.valueOf(montoTotal);
      pedido.setTotal(BigDecimal.valueOf(montoTotalPedidoActual));

      if (!clienteFeignClient.verificarSaldo(pedido.getCliente().getId(), montoTotal)) {
         log.info("El cliente no tiene saldo suficiente para aceptar el pedido");
         pedido.setEstado(Estado.RECHAZADO);
         // throw new RuntimeException("El cliente no tiene saldo suficiente para aceptar
         // el pedido");
      } else {
         pedido.setEstado(Estado.ACEPTADO);
         verificarStockProductos(pedido);
      }
   }

   private void verificarStockProductos(Pedido pedido) {

      for (DetallePedido dp : pedido.getDetalle()) {
         Map<String, Integer> requestBody = new HashMap<>();
         requestBody.put("cantidad", dp.getCantidad());

         try {
            Map<String, Boolean> stockResponse = productoFeignClient.verificarStock(dp.getProducto().getId(),
                  requestBody);
            Boolean stockSuficiente = stockResponse.get("stockDisponible");
            if (!stockSuficiente) {
               log.info("No hay suficiente stock para el producto {}", dp.getProducto().getId());
               pedido.setEstado(Estado.EN_PREPARACION);
            } else {
               productoFeignClient.actualizarStock(dp.getProducto().getId(), requestBody);
            }
         } catch (FeignException e) {
            log.error(String.format("Error verificando stock para el producto %d: %s", dp.getProducto().getId(),
                  e.getMessage()));
            pedido.setEstado(Estado.EN_PREPARACION);
         }
      }

   }

   private void agregarEstadoInicial(Pedido pedido, Estado estado) {
      List<EstadoCambio> historial = new ArrayList<>();
      EstadoCambio estadoCambio = new EstadoCambio(estado, Instant.now(), pedido.getUsuario());
      historial.add(estadoCambio);
      pedido.setHistorialEstado(historial);
   }

   private void agregarEstadoCambio(Pedido pedido, Estado nuevoEstado, String usuario) {
      pedido.addEstadoCambio(nuevoEstado, usuario);
   }

   private void actualizarCamposPermitidos(Pedido pedidoExistente, Pedido pedido) {
      pedidoExistente.setObservaciones(pedido.getObservaciones());
      pedidoExistente.setDetalle(pedido.getDetalle());
      pedidoExistente.setObra(pedido.getObra());
   }

   private int cuentaPedidos(){
      return Math.random()+1;
   }

}