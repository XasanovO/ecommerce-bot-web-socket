package org.example.botwebsocket.services;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.weaver.ast.Or;
import org.example.botwebsocket.bot.component.TelegramBot;
import org.example.botwebsocket.entity.Order;
import org.example.botwebsocket.entity.enums.OrderStatus;
import org.example.botwebsocket.repo.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TelegramBot telegramBot;

    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(orderRepository.findAll());
    }


    @SneakyThrows
    public Order acceptOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId).get();

        if (order.getOrderStatus().equals(OrderStatus.CREATED)) {
            order.setOrderStatus(OrderStatus.IN_PROGRESS);
        } else {
            return null;
        }


        telegramBot.execute(
                new SendMessage(
                        order.getUser().getChatId().toString(),
                        "Your order : " + orderId + " -> In progress"
                )
        );

        return orderRepository.save(order);
    }

    @SneakyThrows
    public Order completeOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId).get();

        if (order.getOrderStatus() == OrderStatus.IN_PROGRESS) {
            order.setOrderStatus(OrderStatus.COMPLETED);
        } else {
            return null;
        }

        telegramBot.execute(
                new SendMessage(
                        order.getUser().getChatId().toString(),
                        "Your order : " + orderId + " -> Completed"
                )
        );

        return orderRepository.save(order);
    }
}
