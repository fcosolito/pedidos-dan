package isi.dan.ms.pedidos.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import isi.dan.ms.pedidos.modelo.Cliente;

@FeignClient(name = "ms-clientes")
public interface ClienteFeignClient {

   @PostMapping("api/clientes")
   Cliente guardarCliente(@RequestBody Cliente cliente);

   @GetMapping("api/clientes/{id}")
   Cliente getCliente(@PathVariable("id") Integer id);

   @GetMapping("/api/clientes/{id}/verificar-saldo")
   Boolean verificarSaldo(@PathVariable("id") Integer id, @RequestParam("montoTotal") double montoTotal);
}
