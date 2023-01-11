package telegram.epsilon_robot.telegramBot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.epsilon_robot.telegramBot.botStates.State;

public class TelegramBotController extends TelegramLongPollingBot {


    private static TelegramBotsApi telegramBotsApi = null;

    //TODO: Вписать данные для подключения к боту
    private static final String BOT_USERNAME = "";
    private static final String BOT_TOKEN = "";



    public static void startTelegramBot() {

        if(telegramBotsApi == null) {
            try {
                telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBotsApi.registerBot(new TelegramBotController());

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("The class has already been created: TelegramBotController");
        }
    }


    private TelegramBotController() {}

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }


    @Override
    public void onUpdateReceived(Update update) {

        //Изменение состояния бота в зависимости от пришедшего запроса. Метод возращает итоговое состояние.
        State state = MessageHandler.changeStateAsRequested(update);
        if(State.ERROR.equals(state)) return;
        //Создание и отправка ответного сообщения, исходя из состояния бота.
        SendMessage sendMessage = MessageHandler.createSendMessage(update, state);

        if(update.hasCallbackQuery()) {
            System.out.println(update.getCallbackQuery().getMessage().getFrom());
        }
        else if (update.hasMessage()) {
            System.out.println(update.getMessage().getFrom());
        }

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }
}
