package telegram.epsilon_robot.telegramBot.botStates;

import lombok.Getter;
import lombok.Setter;


/*
Этот POJO класс не является сущностью БД. Он нужен для приведения (интерпритации)
информации о состояниях, полученной в виде JSON текста, к виду JAVA объектов.
Источник информации файл "telegramBotStates.json"
 */

@Getter
@Setter
class BotStatePOJO {

    private int chainDepth;
    private int chainId;
    private String state;
    private String jumpButtonText;

    public BotStatePOJO() {}


    @Override
    public String toString() {
        return "BotStatePOJO{" +
                "chainDepth=" + chainDepth +
                ", chainId=" + chainId +
                ", state='" + state + '\'' +
                ", jumpButtonText='" + jumpButtonText + '\'' +
                '}';
    }
}
