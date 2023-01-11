package telegram.epsilon_robot.telegramBot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import telegram.epsilon_robot.database.DatabaseController;
import telegram.epsilon_robot.database.UserEntityController;
import telegram.epsilon_robot.database.database_entities.UserEntity;
import telegram.epsilon_robot.telegramBot.botStates.BotStateController;
import telegram.epsilon_robot.telegramBot.botStates.State;
import telegram.epsilon_robot.tokenDataAPI.CoinDataController;
import telegram.epsilon_robot.tokenDataAPI.CoinPOJO;

import java.util.*;

/*
MessageHandler - главный движок Telegram Bot.
Класс реализует связь между всеми блоками программы.

Главные методы:
    - changeStateAsRequested(Update update) - метод обработки входящих сообщений.
    Главная задача - изменение состояния бота во всей программе. Принимает на вход
    объект класса Update (Telegram API), который содержит всю информацию о пользователе
    и входящем контенте. Возращает актуальное состояние бота (State), соответствующее новому состоянию
    в базе данных. Содержит вспомогательные методы для уменьшения объема текста

    - createSendMessage(Update update, State state) - метод создания ответного сообщения.
    Главная задача - сконфигурировать ответ в соответствии с состоянием и входящим контентом.
    Принимает на вход обновленное состояние (State) и объект класса Update (Telegram API) с данными
    о пользователе. Возвращает объект класса ответного сообщения SendMessage (Telegram API)
 */
class MessageHandler {

    private static final BotStateController botStateController = BotStateController.getInstance();      //контроллер состояний бота
    private static final DatabaseController databaseController = UserEntityController.getInstance();    //контроллер подключения к БД
    private static final CoinDataController coinDataController = CoinDataController.getInstance();      //контроллер данных о коинах

    //Специальные объекты для работы поиска коинов из состояния State.COIN_RESEARCH
    private static Map<String, String> jumpButtonsStateAndTextMap = new HashMap<>();
    private static Integer coinId = null;

    /*
    Данный метод изменяет состояние бота в соответствии с пришедшим запросом.
    - объект класса Update хранит значения полученного сообщения и Id чата;
    - полученное сообщение проверяется и интерпретируется в состояние бота;
    - новое состояние записывается в БД и локальный HashMap с объектами UserEntity
     */
    public static State changeStateAsRequested(Update update) {

        State state = null;
        String requestState = null;
        Message message = null;

        //Запись объекта message и поля requestState (Интерпритация состояния)
        //Если пришел текст
        if (update.hasMessage() && update.getMessage().hasText()) {

            message = update.getMessage();
            String requestMessage = message.getText();
            //Вызов метода обработки текстовых запросов
            requestState = processTextRequest(requestMessage, message).name();
            if(State.ERROR.name().equals(requestState)) { return State.ERROR; }
        }
        //Если пришел CallbackQuery
        else if (update.hasCallbackQuery()) {
            requestState = update.getCallbackQuery().getData();
            message = update.getCallbackQuery().getMessage();

            if(isThisTheCoinFoundSuccessfullyState(requestState)) {
                coinId = Integer.parseInt(requestState.substring(State
                        .COIN_FOUND_SUCCESSFULLY.name().length()));
                requestState = State.COIN_FOUND_SUCCESSFULLY.name();
            }

        }
        else return State.ERROR;


        //Проверка наличия состояния в enum
        boolean isStateValue = false;

        for (State comparedState : State.values()) {
            if (comparedState.name().equals(requestState)) {
                isStateValue = true;
                break;
            }
        }

        //Метод возвращает состояние ошибки, если полученного состояния нет в enum
        if (!isStateValue) { return State.ERROR; }

        //Получение текущего состояния по id. Если сущности с таким id не существует, создается новый
        Long chatId = message.getChatId();
        UserEntity currentUser = databaseController.getUserEntity(chatId);
        if (currentUser == null) {

            currentUser = new UserEntity();
            User telegramUser = message.getFrom();

            currentUser.setChatId(chatId);
            currentUser.setBotState(State.MAIN_MENU.name());
            currentUser.setTelegramUsername(telegramUser.getUserName());
            currentUser.setUserFirstName(telegramUser.getFirstName());
            currentUser.setUserLastName(telegramUser.getLastName());
            currentUser.setHasAccountField(false);

//                currentUser.setCoinIdList(null);     //У только что созданного userEntity не может быть списка коинов
            databaseController.saveOrUpdateUserEntity(currentUser);
            return State.MAIN_MENU;
        }
        else {
            String currentState = currentUser.getBotState();

            //Получение нового состояния в соответствии с полученным сообщением и прошлым состоянием
            int currentChainId = botStateController.getCurrentChainIdByState(currentState);
            int newChainId = botStateController.getNewChainId(requestState, currentChainId);
            state = botStateController.getEnumStateByChainId(newChainId);

            //Изменение состояния бота в БД и локальном HashMap
            databaseController.updateStateForUserEntity(chatId, state.name());
            return state;
        }
    }



    /*
    Данный метод создает ответ на основании полученного состояния бота.
         */
    public static SendMessage createSendMessage(Update update, State state) {

        if (state == null) {
            throw new NullPointerException();
        }

        boolean isInlineKeyboardOn = true;      //Активация кнопок под сообщением
        boolean isBackButtonOn = true;          //Активация кнопки "Назад"
        String responseMessageText = null;      //Текст ответного сообщения
        Map<State, String> keyboardStateAndButtonTextMap = null;     //Список кнопок
        InlineKeyboardMarkup inlineKeyboardMarkup = null;            //inline клавиатура

        //Состояние BACK отсутствует, так как просто возвращает chainId на предыдущий
        if (state == State.ERROR) {
            responseMessageText = "Error! Please try again.";
        }

        //chain_id=0
        else if (state == State.MAIN_MENU) {
            responseMessageText = "Главное меню:";
            keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(state);
            isBackButtonOn = false;
        }

        //chain_id=1
        else if (state == State.GET_SELECTED_COIN_COST) {
            responseMessageText = "Доступная информация о криптовалютах:";
            keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(state);
        }

        //chain_id=2 || 20
        else if (state == State.MY_ACCOUNT) {
            UserEntity userEntity = databaseController
                    .getUserEntity(update.getCallbackQuery().getMessage().getChatId());
            if(userEntity != null && userEntity.hasAccount()) {
                responseMessageText = "Мой аккаунт:";
                keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(state);
            }
            else {
                responseMessageText = "У Вас еще нет созданного аккаунта";
                keyboardStateAndButtonTextMap = new HashMap<>();
                keyboardStateAndButtonTextMap.put(State.ACCOUNT_CREATED_RECENTLY, "Создать аккаунт");
            }
        }

        //chain_id=3
        else if (state == State.USER_MANUAL) {
            responseMessageText = "Руководство по работе с ботом:";
            keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(state);
        }

        //chain_id=11
        else if (state == State.COIN_SEARCH) {
            responseMessageText = "Чтобы найти монету введите полностью или частично " +
                    "её название или аббревиатуру. Используйте английскую раскладку";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }

        //chain_id=12
        else if (state == State.TOP_30) {
            responseMessageText = "Топ 30 коинов по капитализации:\n";
            List<CoinPOJO> coinPOJOList = new ArrayList<>();
            coinPOJOList.addAll(coinDataController
                    .getCurrentCoinPOJOMap().values());
            coinPOJOList.sort(new Comparator<CoinPOJO>() {
                @Override
                public int compare(CoinPOJO o1, CoinPOJO o2) {
                    double o1MarketCap = o1.getMarketCap();
                    double o2MarketCap = o2.getMarketCap();
                    if(o1MarketCap < o2MarketCap) return 1;
                    else if(o1MarketCap > o2MarketCap) return -1;
                    return 0;
                }
            });
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < 30; i++) {
                CoinPOJO coinPOJO = coinPOJOList.get(i);
                builder.append("\n" + String.valueOf(i + 1) + ". " + coinPOJO.getName()
                        + " ( " + coinPOJO.getMarketCap() + " USD )");
            }
            responseMessageText += builder.toString();
            keyboardStateAndButtonTextMap = new HashMap<>();
        }

        //chain_id=13
        else if (state == State.TOP_10_ROSE_IN_PRICE_IN_24H) {
            responseMessageText = "Топ 10 выросших в цене за 24 часа";
            List<CoinPOJO> coinPOJOList = new ArrayList<>();
            coinPOJOList.addAll(coinDataController
                    .getCurrentCoinPOJOMap().values());
            coinPOJOList.sort(new Comparator<CoinPOJO>() {
                @Override
                public int compare(CoinPOJO o1, CoinPOJO o2) {
                    double o1MarketCap = o1.getPercentChange24h();
                    double o2MarketCap = o2.getPercentChange24h();
                    if(o1MarketCap < o2MarketCap) return 1;
                    else if(o1MarketCap > o2MarketCap) return -1;
                    return 0;
                }
            });
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < 10; i++) {
                CoinPOJO coinPOJO = coinPOJOList.get(i);
                builder.append("\n" + String.valueOf(i + 1) + ". " + coinPOJO.getName()
                        + " ( " + coinPOJO.getPercentChange24h() + " % )");
            }
            responseMessageText += builder.toString();
            keyboardStateAndButtonTextMap = new HashMap<>();
        }

        //chain_id=14
        else if (state == State.TOP_10_FELL_IN_PRICE_IN_24H) {
            responseMessageText = "Топ 10 упавших в цене за 24 часа";
            List<CoinPOJO> coinPOJOList = new ArrayList<>();
            coinPOJOList.addAll(coinDataController
                    .getCurrentCoinPOJOMap().values());
            coinPOJOList.sort(new Comparator<CoinPOJO>() {
                @Override
                public int compare(CoinPOJO o1, CoinPOJO o2) {
                    double o1MarketCap = o1.getPercentChange24h();
                    double o2MarketCap = o2.getPercentChange24h();
                    if(o1MarketCap > o2MarketCap) return 1;
                    else if(o1MarketCap < o2MarketCap) return -1;
                    return 0;
                }
            });
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < 10; i++) {
                CoinPOJO coinPOJO = coinPOJOList.get(i);
                builder.append("\n" + String.valueOf(i + 1) + ". " + coinPOJO.getName()
                        + " ( " + coinPOJO.getPercentChange24h() + " % )");
            }
            responseMessageText += builder.toString();
            keyboardStateAndButtonTextMap = new HashMap<>();
        }

        //chain_id=20
        else if(state == State.ACCOUNT_CREATED_RECENTLY) {
            UserEntity userEntity = databaseController
                    .getUserEntity(update.getCallbackQuery().getMessage().getChatId());
            userEntity.setHasAccountField(true);
            databaseController.saveOrUpdateUserEntity(userEntity);
            responseMessageText = "Мой аккаунт:";
            keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(State.MY_ACCOUNT);
        }

        //chain_id=21
        else if (state == State.ADD_TO_LIST) {
            responseMessageText = "Добавить в список";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=22
        else if (state == State.DELETE_FROM_LIST) {
            responseMessageText = "Удалить из списка";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=23
        else if (state == State.ANABLE_ALERTS) {
            responseMessageText = "Включить оповещение";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=24
        else if (state == State.COIN_STATISTICS) {
            responseMessageText = "Статистика коина";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=25
        else if (state == State.DELETE_ACCOUNT) {
            responseMessageText = "Вы уверены, что хотите удалить аккаунт?";
            keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(state);
        }

        //chain_id=31
        else if (state == State.HOW_TO_FIND_OUT_THE_VALUE_OF_A_COIN) {
            responseMessageText = "Как узнать стоимость";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=32
        else if (state == State.WHY_DO_I_NEED_THE_ACCOUNT) {
            responseMessageText = "Зачем нужен аккаунт";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=33
        else if (state == State.ALERT_OF_COIN_RISING_AND_FALLING) {
            responseMessageText = "Оповещения о росте/падении цен";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=34
        else if (state == State.HOW_MUCH_DOES_IT_COST) {
            responseMessageText = "Сколько это стоит";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=35
        else if (state == State.NEWS_AND_RECOMMENDATIONS) {
            responseMessageText = "Новости и рекомендации";
            keyboardStateAndButtonTextMap = new HashMap<>();
        }
        //chain_id=110
        else if (state == State.COIN_RESEARCH) {
            responseMessageText = "Вот список вариантов, похожих на ваш запрос. " +
                    "Нажмите на подходящий вам.\nЕсли среди вариантов нет подходящего, " +
                    "попробуйте ввести другую комбинацию английских символов.";
            /*
            Все предложенные варианты будут вызывать одно состояние State.COIN_FOUND_SUCCESSFULLY,
            поэтому создание inline клавиатуры вынесено в отдельный метод, принимающий только
            значения текстового поля кнопок
             */
            inlineKeyboardMarkup = ViewController
                    .createInlineKeyboardMarkupForStateCOIN_RESEARCH(jumpButtonsStateAndTextMap);
            jumpButtonsStateAndTextMap.clear();
        }

        //chain_id=111
        else if (state == State.COIN_FOUND_SUCCESSFULLY) {
            responseMessageText = "Информация по монете:\n";
            responseMessageText += coinDataController
                    .getCurrentCoinPOJOMap().get(coinId).getCoinInfo();
            keyboardStateAndButtonTextMap = new HashMap<>();
        }

        //chain_id=251
        else if (state == State.ACCOUNT_DELETED) {
            responseMessageText = "Ваш аккаунт успешно удален!\n\n" + "Главное меню:";
            UserEntity userEntity = databaseController
                    .getUserEntity(update.getCallbackQuery().getMessage().getChatId());
            userEntity.setHasAccountField(false);
            userEntity.setBotState(State.MAIN_MENU.name());
            databaseController.saveOrUpdateUserEntity(userEntity);
            keyboardStateAndButtonTextMap = botStateController.getHeirsStateAndJumpButtonText(State.MAIN_MENU);
            isBackButtonOn = false;
        }

        else { throw new NullPointerException(); }


        //Создание Inline клавиатуры
        if(inlineKeyboardMarkup == null && isInlineKeyboardOn) {
            inlineKeyboardMarkup = ViewController.createInlineKeyboardMarkup(
                    keyboardStateAndButtonTextMap,
                    false,
                    isBackButtonOn);
        }

        SendMessage message = new SendMessage();

        String chatId = null;
        if(update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            message.disableWebPagePreview();
        }
        else if(update.hasMessage()) {
            chatId = update.getMessage().getChatId().toString();
        }

        message.enableMarkdown(true);
        message.setChatId(chatId);
        message.setText(responseMessageText);

        if(inlineKeyboardMarkup != null) {
            message.setReplyMarkup(inlineKeyboardMarkup);
        }
        return message;
    }



    /*
    Блок кода для метода changeStateAsRequested().
    Задача: обработка входящих только текстовых сообщений
     */
    private static State processTextRequest(String requestText, Message message) {

        //Начало работы с ботом (инициализация)
        if ("/start".equals(requestText)) {
            return State.NEW_USER;
        }

        //Получение текущего состояния бота
        Long chatId = message.getChatId();
        UserEntity currentUser = databaseController.getUserEntity(chatId);
        if(currentUser == null) { return State.ERROR; }
        String currentState = currentUser.getBotState();

        //Для состояний chain_id=11 (поиск определенного коина) и chain_id=110 (повторный поиск)
        if(State.COIN_SEARCH.name().equals(currentState) ||
                State.COIN_RESEARCH.name().equals(currentState)) {

            requestText = requestText.toLowerCase().trim();
            jumpButtonsStateAndTextMap.clear();

            //Поиск совпадений для вывода возможных вариантов
            List<CoinPOJO> coinPOJOList = new ArrayList<>();
            coinPOJOList.addAll(coinDataController.getCurrentCoinPOJOMap().values());

            //Создания массивов последовательностей букв из запрашиваемого слова по 3 буквы
            List<CharSequence> threeCharsArray = null;
            if(requestText.length() > 3) {
                threeCharsArray = new ArrayList<>();
                for(int i = 0; i < requestText.length() - 2; i++) {
                    threeCharsArray.add(requestText.subSequence(i, i + 2));
                }
            }

            for(CoinPOJO coinPOJO : coinPOJOList) {

                String coinName = coinPOJO.getName().toLowerCase();
                String coinSymbol = coinPOJO.getSymbol().toLowerCase();

                //Слово совпадает подностью
                CharSequence charSequence = requestText.subSequence(0, requestText.length());
                if(requestText.equals(coinName) || requestText.equals(coinSymbol)
                        || coinName.contains(charSequence) || coinSymbol.contains(charSequence)) {
                    String arrayLine = coinPOJO.getName() + " (" + coinPOJO.getSymbol() + ")";
                    jumpButtonsStateAndTextMap.put(
                            State.COIN_FOUND_SUCCESSFULLY.name() + coinPOJO.getId(),
                            arrayLine);
                    continue;
                }


            }
            //Если найдено мало вариантов
            if(jumpButtonsStateAndTextMap.size() < 2) {
                for(CoinPOJO coinPOJO : coinPOJOList) {

                    String coinName = coinPOJO.getName().toLowerCase();
                    String coinSymbol = coinPOJO.getSymbol().toLowerCase();

                    if(threeCharsArray != null) {
                        for(CharSequence comparableCharSequence : threeCharsArray) {
                            if(coinName.contains(comparableCharSequence) ||
                                    coinSymbol.contains(comparableCharSequence)) {
                                String arrayLine = coinPOJO.getName() + " (" + coinPOJO.getSymbol() + ")";
                                jumpButtonsStateAndTextMap.put(
                                        State.COIN_FOUND_SUCCESSFULLY.name() + coinPOJO.getId(),
                                        arrayLine);
                                continue;
                            }
                        }
                    }
                }
            }
            return State.COIN_RESEARCH;
        }

        return State.ERROR;
    }



    /*
    Блок кода для метода changeStateAsRequested().
    Задача: проверка не является ли состояние State.COIN_FOUND_SUCCESSFULLY
     */
    private static boolean isThisTheCoinFoundSuccessfullyState(String state) {
        String comparableState = State.COIN_FOUND_SUCCESSFULLY.name();
        if(state.contains(comparableState.subSequence(0, comparableState.length() - 1))) {
            return true;
        }
        return false;
    }
}