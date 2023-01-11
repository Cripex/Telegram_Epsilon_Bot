package telegram.epsilon_robot.telegramBot;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import telegram.epsilon_robot.telegramBot.botStates.BotStateController;
import telegram.epsilon_robot.telegramBot.botStates.State;

import java.util.Map;


public class MessageHandlerTest extends TestCase {

    private static Update update1;
    private static Update update2;
    private static Update update3;
    private static Update update4;

    @Before
    public void setUp() throws Exception {

        User telegramUser = new User();
        telegramUser.setUserName("IvanIvanov");
        telegramUser.setFirstName("Ivan");
        telegramUser.setLastName("Ivanov");

        /*
        Update 1:
        chatId = 1;
        requestType = text ("/start");
         */
        Chat chat1 = new Chat();
        chat1.setId(Long.parseLong("1"));

        Message message1 = new Message();
        message1.setText("/start");
        message1.setChat(chat1);
        message1.setFrom(telegramUser);

        update1 = new Update();
        update1.setMessage(message1);

        /*
        Update 2:
        chatId = 2;
        requestType = callbackQuery (State.MAIN_MENU);
         */
        Chat chat2 = new Chat();
        chat2.setId(Long.parseLong("2"));

        Message message2 = new Message();
        message2.setChat(chat2);
        message2.setFrom(telegramUser);

        CallbackQuery callbackQuery2 = new CallbackQuery();
        callbackQuery2.setData(State.MAIN_MENU.name());
        callbackQuery2.setMessage(message2);

        update2 = new Update();
        update2.setCallbackQuery(callbackQuery2);

        /*
        Update 3:
        chatId = 3;
        requestType = callbackQuery (State.MY_ACCOUNT);
         */
        Chat chat3 = new Chat();
        chat3.setId(Long.parseLong("3"));

        Message message3 = new Message();
        message3.setChat(chat3);
        message3.setFrom(telegramUser);

        CallbackQuery callbackQuery3 = new CallbackQuery();
        callbackQuery3.setData(State.MY_ACCOUNT.name());
        callbackQuery3.setMessage(message3);

        update3 = new Update();
        update3.setCallbackQuery(callbackQuery3);

        /*
        Update 4:
        chatId = 4;
        requestType = text ("/star");
         */
        Chat chat4 = new Chat();
        chat4.setId(Long.parseLong("4"));

        Message message4 = new Message();
        message4.setText("/st");
        message4.setChat(chat4);
        message4.setFrom(telegramUser);

        update4 = new Update();
        update4.setMessage(message4);
    }


    @Test
    public void testWriteStateForRequest() {

        State actualState1 = MessageHandler.changeStateAsRequested(update1);
        State actualState2 = MessageHandler.changeStateAsRequested(update2);
        State actualState3 = MessageHandler.changeStateAsRequested(update3);
        State actualState4 = MessageHandler.changeStateAsRequested(update4);

        State expectedState1 = State.MAIN_MENU;
        State expectedState2 = State.MAIN_MENU;
        State expectedState3 = State.MAIN_MENU;
        State expectedState4 = State.ERROR;

        assertEquals(expectedState1, actualState1);
        assertEquals(expectedState2, actualState2);
        assertEquals(expectedState3, actualState3);
        assertEquals(expectedState4, actualState4);
    }


    @Test
    public void testCreateSendMessage() {

        BotStateController botStateController = BotStateController.getInstance();

        SendMessage expectedSendMessage = new SendMessage();
        expectedSendMessage.enableMarkdown(true);

        //===========================================
        expectedSendMessage.setChatId("1");
        expectedSendMessage.setText("Главное меню:");
        Map<State, String> map = botStateController
                .getHeirsStateAndJumpButtonText(State.MAIN_MENU);
        expectedSendMessage.setReplyMarkup(ViewController
                .createInlineKeyboardMarkup(map, false, false));

        SendMessage actualSendMessage1 = MessageHandler
                .createSendMessage(update1, State.MAIN_MENU);

        assertEquals(expectedSendMessage, actualSendMessage1);

        //===========================================
        expectedSendMessage.disableWebPagePreview();
        expectedSendMessage.setChatId("3");
        expectedSendMessage.setText("У Вас еще нет созданного аккаунта");
        map.clear();
        map.put(State.ACCOUNT_CREATED_RECENTLY, "Создать аккаунт");
        expectedSendMessage.setReplyMarkup(ViewController
                .createInlineKeyboardMarkup(map, false, true));

        SendMessage actualMessage2 = MessageHandler
                .createSendMessage(update3, State.MY_ACCOUNT);

        assertEquals(expectedSendMessage, actualMessage2);
    }
}