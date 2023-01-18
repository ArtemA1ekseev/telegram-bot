package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.service.NotificationService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final String START_CMD = "/start";

    private static final String GREETING_TEXT = "Приветствую, пользователь!";

    private static final String INVALID_ID_NOTIFY_OR_CMD = "Ошибка уведомления или команды";

    @Autowired
    private final TelegramBot telegramBot;

    private final NotificationService notificationService;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationService notificationService) {
        this.telegramBot = telegramBot;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // Process your updates here
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

    public void sendMessage(NotificationTask task) {
        sendMessage(task.getChatId(), task.getNotificationMessage());
    }

    public Long extractChatId(Message message) {
        return message.chat().id();
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void notifyScheduledTasks() {
        notificationService.notifyAllScheduledTasks(this::sendMessage);
    }

    private void scheduleNotification(Long chatId, NotificationTask task) {
        notificationService.schedule(chatId, task);
        sendMessage(chatId, "Уведомление успешно добавлено");
    }
}
