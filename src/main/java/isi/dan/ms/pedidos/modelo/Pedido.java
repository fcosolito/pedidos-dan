package isi.dan.ms.pedidos.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "pedidos")
public class Pedido {
   public static final String SEQUENCE_NAME = "pedidos_sequence";

   @Id
   private String id;
   private Instant fecha;
   private Integer numeroPedido;
   private String usuario;
   private String observaciones;

   private Cliente cliente;
   private BigDecimal total;

   private Obra obra;

   private Estado estado;
   private List<EstadoCambio> historialEstado = new ArrayList<>();

   @Field("detalle")
   private List<DetallePedido> detalle;

   // Método para añadir un cambio de estado al historial
   public void addEstadoCambio(Estado nuevoEstado, String usuario) {
      EstadoCambio estadoCambio = new EstadoCambio(nuevoEstado, Instant.now(), usuario);
      historialEstado.add(estadoCambio);
   }
}
