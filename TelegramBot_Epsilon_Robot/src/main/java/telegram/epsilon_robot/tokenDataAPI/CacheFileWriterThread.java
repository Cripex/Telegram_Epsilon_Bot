package telegram.epsilon_robot.tokenDataAPI;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/*
Класс, наследник класса Thread. Производит обновления кэш-файла
Задача: записывать информацию, полученную по запросу, в json файл в папке resources.
Конструктор: получает на вход ссылку для запроса, путь для сохранения кэш-файла,
период для повторного запроса, имя потока
 */
class CacheFileWriterThread extends Thread{


    private String requestUri = null;
    private Path cacheFilePath = null;
    private int updatingPeriod = 0;
    private volatile boolean isStopped = false;

    private static volatile ConnectorAPI coinDataConnectorAPI = null;
    private static final Map<Integer, CoinPOJO> coinPOJOMap = new HashMap<>();


    @Deprecated
    protected CacheFileWriterThread() { super(); }


    protected CacheFileWriterThread(String requestUri, String cacheFilePath, int updatingPeriodInSec, String name, ConnectorAPI coinDataConnectorAPI) {
        this();
        this.requestUri = requestUri;
        this.cacheFilePath = getCacheFilePath(cacheFilePath);
        this.updatingPeriod = updatingPeriodInSec;
        setName(name);

        if(this.coinDataConnectorAPI == null) {
            this.coinDataConnectorAPI = coinDataConnectorAPI;
        }
    }


    public void stopUpdatingTheCacheFile() {
        isStopped = true;
    }


    @Override
    public void run() {

        //Инициализация и проверки
        if(requestUri == null || cacheFilePath == null || updatingPeriod < 1) {
            System.out.println("Starting error in Thread: " + getName());
            return;
        }
        final JSONViewFormatter formatter = new JSONViewFormatter();

        //Цикличная часть
        while(!isStopped) {

            //Запрашиваются данные с сервиса по API
            String responseContent = coinDataConnectorAPI.requestCurrentTokenData(requestUri);

            //Если ответ не получен, используется кешированное значение прошлого запроса
            if (responseContent == null) {
                System.out.println("Null response. Returned the cash file value in Thread: " + getName());
                responseContent =  readCacheFile(cacheFilePath);
            }
            else { writeNewCacheFile(formatter, responseContent); }

            synchronized (coinPOJOMap) {
                coinPOJOMap.clear();
                coinPOJOMap.putAll(coinDataConnectorAPI.convertJSONResponceToPOJOMap(responseContent));
            }

            sleepInSecond(updatingPeriod);
        }
    }


    public Map<Integer, CoinPOJO> getCoinPOJOMap() {
        synchronized (coinPOJOMap) {
            return coinPOJOMap;
        }
    }


    //Метод записывает полученный ответ в локальный JSON файл.
    private String writeNewCacheFile(JSONViewFormatter formatter, String responseContent) {

        try {
            Files.deleteIfExists(cacheFilePath);
            Files.createFile(cacheFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }

        formatter.setNewText(responseContent);
        String formatedResponceContent = formatter.toJSONSimpleViewString();

        try (PrintWriter writer = new PrintWriter(cacheFilePath.toString())) {
            writer.write(formatedResponceContent);
            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Cache file successfully updated in Thread: "
                + getName());
        return responseContent;
    }



    private void sleepInSecond(int seconds) {

        int timeToSleep = seconds * 1000;
        try { Thread.sleep(timeToSleep); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }


    private Path getCacheFilePath(String stringPath) {
        return new File(stringPath).toPath();
    }


    private String readCacheFile(Path path) {

        StringBuilder stringBuilder = new StringBuilder();

        try(InputStreamReader reader = new InputStreamReader(new FileInputStream(path.toString()), "UTF-8")) {

            int code = 0;
            while((code = reader.read()) != -1) {

                char ch = (char) code;
                stringBuilder.append(ch);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }
}
