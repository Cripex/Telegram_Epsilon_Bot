package telegram.epsilon_robot.database;

import telegram.epsilon_robot.database.database_entities.UserEntity;

import java.util.List;

public interface DatabaseController {

    List<UserEntity> getAllUsers();

    UserEntity getUserEntity(long chatId);

    UserEntity getUserEntityFromDB(long chatId);

    void saveOrUpdateUserEntity(UserEntity userEntity);

    void updateStateForUserEntity(long id, String state);

    void addCoinIdToUserCoinList(long chatId, int coinId);

    void deleteCoinIdFromUserCoinList(long chatId, int coinId);

    void deleteUserEntity(long chatId);
}
