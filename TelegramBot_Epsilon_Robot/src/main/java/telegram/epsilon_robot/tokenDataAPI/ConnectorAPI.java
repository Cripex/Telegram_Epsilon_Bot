package telegram.epsilon_robot.tokenDataAPI;


import java.util.Map;

interface ConnectorAPI {

    /*
    Метод запрашивает актуальные данные цен коинов.
    Возвращает содержание JSON файла (строковое представление) с информацией стоимости коинов.
     */
    String requestCurrentTokenData(String uri);

    /*
    Метод преобразует JSON файл (строковое представление) в коллекцию объектов POJO класса.
    Возвращает Map: ключ - id, значение - объект класса POJO.
     */
    Map<Integer, CoinPOJO> convertJSONResponceToPOJOMap(String responceContent);

}
