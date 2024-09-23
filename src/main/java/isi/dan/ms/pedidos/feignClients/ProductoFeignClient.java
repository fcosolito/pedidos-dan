package isi.dan.ms.pedidos.feignClients;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import isi.dan.ms.pedidos.modelo.Producto;

@FeignClient(name = "ms-productos")
public interface ProductoFeignClient {

   @PostMapping("/api/productos")
   Producto agregarProducto(@RequestBody Producto producto);

   @GetMapping("/producto/pedido/{id}")
   List<Producto> getProductos(@PathVariable("id") String id);

   @PostMapping("/api/productos/{id}/verificar-stock")
   Map<String, Boolean> verificarStock(@PathVariable("id") Long id, @RequestBody Map<String, Integer> cantidad);

   @PostMapping("/api/productos/{id}/update-stock")
   void actualizarStock(@PathVariable("id") Long id, @RequestBody Map<String, Integer> cantidad);

   @GetMapping("/api/productos/ids")
   List<Producto> getProductosByIds(@RequestParam("ids") List<Long> ids);
}
