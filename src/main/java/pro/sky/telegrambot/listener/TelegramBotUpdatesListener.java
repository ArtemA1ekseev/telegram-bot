package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final String GREETING_TEXT = "Приветствую, пользователь!" +
            "\nБот умеет принимать от пользователя сообщения в формате: " +
            "\n01.01.2022 20:00 Сделать домашнюю работу. " +
            "\nи присылать пользователю сообщение в 20:00 1 января 2022 года с текстом “Сделать домашнюю работу”.";

    private static final String INVALID_ID_NOTIFY_OR_CMD = "Ошибка уведомления или команды";

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

    /*
     * Redefined "process" method, receiving a message from the user
     */
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

    /*
     * Method for sending a message
     */
    public void sendMessage(Long chatId, String messageText) {
        SendMessage sendMessage = new SendMessage(chatId, messageText);
        telegramBot.execute(sendMessage);
    }

    /*
     * Method for sending a message
     */
    public void sendMessage(NotificationTask task) {
        sendMessage(task.getChatId(), task.getNotificationMessage());
    }

    /*
     * Method for getting the chat id
     */
    public Long extractChatId(Message message) {
        return message.chat().id();
    }

    /*
     * A method that notifies the user about scheduled tasks
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void notifyScheduledTasks() {
        notificationService.notifyAllScheduledTasks(this::sendMessage);
    }

    /*
     * Method notifying about the success of the added task
     */
    private void scheduleNotification(Long chatId, NotificationTask task) {
        notificationService.schedule(chatId, task);
        sendMessage(chatId, "Уведомление успешно добавлено, ожидайте оповещение!");
    }
}
