package telegram.epsilon_robot.telegramBot.botStates;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class BotStateJSONtoJavaClassConverter {


    private List<BotStatePOJO> statePOJOList = new ArrayList<>();
    private HashMap<String, BotStatePOJO> botStatePOJOMap = new HashMap<>();

    //counters, values and checkBooleans
    private final String CONSTANT_KEY = "key";
    private final String CONSTANT_VALUE = "value";
    private final String CONSTANT_CHOISES = "choices";

    private boolean isQuotationMarkOpen = false;            //Открыты ли кавычки
    private boolean isObjectMarkOpen = false;               //Открыты ли фигурные скобки
    private boolean isArrayMarkOpen = false;                //Открыты ли квадратные скобки
    private boolean isWritingMarkOpen = false;              //Маркер записи переменных: currentKey, currentValue
    private String writingMarkValue = CONSTANT_KEY;         //Куда именно идет запись: currentKey, currentValue
    private String currentKey = "";                         //Переменная для записи значения ключа
    private String currentValue = "";                       //Переменная для записи значения переменной после ключа



    public BotStateJSONtoJavaClassConverter(String jsonFilePath) {

        File file = new File(jsonFilePath);

        char[] chars = readJSONFileToCharArray(file, 64);

        createPojo(chars);

        if(!statePOJOList.isEmpty()) {
            for(BotStatePOJO statePOJO : statePOJOList) {
                botStatePOJOMap.put(statePOJO.getState(), statePOJO);
            }
        }
        else {
            throw new NullPointerException();
        }
    }



    public HashMap<String, BotStatePOJO> getBotStatePOJOMap() {
        if(botStatePOJOMap.isEmpty()) {
            throw new NullPointerException();
        }
        return botStatePOJOMap;
    }



    private char[] readJSONFileToCharArray(File file, int bufferSizeInChar) {

        final int BUFFER_VALUE = bufferSizeInChar;

        char[] chars = new char[0];
        char[] bufferedChars = new char[BUFFER_VALUE];

//        try(FileInputStream inputStream = new FileInputStream(file)) {
        try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(file), "UTF-8")) {

            int code;
            int counter = 0;
            int bufferedCounter = 0;
            while((code = inputStream.read()) != -1) {

                //Увеличение основного массива на BUFFER_VALUE и перенос данных из буфера.
                if(bufferedCounter == BUFFER_VALUE) {

                    char[] newChars = new char[counter + bufferedCounter];

                    for(int i = 0; i < counter; i++) {
                        newChars[i] = chars[i];
                    }

                    for(int i = 0; i < bufferedCounter; i++) {
                        newChars[counter + i] = bufferedChars[i];
                    }

                    chars = new char[newChars.length];
                    for(int i = 0; i < newChars.length; i++) {
                        chars[i] = newChars[i];
                    }
                    counter += bufferedCounter;
                    bufferedCounter = 0;
                }

                bufferedChars[bufferedCounter] = (char) code;
                bufferedCounter++;
            }

            //Увеличение буфера до фактического размера данных
            char[] newChars = new char[counter + bufferedCounter];

            for(int i = 0; i < counter; i++) {
                newChars[i] = chars[i];
            }

            for(int i = 0; i < bufferedCounter; i++) {
                newChars[counter + i] = bufferedChars[i];
            }

            chars = new char[newChars.length];
            for(int i = 0; i < newChars.length; i++) {
                chars[i] = newChars[i];
            }

        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

        return chars;
    }



    private void createPojo(char[] chars) {

        for(int i = 0; i < chars.length; i++){

            char ch = chars[i];

            //Проверка на символа на соответствие знакам триггерам: " , {} []

            //Проверка открывающей круглой скобки. Начало объекта
            if('{' == ch) {
                isObjectMarkOpen = true;
                statePOJOList.add(new BotStatePOJO());
                continue;
            }

            //Проверка кавычки. Ключ, либо текстовое значение
            else if('\"' == ch) {
                //Открывающая кавычка
                if(!isQuotationMarkOpen) {
                    isQuotationMarkOpen = true;
                    isWritingMarkOpen = true;
                }
                //Закрывающая кавычка
                else {
                    isQuotationMarkOpen = false;
                    isWritingMarkOpen = false;
                }
                continue;
            }

            //Проверка двоеточия. Переход от ключа к значению
            else if(':' == ch) {

                if(CONSTANT_CHOISES.equals(currentKey)) {
                    writingMarkValue = CONSTANT_KEY;
                    currentKey = "";
                    continue;
                }

                writingMarkValue = CONSTANT_VALUE;
                boolean isThisValueString = false;

                for(int a = 1; a <= 5; a++) {
                    if('\"' == chars[i+a]){
                       isThisValueString = true;
                    }
                }

                if(!isThisValueString) {
                    isWritingMarkOpen = true;
                }
                continue;
            }

            //Проверка запятой. Переход от значения к ключу или к другому объекту
            else if(',' == ch) {
                writingMarkValue = CONSTANT_KEY;
                isWritingMarkOpen = false;
                currentValue = currentValue.trim();

                if(!"".equals(currentKey) && !"".equals(currentValue)) {
                    doSetterToPojo(currentKey, currentValue, statePOJOList.get(statePOJOList.size()-1));
                    currentKey = "";
                    currentValue = "";
                }
                continue;
            }

            //Проверка закрывающей круглой скобки и квадратных скобок. В данной реализации эти символы пропускаются
            else if('}' == ch) {
                isObjectMarkOpen = false;

                if((i + 1) != chars.length && ',' == chars[i+1]) {
                    i++;
                }
                continue;
            }
            else if('[' == ch) {
                isArrayMarkOpen = true;
                continue;
            }
            else if(']' == ch) {
                isArrayMarkOpen = false;
                continue;
            }


            //Запись значения в кавычках.
            if(isQuotationMarkOpen && CONSTANT_KEY.equals(writingMarkValue)) {
                currentKey += ch;
            }
            else if(CONSTANT_VALUE.equals(writingMarkValue)) {
                currentValue += ch;
            }
        }
    }



    private void doSetterToPojo(String key, String value, BotStatePOJO pojoClass) {

        if("chain_depth".equals(key)){
            pojoClass.setChainDepth(Integer.parseInt(value));
        }
        else if("chain_id".equals(key)){
            pojoClass.setChainId(Integer.parseInt(value));
        }
        else if("state".equals(key)){
            pojoClass.setState(value);
        }
        else if("jump_button_text".equals(key)) {
            pojoClass.setJumpButtonText(value);
        }
    }

}
