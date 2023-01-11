package telegram.epsilon_robot.tokenDataAPI;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/*
Этот POJO класс не является сущностью БД. Он нужен для приведения (интерпритации)
информации о цене коинов, полученной в виде JSON текста, к виду JAVA объектов
Источник информации файл "coinDataCacheFile.json"
 */

@Getter
@Setter
public class CoinPOJO {


    private int id;
    private String name;
    private String symbol;
    private String slug;

//    @Column(name = "usd_price")
    private float usdPrice;

//    @Column(name = "percent_change_1h")
    private float percentChange1h;

//    @Column(name = "percent_change_24h")
    private float percentChange24h;

    private long marketCap;


    public CoinPOJO() {}


    @Override
    public String toString() {
        return "CoinPOJO{" +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", slug='" + slug + '\'' +
                ", usdPrice=" + usdPrice +
                ", percentChange1h=" + percentChange1h +
                ", percentChange24h=" + percentChange24h +
                ", marketCap=" + marketCap +
                '}';
    }

    public String getCoinInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("\nНазвание = ").append(name)
                .append("\nАббревиатура = ").append(symbol)
                .append("\nЦена = ").append(usdPrice).append(" USD")
                .append("\nИзменение цены за час = ")
                .append(percentChange1h).append(" %")
                .append("\nИзменение цены за 24 часа = ")
                .append(percentChange24h).append(" %")
                .append("\nРыночная капитализация = ")
                .append(marketCap).append(" USD");
        return builder.toString();
    }
}
