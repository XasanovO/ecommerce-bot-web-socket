package org.example.botwebsocket.component;


import lombok.RequiredArgsConstructor;
import org.example.botwebsocket.entity.Category;
import org.example.botwebsocket.entity.Product;
import org.example.botwebsocket.repo.CategoryRepository;
import org.example.botwebsocket.repo.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddl;

    @Override
    public void run(String... args) throws Exception {
        if (ddl.equals("create")) {

            Category category = new Category(1, "Yeguliklar");
            Category category1 = new Category(2, "Ichimlikar");
            Category category2 = new Category(3, "Kiyguliklar");

            categoryRepository.saveAll(List.of(category, category1, category2));

            Product product = new Product(1, "Apple", 15000, "/static/apple.jpg", category);
            Product product1 = new Product(2, "Banana", 15000, "/static/banana.jpg", category);
            Product product2 = new Product(3, "Fanta", 7000, "/static/Fanta.jpg", category1);
            Product product3 = new Product(4, "Cola", 7000, "/static/cola.jpg", category1);
            Product product4 = new Product(5, "Jeans", 200000, "/static/jeans.jpg", category2);
            Product product5 = new Product(6, "Shirt", 150000, "/static/shirt.jpg", category2);

            productRepository.saveAll(List.of(product, product1, product2, product3, product4, product5));

        }
    }
}
