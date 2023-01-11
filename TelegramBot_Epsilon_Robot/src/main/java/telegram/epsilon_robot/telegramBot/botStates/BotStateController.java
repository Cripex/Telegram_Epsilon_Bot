package telegram.epsilon_robot.telegramBot.botStates;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


/*
Класс, который отвечает за работу с состояниями бота: получение, изменение, интерпретация состояний.
Изначально все состояния записаны в JSON файл BOT_STATES_FILE_PATH, откуда они инпретируются в
объекты класса BotStatePOJO и записываются в botStatePOJOMap.

!Для добавления новых состояний необходимо:
    - вписать новый объект в JSON файл BOT_STATES_FILE_PATH;
    - добавить характеристику "state" нового объекта в enum State (директория botStates);
    - добавить новый триггер в метод sendResponse() класса MessageHandler;
Далее при следующем запуске состояние будет прочитано и добавлено автоматически.

Класс содержит методы:
    - для получения текущего chainId (идентификатор цепочки - смотреть файл .json)
    - для получения нового chainId на основе нового состояния ("state") и старого chainId (для проверки подлинности)
    - для получения текущего состояния ("state") на основе текущего chainId
 */

public class BotStateController {

    private static final BotStateController controller = new BotStateController();

    private static final String BOT_STATES_FILE_PATH = "src/main/resources/static/telegramBotStates.json";
    private static BotStateJSONtoJavaClassConverter converter = null;

    private HashMap<String, BotStatePOJO> botStatePOJOMap = null;
    private List<BotStatePOJO> botStatePOJOList = null;



    public static void main(String[] args) {
//        System.out.println(controller.getNewChainId(State.BACK, 1));   //=0
//        System.out.println(controller.getNewChainId(State.BACK, 5));   //=0
//        System.out.println(controller.getNewChainId(State.HOW_MUCH_DOES_IT_COST, 3));   //=34
//        System.out.println(controller.getNewChainId(State.HOW_MUCH_DOES_IT_COST, 2));   //=null
//        System.out.println(controller.getNewChainId(State.HOW_MUCH_DOES_IT_COST, 1));
//
//        System.out.println(controller.getStateByChainId(1));

        BotStateController.getInstance().botStatePOJOList.forEach(System.out::println);
    }



    private BotStateController() {

        converter = new BotStateJSONtoJavaClassConverter(BOT_STATES_FILE_PATH);
        botStatePOJOMap = converter.getBotStatePOJOMap();

        botStatePOJOList = new ArrayList<>();
        for(BotStatePOJO botStatePOJO : botStatePOJOMap.values()) {
            botStatePOJOList.add(botStatePOJO);
        }
    }



    public static BotStateController getInstance() {
        return controller;
    }



    public int getNewChainId(State state, int currentChainId) {
        return getNewChainId(state.name(), currentChainId);
    }


    public int getNewChainId(String state, int currentChainId) {

        int newChainId;

        if(State.ERROR.name().equals(state)) {
            return currentChainId;
        }

        //Команда "Назад"
        else if(State.BACK.name().equals(state)) {
            if(currentChainId < 10) {
                newChainId = 0;
            }
            else {
                newChainId = currentChainId / 10;
            }
            return newChainId;
        }

        //Пользователь повторно ввел "/start"
        else if(State.NEW_USER.name().equals(state)) {
            return 0;
        }

        else if(botStatePOJOMap.containsKey(state)) {
            newChainId = botStatePOJOMap.get(state).getChainId();

            //Базовая ситуация. Наследник имеет chainId больше на 1 единицу справа
            if((newChainId/10) == currentChainId) {
                return newChainId;
            }
            //Исключение для состояния поиска монеты. Состояние может не меняться
            else if (State.COIN_RESEARCH.name().equals(state) && newChainId == currentChainId) {
                return newChainId;
            }
            //Исключение для состояния успешного поиска. chainId = 110 -> 111
            else if (State.COIN_FOUND_SUCCESSFULLY.name().equals(state)) {
                return newChainId;
            }
            //Исключение для состояния только созданного аккаунта. chainId = 20 -> [21...29]
            else if( (newChainId / 10) == 2 && currentChainId == 20) {
                return newChainId;
            }
            else return 0;      //Возвращает начальное состояние "Главного меню", если ошибка с chainId
        }

        throw new NullPointerException();
    }



    public State getEnumStateByChainId(int currentChainId) {

        String state = getStateByChainId(currentChainId);
        if(state != null) {
            return State.valueOf(state);
        }
        return null;
    }


    public String getStateByChainId(int currentChainId) {

        if(botStatePOJOList.isEmpty()) { return null; }

        AtomicReference<String> botState = new AtomicReference<>();

        botStatePOJOList.forEach(statePOJO -> {
            if(statePOJO.getChainId() == currentChainId) {
              botState.set(statePOJO.getState());
            }
        });

        if(botState != null) {
            return botState.get();
        }

        return null;
    }



    public int getCurrentChainIdByState(State state) {
        return getCurrentChainIdByState(state.name());
    }


    public int getCurrentChainIdByState(String state) {

        if(botStatePOJOMap.containsKey(state)) {
            return botStatePOJOMap.get(state).getChainId();
        }

        throw new NullPointerException();
    }


    /*
    Метод возращает HashMap со значениями состояния бота (state) и текста кнопки (jump_button_text)
    для всех найденных наследников. Вычисление наследников производится на основе полученного
    параметра "текущее состояние" (state для состояния в котором находится бот)
        */
    public Map<State, String> getHeirsStateAndJumpButtonText(State currentState) {

        Map<State, String> heirsBotStatePOJOInfoMap = new TreeMap<>();

        //Текущий chainId. Вычислен на основании полученного текущего состояния state
        int currentChainId = -1;
        currentChainId = getCurrentChainIdByState(currentState);

        if(currentChainId == -1) { throw new NullPointerException(); }

        //Создание массива идентификаторов chainId возможных наследников
        int heirChainIdFactor = currentChainId * 10;
        int[] heirChainIdArray = new int[9];
        for(int i = 0; i < 9; i++) {
            heirChainIdArray[i] = heirChainIdFactor + i + 1;
        }

        //Поиск объктов BotStatePOJO с параметром chainId, равным одному из значений массива
        int checkCounter = 0;
        for(int i = 0; i < botStatePOJOList.size(); i++) {

            BotStatePOJO botStatePOJO = botStatePOJOList.get(i);
            int chainId = botStatePOJO.getChainId();

            for(int heirChainId : heirChainIdArray) {
                if(chainId == heirChainId) {
                    heirsBotStatePOJOInfoMap.put(
                            State.valueOf(botStatePOJO.getState()),
                            botStatePOJO.getJumpButtonText()
                            );
                    checkCounter++;
                    break;
                }
            }
        }

        if(checkCounter == 0) { throw new NullPointerException(); }

        return heirsBotStatePOJOInfoMap;
    }


//    //Метод возвращает массив id наследников для объекта с указанным параметром state
//    public HashMap<Integer, State> getHeirsChainIdAndStatesByCurrentState(State currentState) {
//
//        HashMap<Integer, State> heirsChainIdAndStatesMap = new HashMap<>();
//
//        int chainId = getCurrentChainIdByState(currentState);
//        int counter = 0;
//        int[] bufferedHeirsChainId = new int[10];
//
//        for(int i = chainId * 10 + 1; i <= chainId + 9; i++) {
//
//            State state = getEnumStateByChainId(i);
//            if(state == null) { break; }
//
//            heirsChainIdAndStatesMap.put(i, state);
//            counter++;
//        }
//
//        if(counter == 0) { return null; }
//
//        return heirsChainIdAndStatesMap;
//    }
}