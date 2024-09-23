package isi.dan.ms.pedidos.modelo;

import java.time.Instant;

import lombok.Data;

@Data
public class EstadoCambio {
   private Estado estado;
   private Instant fechaCambio;
   private String usuarioCambio;

   public EstadoCambio(Estado estado, Instant fechaCambio, String usuarioCambio) {
      this.estado = estado;
      this.fechaCambio = fechaCambio;
      this.usuarioCambio = usuarioCambio;
   }
}
