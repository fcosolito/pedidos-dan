package isi.dan.ms.pedidos.modelo;

import lombok.Data;

@Data
public class EstadoCambioRequest {
   private Estado nuevoEstado;
   private String usuarioCambio;
}
