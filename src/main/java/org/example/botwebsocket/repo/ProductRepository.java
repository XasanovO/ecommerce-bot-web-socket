package org.example.botwebsocket.repo;

import org.example.botwebsocket.entity.Category;
import org.example.botwebsocket.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(Category category);

    Product findByName(String name);

}