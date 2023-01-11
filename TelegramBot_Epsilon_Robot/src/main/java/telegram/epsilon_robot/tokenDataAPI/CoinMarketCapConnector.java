package telegram.epsilon_robot.tokenDataAPI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class CoinMarketCapConnector implements ConnectorAPI {

//    private static boolean isInitialized = false;
    private static final CoinMarketCapConnector APIConnector = new CoinMarketCapConnector();

    private final String uri = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest";
//    private static String uri = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/map";
    //TODO: Вписать ключ CoinMarketCap API
    private static String apiKey = "";



    private CoinMarketCapConnector() {}


    public static CoinMarketCapConnector getInstance() {
        return APIConnector;
    }


    private String makeAPICall(String uri, List<NameValuePair> parameters)
            throws URISyntaxException, IOException {

        String response_content = "";

        URIBuilder query = new URIBuilder(uri);
//        query.addParameters(parameters);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(query.build());

        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.addHeader("X-CMC_PRO_API_KEY", apiKey);

        CloseableHttpResponse response = client.execute(request);

        try {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            response_content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } finally {
            response.close();
        }

        return response_content;
    }



    @Override
    //
    public String requestCurrentTokenData(String uri){

        try {
            return makeAPICall(uri, null);
        }
        catch (URISyntaxException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

        return null;
    }


    @Override
    /*
    Метод преобразует JSON файл, полученный по запросу URL:
    'https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest'
     */
    public Map<Integer, CoinPOJO> convertJSONResponceToPOJOMap(String responceContent) {

        List<CoinPOJO> coinPOJOList = new ArrayList<>();
        String[] allowedKeys = new String[] {"id", "name", "symbol",
                "slug", "quote", "USD", "price",
                "percent_change_1h", "percent_change_24h", "market_cap"};

        //counters, values and checkBooleans
        final String CONSTANT_KEY = "key";
        final String CONSTANT_VALUE = "value";
        final String CONSTANT_DATA = "data";

        boolean isQuotationMarkOpen = false;            //Открыты ли кавычки
        boolean isObjectMarkOpen = false;               //Открыты ли фигурные скобки
        int openObjectMarkChainLevel = 0;               //Переменная для проверки подобъектов
        boolean isArrayMarkOpen = false;                //Открыты ли квадратные скобки
        int openArrayMarkChainLevel = 0;               //Переменная для проверки подмассивов
        boolean isWritingMarkOpen = false;              //Маркер записи переменных: currentKey, currentValue
        boolean isDataKeyPassed = false;                //Пройден ли ключ "data"
        String writingMarkValue = CONSTANT_KEY;         //Куда именно идет запись: currentKey, currentValue
        String currentKey = "";                         //Переменная для записи значения ключа
        String currentValue = "";                       //Переменная для записи значения переменной после ключа


        char[] chars = responceContent.toCharArray();


        for(int i = 0; i < chars.length; i++) {

            char ch = chars[i];


            /*
            --------------------------------------------------
            Блок записи ключей и значений. Блок зациклен, пока не пройдет ключ "data"
             */


            //Проверка кавычки. Ключ, либо текстовое значение
            if ('\"' == ch) {
                //Открывающая кавычка
                if (!isQuotationMarkOpen) {
                    isQuotationMarkOpen = true;
                    isWritingMarkOpen = true;

                    if (!isDataKeyPassed) {
                        currentKey = "";
                    }
                }
                //Закрывающая кавычка
                else {
                    isQuotationMarkOpen = false;
                    isWritingMarkOpen = false;
                }
            } else if (!isDataKeyPassed && isQuotationMarkOpen && CONSTANT_KEY.equals(writingMarkValue)) {
                currentKey += ch;
            }


            /*
            Дополнительный блок. Проверяет текущий ключ на соответствие "data".
            Работает до того момента, пока ключ "data" не будет пройден
             */
            else if (!isDataKeyPassed) {

                if (CONSTANT_DATA.equals(currentKey)) {
                    isDataKeyPassed = true;
                    currentKey = "";

                    while (chars[i + 1] != '{') {
                        i++;
                    }
                } else if (ch == ':') {
                    while (chars[i] != ',') {
                        i++;
                    }
                }
                continue;
            }
            /*
            --------------------------------------------------
            Блок поиска триггеров: , {} []
             */


            //Проверка открывающей круглой скобки. Начало объекта
            else if ('{' == ch) {

                if (!isObjectMarkOpen && openObjectMarkChainLevel == 0) {
                    coinPOJOList.add(new CoinPOJO());
                }

                isObjectMarkOpen = true;
                openObjectMarkChainLevel++;
            }


            //Проверка двоеточия. Переход от ключа к значению; проверка ключа
            else if (':' == ch) {

                //Проверка ключа на соответствие разрешенным ключам
                boolean isAllowedKey = false;
                for (String allowedKey : allowedKeys) {
                    if (currentKey.equals(allowedKey)) {
                        isAllowedKey = true;
                        break;
                    }
                }

                //Если ключ не является разрешенных, пропускаем его значение
                if (!isAllowedKey) {

                    char nextChar;
                    char coveringChar;
                    while (((nextChar = chars[i + 1]) == ' ' || nextChar == '\n' || nextChar == '\r')
                            && (i + 1 != chars.length)) {
                        i++;
                    }
                    switch (nextChar) {
                        case '"':
                            coveringChar = '"';
                            isQuotationMarkOpen = true;     //В этом случае следущий символ - закрывающая кавычка
                            break;
                        case '[':
                            coveringChar = ']';
                            openArrayMarkChainLevel++;      //В этом случае следущий символ - закрывающая кв. скобка
                            break;
                        case '{':
                            coveringChar = '}';
                            openObjectMarkChainLevel++;     //В этом случае следущий символ - закрывающая фг. скобка
                            break;
                        default:
                            coveringChar = ',';
                            break;
                    }
                    i++;
                    while ((i + 2 != chars.length) && chars[i + 1] != coveringChar) {
                        i++;
                    }
                    currentKey = "";
                }
                //Ключ является разрешенным
                else {
                    //Для ключей "quote" и "USD" пропускаем символы пока не попадем к нужным ключам
                    switch (currentKey) {
                        case "quote":
                            while (chars[i + 1] != '{') {
                                i++;
                            }
                            currentKey = "";
                            break;
                        case "USD":
                            while (chars[i + 1] != '{') {
                                i++;
                            }
                            currentKey = "";
                            break;
                        //Ключ разрешен и должен быть записан
                        default:
                            isWritingMarkOpen = true;
                            writingMarkValue = CONSTANT_VALUE;
                            break;
                    }
                }
            }


            //Проверка запятой. Переход от значения к ключу или к другому объекту
            else if (',' == ch) {

                isWritingMarkOpen = false;

                if (CONSTANT_VALUE.equals(writingMarkValue) && currentKey != null && currentValue != null) {
                    CoinPOJO coinPOJO = coinPOJOList.get(coinPOJOList.size() - 1);
                    switch (currentKey) {
                        case "id":
                            coinPOJO.setId(Integer.parseInt(currentValue.trim()));
                            break;
                        case "name":
                            coinPOJO.setName(currentValue.trim());
                            break;
                        case "symbol":
                            coinPOJO.setSymbol(currentValue.trim());
                            break;
                        case "slug":
                            coinPOJO.setSlug(currentValue.trim());
                            break;
                        case "price":
                            coinPOJO.setUsdPrice(Float.parseFloat(currentValue.trim()));
                            break;
                        case "percent_change_1h":
                            coinPOJO.setPercentChange1h(Float.parseFloat(currentValue.trim()));
                            break;
                        case "percent_change_24h":
                            coinPOJO.setPercentChange24h(Float.parseFloat(currentValue.trim()));
                            break;
                        case "market_cap":
                            double value = Double.parseDouble(currentValue);
                            coinPOJO.setMarketCap((long)value);
                            break;

                    }
                }

                writingMarkValue = CONSTANT_KEY;
                currentKey = "";
                currentValue = "";
            }

            //Проверка закрывающей круглой скобки и квадратных скобок. В данной реализации эти символы пропускаются
            else if ('}' == ch) {

                isWritingMarkOpen = false;
                openObjectMarkChainLevel--;

                if (openObjectMarkChainLevel == 0) {
                    isObjectMarkOpen = false;
                }
            } else if ('[' == ch) {

                if (!isArrayMarkOpen && openArrayMarkChainLevel == 0) {
                    isArrayMarkOpen = true;
                }
                openArrayMarkChainLevel++;
            } else if (']' == ch) {

                isWritingMarkOpen = false;
                openArrayMarkChainLevel--;

                if (isArrayMarkOpen && openArrayMarkChainLevel == 0) {
                    isArrayMarkOpen = false;
                }
            }


            //Запись значения в кавычках.
            else if (isWritingMarkOpen && CONSTANT_KEY.equals(writingMarkValue)) {
                currentKey += ch;
            } else if (isWritingMarkOpen && CONSTANT_VALUE.equals(writingMarkValue)) {
                currentValue += ch;
            }

        }
//        coinPOJOList.forEach(System.out::println);
//        System.out.println(coinPOJOList.size());

        Map<Integer, CoinPOJO> coinPOJOMap = new HashMap<>();
        for(CoinPOJO coinPOJO : coinPOJOList) {
            coinPOJOMap.put(coinPOJO.getId(), coinPOJO);
        }

        return coinPOJOMap;
    }
}
