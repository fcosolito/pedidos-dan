package isi.dan.ms.pedidos.dao;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import isi.dan.ms.pedidos.modelo.Pedido;

@Repository
public interface PedidoRepository extends MongoRepository<Pedido, String> {

   List<Pedido> findByClienteId(Integer clienteId);

}
