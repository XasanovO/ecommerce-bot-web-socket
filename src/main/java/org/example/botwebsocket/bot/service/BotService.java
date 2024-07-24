package org.example.botwebsocket.bot.service;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.botwebsocket.bot.botUtils.BotConstants;
import org.example.botwebsocket.bot.component.TelegramBot;
import org.example.botwebsocket.entity.*;
import org.example.botwebsocket.entity.enums.OrderStatus;
import org.example.botwebsocket.entity.enums.UserStatus;
import org.example.botwebsocket.repo.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BotService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BasketRepository basketRepository;
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @SneakyThrows
    public void handleUpdate(Update update, TelegramBot telegramBot) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            User user = getCurrentUser(chatId);
            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start")) {
                    acceptStartAskCategory(user, telegramBot);
                } else if (user.getStatus().equals(UserStatus.SELECT_CATEGORY)) {
                    Category category = categoryRepository.findByTitle(text);
                    if (category != null) {
                        acceptCategoryAskProduct(user, telegramBot, category);
                    } else if (text.equals(BotConstants.BASKET)) {
                        showBasket(user, telegramBot);
                    } else {
                        telegramBot.execute(new SendMessage(String.valueOf(user.getChatId()), "Choose correct category!"));
                    }
                } else if (user.getStatus().equals(UserStatus.SELECT_PRODUCT)) {
                    Product product = productRepository.findByName(text);
                    if (text.equals(BotConstants.BACK)) {
                        acceptStartAskCategory(user, telegramBot);
                    } else if (product != null) {
                        acceptProductNameAskAmount(product, user, telegramBot);
                    } else {
                        telegramBot.execute(new SendMessage(
                                String.valueOf(user.getChatId()),
                                "Choose correct product!"
                        ));
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long id = callbackQuery.getFrom().getId();
            User user = getCurrentUser(id);
            if (!callbackQuery.getData().isEmpty()) {
                String callbackData = callbackQuery.getData();
                if (user.getStatus().equals(UserStatus.PRODUCT_ACTION)) {
                    Category category = categoryRepository.findById(user.getCurrentCategoryId()).get();
                    if (callbackData.equals(BotConstants.INCREMENT)) {
                        updateCounter(user, telegramBot, true);
                    } else if (callbackData.equals(BotConstants.DECREMENT)) {
                        updateCounter(user, telegramBot, false);
                    } else if (callbackData.equals(BotConstants.BACK)) {
                        backSelectCategory(user, telegramBot, category);
                    } else if (callbackData.equals(BotConstants.ADD_TO_BASKET)) {
                        addToBasket(user, telegramBot, category);
                    }
                } else if (user.getStatus().equals(UserStatus.SELECT_CATEGORY)) {
                    if (callbackData.equals(BotConstants.BACK)) {
                        acceptStartAskCategory(user, telegramBot);
                    } else if (callbackData.equals(BotConstants.ORDER)) {
                        saveOrderAndOrderProducts(user, telegramBot);
                    }
                }
            }
        }
    }

    @SneakyThrows
    private void saveOrderAndOrderProducts(User user, TelegramBot telegramBot) {
        List<OrderProduct> orderProducts = new ArrayList<>();

        user.getBasket().getProductAmount().forEach(((item, amount) -> {
            Product product = productRepository.findByName(item);
            orderProducts.add(
                    OrderProduct.builder()
                            .product(product)
                            .amount(amount)
                            .build()
            );
        }));

        orderProductRepository.saveAll(orderProducts);

        Order order = orderRepository.save(
                Order.builder()
                        .orderStatus(OrderStatus.CREATED)
                        .user(user)
                        .orderProducts(orderProducts)
                        .build()
        );

        messagingTemplate.convertAndSend("/topic/orders", order);

        Basket basket = user.getBasket();
        basket.getProductAmount().clear();
        basketRepository.save(basket);

        telegramBot.execute(new SendMessage(
                user.getChatId().toString(), "order successfully"
        ));
        acceptStartAskCategory(user, telegramBot);
    }

    @SneakyThrows
    private void showBasket(User user, TelegramBot telegramBot) {
        Basket basket = user.getBasket();
        if (basket.getProductAmount().isEmpty()) {
            telegramBot.execute(new SendMessage(user.getChatId().toString(), "Basket is Empty !"));
        } else {
            SendMessage sendMessage = new SendMessage();
            StringBuilder stringBuilder = new StringBuilder("Your basket:\n");
            basket.getProductAmount().forEach((item, amount) -> {
                Product product = productRepository.findByName(item);
                double totalPrice = product.getPrice() * amount;
                stringBuilder.append(product.getName())
                        .append(" - ")
                        .append(amount)
                        .append(" x ")
                        .append(product.getPrice())
                        .append(" = ")
                        .append(totalPrice)
                        .append("\n");
            });
            sendMessage.setText(stringBuilder.toString());
            sendMessage.setReplyMarkup(generateBasketBtn());
            sendMessage.setChatId(user.getChatId());
            telegramBot.execute(sendMessage);
        }
    }

    private ReplyKeyboard generateBasketBtn() {
        InlineKeyboardButton order = new InlineKeyboardButton(BotConstants.ORDER);
        order.setCallbackData(BotConstants.ORDER);
        InlineKeyboardButton back = new InlineKeyboardButton(BotConstants.BACK);
        back.setCallbackData(BotConstants.BACK);
        return new InlineKeyboardMarkup(List.of(List.of(order), List.of(back)));
    }

    private void backSelectCategory(User user, TelegramBot telegramBot, Category category) {
        acceptCategoryAskProduct(user, telegramBot, category);
        user.setCounter(1);
        userRepository.save(user);
    }

    private void updateCounter(User user, TelegramBot telegramBot, boolean plus) {
        if (!plus) {
            if (user.getCounter() > 1) {
                user.setCounter(user.getCounter() - 1);
                editProductMessage(user, telegramBot);
                userRepository.save(user);
            }
        } else {
            user.setCounter(user.getCounter() + 1);
            editProductMessage(user, telegramBot);
            userRepository.save(user);
        }
    }

    @SneakyThrows
    private void addToBasket(User user, TelegramBot telegramBot, Category category) {
        Basket basket = user.getBasket();
        Map<String, Integer> productAmount = basket.getProductAmount();
        productAmount.put(
                productRepository.findById(user.getCurrentProductId()).get().getName(),
                user.getCounter()
        );
        basketRepository.save(basket);
        user.setCounter(1);
        telegramBot.execute(
                new SendMessage(user.getChatId().toString(), "added to basket ✅")
        );
        acceptCategoryAskProduct(user, telegramBot, category);
    }

    @SneakyThrows
    private void acceptProductNameAskAmount(Product product, User user, TelegramBot telegramBot) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(user.getChatId());
        sendPhoto.setCaption(product.getName() + " " + product.getPrice());
        InputFile photoFile = new InputFile(new File(getClass().getResource(product.getImage()).toURI()));
        sendPhoto.setPhoto(photoFile);
        sendPhoto.setReplyMarkup(generateProductActionButton(user));
        Message execute = telegramBot.execute(sendPhoto);
        user.setEditMessageId(execute.getMessageId());
        user.setStatus(UserStatus.PRODUCT_ACTION);
        user.setCurrentProductId(product.getId());
        userRepository.save(user);
    }


    private ReplyKeyboard generateProductActionButton(User user) {

        InlineKeyboardButton incrementButton = new InlineKeyboardButton("➕");
        incrementButton.setCallbackData(BotConstants.INCREMENT);

        InlineKeyboardButton decrementButton = new InlineKeyboardButton("➖");
        decrementButton.setCallbackData(BotConstants.DECREMENT);

        InlineKeyboardButton counterButton = new InlineKeyboardButton(String.valueOf(user.getCounter()));
        counterButton.setCallbackData(BotConstants.COUNTER);

        InlineKeyboardButton addToBasket = new InlineKeyboardButton(BotConstants.ADD_TO_BASKET);
        addToBasket.setCallbackData(BotConstants.ADD_TO_BASKET);

        InlineKeyboardButton backBtn = new InlineKeyboardButton(BotConstants.BACK);
        backBtn.setCallbackData(BotConstants.BACK);

        List<InlineKeyboardButton> row1 = List.of(decrementButton, counterButton, incrementButton);
        List<InlineKeyboardButton> row2 = List.of(addToBasket);
        List<InlineKeyboardButton> row3 = List.of(backBtn);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(row1, row2, row3));

        return inlineKeyboardMarkup;
    }

    @SneakyThrows
    private void editProductMessage(User user, TelegramBot telegramBot) {
        int messageId = user.getEditMessageId();
        ReplyKeyboard replyKeyboard = generateProductActionButton(user);
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(user.getChatId());
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup((InlineKeyboardMarkup) replyKeyboard);
        telegramBot.execute(editMessageReplyMarkup);
    }

    private void acceptCategoryAskProduct(User user, TelegramBot telegramBot, Category category) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Please select a Product:");
        sendMessage.setReplyMarkup(generateProductButton(category));
        user.setStatus(UserStatus.SELECT_PRODUCT);
        user.setCurrentCategoryId(category.getId());
        userRepository.save(user);
        try {
            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboard generateProductButton(Category category) {
        List<Product> products = productRepository.findByCategory(category);

        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        products.forEach(product -> {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(product.getName()));
            keyboard.add(row);
        });

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(BotConstants.BACK));
        keyboard.add(row);

        replyMarkup.setKeyboard(keyboard);
        return replyMarkup;
    }

    @SneakyThrows
    private void acceptStartAskCategory(User user, TelegramBot telegramBot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Please select a category:");
        sendMessage.setReplyMarkup(generateCategoryButton());
        user.setStatus(UserStatus.SELECT_CATEGORY);
        userRepository.save(user);
        try {
            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboard generateCategoryButton() {
        List<Category> categories = categoryRepository.findAll();

        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        categories.forEach(category -> {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(category.getTitle()));
            keyboard.add(row);
        });

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(BotConstants.BASKET));
        keyboard.add(row);

        replyMarkup.setKeyboard(keyboard);
        return replyMarkup;
    }

    private User getCurrentUser(Long chatId) {
        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            Basket userBasket = basketRepository.save(new Basket());
            return userRepository.save(
                    User.builder()
                            .chatId(chatId)
                            .counter(1)
                            .status(UserStatus.SELECT_CATEGORY)
                            .basket(userBasket)
                            .build()
            );
        } else {
            return user;
        }
    }


}
