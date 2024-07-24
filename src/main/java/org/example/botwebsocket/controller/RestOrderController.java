package org.example.botwebsocket.controller;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.botwebsocket.entity.Order;
import org.example.botwebsocket.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class RestOrderController {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return orderService.getAll();
    }

    @SneakyThrows
    @PostMapping("/{orderId}/inProgress")
    public ResponseEntity<?> acceptOrder(@PathVariable Integer orderId) {
        Order order = orderService.acceptOrder(orderId);
        if (order != null) {
            messagingTemplate.convertAndSend("/topic/orders", order);
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.badRequest().body("can't accept order");
        }
    }


    @SneakyThrows
    @PostMapping("/{orderId}/completed")
    public ResponseEntity<?> completeOrder(@PathVariable Integer orderId) {

        Order order = orderService.completeOrder(orderId);

        if (order != null) {
            messagingTemplate.convertAndSend("/topic/orders", order);
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.badRequest().body("can't complete order");
        }
    }

}
