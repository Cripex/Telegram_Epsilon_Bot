package telegram.epsilon_robot.telegramBot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.epsilon_robot.telegramBot.botStates.State;

import java.util.*;

//Обработчик визуальной части бота
class ViewController {

    /*TODO - реализовать статические методы:
    - создание Reply клавиатуры (клавиатура над основной)
    - создание Inline клавиатуры (клавиатура в сообщении)
     */



    //Метод создает и возвращает KeyboardMarkup на основании HashMap с состояниями и текстом кнопок
    public static InlineKeyboardMarkup createInlineKeyboardMarkup(Map<State, String> buttonInfoMap,
                                                                  boolean isTheListOrdered,
                                                                  boolean isBackButtonOn) {

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> allButtonList = new ArrayList<>();

        //Добавление полученных кнопок
        if(!buttonInfoMap.isEmpty()) {
            for (State state : buttonInfoMap.keySet()) {

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonInfoMap.get(state));
                button.setCallbackData(state.name());
                allButtonList.add(button);
            }
        }

        //Добавление кнопки "назад"
        if(isBackButtonOn) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Назад");
            button.setCallbackData(State.BACK.name());
            allButtonList.add(button);
        }

        //Если список неупорядочен, каждая кнопка выводится на новую строку
        if(!isTheListOrdered) {

            List<List<InlineKeyboardButton>> inlineKeyboardRows = new ArrayList<>();

            for(InlineKeyboardButton button : allButtonList) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                inlineKeyboardRows.add(row);
            }

            inlineKeyboardMarkup.setKeyboard(inlineKeyboardRows);
        }

        //Если список неупорядочен, кнопки выводятся списком по 3
        else {

            List<List<InlineKeyboardButton>> inlineKeyboardRows = new ArrayList<>();

            int buttonListSize = allButtonList.size();
            for(int i = 0; i < buttonListSize; i++) {

                List<InlineKeyboardButton> row = new ArrayList<>();

                while( i < buttonListSize && row.size() <= 3) {

                    row.add(allButtonList.get(i));
                    i++;
                }

                inlineKeyboardRows.add(row);
            }

            inlineKeyboardMarkup.setKeyboard(inlineKeyboardRows);
        }

        return inlineKeyboardMarkup;
    }



    public static InlineKeyboardMarkup createInlineKeyboardMarkupForStateCOIN_RESEARCH(
            Map<String, String> jumpButtonsStateAndTextMap) {

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> allButtonList = new ArrayList<>();

        //Добавление кнопок из списка
        for(String buttonState : jumpButtonsStateAndTextMap.keySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(jumpButtonsStateAndTextMap.get(buttonState));
            button.setCallbackData(buttonState);
            allButtonList.add(button);
        }

        //Добавление кнопки "назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData(State.BACK.name());
        allButtonList.add(backButton);

        //Добавление кнопок по одной в строку
        List<List<InlineKeyboardButton>> inlineKeyboardRows = new ArrayList<>();
        for(InlineKeyboardButton button : allButtonList) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            inlineKeyboardRows.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(inlineKeyboardRows);

        return inlineKeyboardMarkup;
    }
}
