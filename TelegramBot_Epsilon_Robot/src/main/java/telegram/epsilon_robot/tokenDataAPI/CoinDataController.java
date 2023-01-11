package telegram.epsilon_robot.tokenDataAPI;

import java.io.File;
import java.util.Map;


public class CoinDataController {


    private static final CoinDataController coinDataController = new CoinDataController();
    private static volatile CacheFileWriterThread cacheFileWriterThread = null;

//    private static final String REQUEST_URI = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/map";
    private static final String REQUEST_URI = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest";
    private static final String CACHE_FILE_PATH = "src/main/resources/coinDataCacheFile.json";
    private static final int UPDATING_PERIOD_IN_SEC = 60;
    private static final String THREAD_NAME = "MapThread";


    public static void main(String[] args) throws InterruptedException {

//        CoinDataController controller = CoinDataController.getInstance().initCacheFileWriterThread();
//        controller.startCacheFileWriterThread();
//
//        for(int i = 0; i < 3; i++) {
//            controller.getCurrentCoinPOJOMap().values().forEach(System.out::println);
//            System.out.println("-");
//            Thread.sleep(5000);
//        }
//
//        controller.stopCacheFileWriterThread();
    }


    private CoinDataController() {}


    public static CoinDataController getInstance() {
        return coinDataController;
    }


    public CoinDataController initCacheFileWriterThread() {

        if(cacheFileWriterThread == null) {

            cacheFileWriterThread = new CacheFileWriterThread(
                    REQUEST_URI,
                    CACHE_FILE_PATH,
                    UPDATING_PERIOD_IN_SEC,
                    THREAD_NAME,
                    CoinMarketCapConnector.getInstance()
            );
        }
        else {
            System.out.println("The class has already been created: CacheFileWriterThread");
        }

        return coinDataController;
    }


    public void startCacheFileWriterThread() {

        if(cacheFileWriterThread != null) {
            cacheFileWriterThread.start();
        }
        else {
            throw new NullPointerException("cacheFileWriterThread = null");
        }
    }


    public void stopCacheFileWriterThread() {

        if(cacheFileWriterThread != null) {
            cacheFileWriterThread.stopUpdatingTheCacheFile();
        }
        else {
            throw new NullPointerException("cacheFileWriterThread = null");
        }
    }


    public Map<Integer, CoinPOJO> getCurrentCoinPOJOMap() {
        return cacheFileWriterThread.getCoinPOJOMap();
    }
}
