package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final static Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final String START_CMD = "/start";

    private static final String GREETING_TEXT = "Приветствую, пользователь!";

    private static final String INVALID_ID_NOTIFY_OR_CMD = "Ошибка уведомления или команды";

    @Autowired
    private final TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            Message message = update.message();
            if (message.text().startsWith(START_CMD)) {
                logger.info(START_CMD + " команда была получена");
                sendMessage(extractChatId(message), GREETING_TEXT);
            } else {
                notificationService.parse(message.text()).ifPresentOrElse(
                        task -> scheduleNotification(extractChatId(message), task),
                        () -> sendMessage(extractChatId(message), INVALID_ID_NOTIFY_OR_CMD)
                );
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void sendMessage(Long chatId, String messageText) {
        SendMessage sendMessage = new SendMessage(chatId, messageText);
        telegramBot.execute(sendMessage);
    }

    public Long extractChatId(Message message) {
        return message.chat().id();
    }

}
