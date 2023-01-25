package pro.sky.telegrambot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final String REGEX_BOT_MESSAGE = "([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationTask schedule(Long chatId, NotificationTask task) {
        logger.info("Processing schedule ");
        task.setChatId(chatId);
        NotificationTask storedTask = notificationRepository.save(task);
        return storedTask;
    }

    public Optional<NotificationTask> parse(String notificationBotMessage) {
        logger.info("Processing parse ");
        Pattern pattern = Pattern.compile(REGEX_BOT_MESSAGE);
        Matcher matcher = pattern.matcher(notificationBotMessage);

        NotificationTask result = null;

        try {
            if (matcher.find()) {
                LocalDateTime notificationDateTime = LocalDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);
                String notification = matcher.group(3);
                result = new NotificationTask(notification, notificationDateTime);
            }

        } catch (RuntimeException e) {
            logger.error("Ошибка парсинга " + notificationBotMessage, e);
        }

        return Optional.ofNullable(result);
    }

    public void notifyAllScheduledTasks(Consumer<NotificationTask> notifier) {
        logger.info("Processing notifyAllScheduledTasks ");
        Collection<NotificationTask> notifications = notificationRepository.getScheduledNotifications();
        for (NotificationTask task : notifications) {
            notifier.accept(task);
            task.markStatus();
        }
        notificationRepository.saveAll(notifications);
    }
}
