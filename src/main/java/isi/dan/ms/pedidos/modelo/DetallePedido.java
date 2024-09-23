package isi.dan.ms.pedidos.modelo;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
public class DetallePedido {
   @Field("producto")
   private Producto producto;
   private Integer cantidad;
   private BigDecimal precioUnitario;
   private BigDecimal descuento;
   private BigDecimal precioFinal;

   public DetallePedido(Producto producto, Integer cantidad) {
      this.producto = producto;
      this.cantidad = cantidad;
   }

   public DetallePedido() {
   };
}
