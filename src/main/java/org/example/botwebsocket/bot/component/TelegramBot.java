package org.example.botwebsocket.bot.component;


import org.example.botwebsocket.bot.service.BotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotService botService;

    public TelegramBot(TelegramBotsApi telegramBotsApi, BotService botService) throws TelegramApiException {
        super("7118887199:AAFho0YQzAPowKGG8XnRmQVe8ZqwuVWgnRE");
        this.botService = botService;
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        botService.handleUpdate(update, this);
    }

    @Override
    public String getBotUsername() {
        return "https://t.me/Xasanovs_bot";
    }
}
