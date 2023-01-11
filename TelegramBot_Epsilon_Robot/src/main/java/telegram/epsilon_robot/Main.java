package telegram.epsilon_robot;


import telegram.epsilon_robot.telegramBot.TelegramBotController;
import telegram.epsilon_robot.tokenDataAPI.CoinDataController;


public class Main {

    public static void main(String[] args) {

        //Запуск потока кеширования CoinMarketCap API
        CoinDataController coinDataController = CoinDataController.getInstance();
        coinDataController.initCacheFileWriterThread();
        coinDataController.startCacheFileWriterThread();

        TelegramBotController.startTelegramBot();
    }
}
