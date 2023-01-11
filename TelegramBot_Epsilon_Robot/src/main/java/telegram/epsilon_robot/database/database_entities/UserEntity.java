package telegram.epsilon_robot.database.database_entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "Users")
public class UserEntity {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_first_name")
    private String userFirstName;

    @Column(name = "user_last_name")
    private String userLastName;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "bot_state")
    private String botState;

    @Column(name = "has_account")
    private boolean hasAccountField;

    @Column(name = "coin_id_list")
    @ElementCollection
    @CollectionTable(name = "Users_CoinLists")
    private List<Integer> coinIdList;


    public UserEntity() {}


    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public String getBotState() {
        return botState;
    }

    public void setBotState(String botState) {
        this.botState = botState;
    }

    public void addCoinIdToCoinIdList(int coinId) {
        coinIdList.add(coinId);
    }

    public void addCoinsIdToCoinIdList(List<Integer> coinIdList) {
        coinIdList.addAll(coinIdList);
    }

    public boolean deleteCoinIdFromCoinIdList(int coinId) {
        if(coinIdList.contains(coinId)) {
            coinIdList.remove(coinId);
            return true;
        }
        return false;
    }

    public List<Integer> getCoinIdList() {
        return coinIdList;
    }

    public boolean hasAccount() {
        return hasAccountField;
    }

    public void setHasAccountField(boolean hasAccount) {
        hasAccountField = hasAccount;
    }

}
