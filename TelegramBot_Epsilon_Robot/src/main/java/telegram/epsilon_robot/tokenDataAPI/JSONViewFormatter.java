package telegram.epsilon_robot.tokenDataAPI;


class JSONViewFormatter {
    private String text = null;

    //Переменная используется в методе writeInStructuredText()
    int spaceNum = 0;


    public JSONViewFormatter(String JSONText)
    {
        text = JSONText;
    }


    public JSONViewFormatter() {}


    public void setNewText(String text) {
        this.text = text;
    }


    //Этот метод редактирует JSON с разделением по строкам
    public String toJSONSimpleViewString(){

        if(text == null) { return null; }

        StringBuilder stringBuilder = new StringBuilder();
        int check = 0;

        //Количество пробелов для смещения новой строки
        spaceNum = 0;

        //Перебор всей JSON строки по символам
        for(int i = 0; i < text.length(); i++) {

            Character ch = text.charAt(i);

            //Значение предыдущего символа
            Character prevCh = ch;
            if(i > 0) { prevCh = text.charAt(i - 1); }

            //Проверка записан ли этот элемент. Если записан ранее, то 1
            if(check == 1) {
                check = 0;
                continue;
            }

            /* Проверка открывающих скобок для корректного отображения:
             Не является ли предыдущий элемент одним из разделительных знаков.
             Следующий за ним элемент будет сдвинут на 2 пробела вперед.
             */
            else if('[' == ch || '{' == ch) {

                String checkingPrev = checkThePreviousCharacter(prevCh);
                if("\n".equals(checkingPrev)) {
                    stringBuilder.append(checkingPrev);
                    stringBuilder.append(lineIndent(0));
                }

                stringBuilder.append(ch);
                stringBuilder.append("\r\n");
                stringBuilder.append(lineIndent(1));
            }

            /* Проверка закрывающих скобок для корректного отображения по аналогии:
             Не является ли предыдущий элемент одним из разделительных знаков.
             Следующий за ним элемент будет сдвинут на 2 пробела назад.
             */
            else if(']' == ch || '}' == ch) {

                Character nextCh = ' ';

                if(i != text.length() - 1) { nextCh = text.charAt(i + 1); }

                if(',' == nextCh) {

                    String checkingPrev = checkThePreviousCharacter(prevCh);
                    if("\n".equals(checkingPrev)) {
                        stringBuilder.append(checkingPrev);
                        stringBuilder.append(lineIndent(-1));
                    }

                    stringBuilder.append(ch);
                    stringBuilder.append(nextCh);
                    stringBuilder.append("\r\n");
                    stringBuilder.append(lineIndent(0));
                    check = 1;
                    continue;
                }
                else if('}' == nextCh || ']' == nextCh) {

                    String checkingPrev = checkThePreviousCharacter(prevCh);
                    if("\n".equals(checkingPrev)) {
                        stringBuilder.append(checkingPrev);
                        stringBuilder.append(lineIndent(-1));
                    }

                    stringBuilder.append(ch);
                    stringBuilder.append("\r\n" + lineIndent(-1));
                    stringBuilder.append(nextCh);

                    /*
                    Эта проверка введена для корректного отображения более 3 скобок подряд.
                    В тексте она используется в предпоследнем элементе
                     */
                    if(i + 2 < text.length()) {

                        char nextCh2 = text.charAt(i + 2);
                        if('}' == nextCh2 || ']' == nextCh2)
                            stringBuilder.append("\r\n" + lineIndent(-1));
                    }
                    check = 1;
                    continue;
                }

                String checkingPrev = checkThePreviousCharacter(prevCh);
                if("\n".equals(checkingPrev)) {
                    stringBuilder.append(checkingPrev);
                    stringBuilder.append(lineIndent(-1));
                }

                stringBuilder.append(ch);
                stringBuilder.append("\r\n");
                stringBuilder.append(lineIndent(0));

            }
            //Проверка запятой. Следующий элемент с новой строки со сдвигом на spaceNum единиц
            else if(',' == ch) {

                stringBuilder.append(ch);
                stringBuilder.append("\r\n");
                stringBuilder.append(lineIndent(0));
            }
            else {
                stringBuilder.append(ch);
            }
        }

        return stringBuilder.toString();
    }


    //Функция считает и возвращает отступ (количество пробелов) перед новой строкой.
    private String lineIndent(int isOpeningBracket) {

        String indent = "";
        if(isOpeningBracket == 1) {

            spaceNum += 2;

            for(int i = 0; i < spaceNum; i++) {
                indent += " ";
            }
            return indent;
        }
        else if(isOpeningBracket == -1) {

            spaceNum -= 2;

            for(int i = 0; i < spaceNum; i++) {
                indent += " ";
            }
            return indent;
        }

        for(int i = 0; i < spaceNum; i++) {
            indent += " ";
        }
        return indent;
    }



    private String checkThePreviousCharacter(char previousCh) {

        if('{' == previousCh || '[' == previousCh || '}' == previousCh
                || ']' == previousCh || ',' == previousCh) { return ""; }

        return "\n";
    }
}
