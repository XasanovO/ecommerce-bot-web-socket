package org.example.botwebsocket.repo;

import org.example.botwebsocket.entity.Basket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasketRepository extends JpaRepository<Basket, Integer> {
}