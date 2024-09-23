package isi.dan.ms.pedidos.modelo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Obra {

   private Integer id;

   private String calle;
   private String ciudad;
   private String provincia;
   private String pais;
   private String altura;
   private Boolean esRemodelacion;
   private float lat;
   private float lng;
   private BigDecimal presupuesto;
   private EstadoObra estado;
}
