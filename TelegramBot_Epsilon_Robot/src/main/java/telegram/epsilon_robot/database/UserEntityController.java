package telegram.epsilon_robot.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import telegram.epsilon_robot.database.database_entities.UserEntity;

import java.util.HashMap;
import java.util.List;

public class UserEntityController implements DatabaseController {

    private static final SessionFactory sessionFactory = DatabaseConnector.getInstance().getSessionFactory();
    private static final HashMap<Long, UserEntity> userEntityMap = new HashMap<>();
    private static final DatabaseController userEntityController = new UserEntityController();


    public static DatabaseController getInstance() {
        return userEntityController;
    }



    private UserEntityController() {

        if(sessionFactory == null) {
            throw new NullPointerException("sessionFactory have null pointer");
        }

        List<UserEntity> allUserEntities = getAllUsers();

        synchronized (userEntityMap) {

            for (UserEntity user : allUserEntities) {
                userEntityMap.put(user.getChatId(), user);
            }
        }
    }


    @Override
    public List<UserEntity> getAllUsers() {

        List<UserEntity> allUserEntities = null;

        synchronized (sessionFactory) {

            try(Session session = sessionFactory.openSession()) {
                Transaction transaction = session.beginTransaction();
                Query<UserEntity> query = session.createQuery("from UserEntity", UserEntity.class);
                allUserEntities = query.getResultList();
                transaction.commit();
            }
            catch (Exception ex) { ex.printStackTrace(); }
        }

        return allUserEntities;
    }


    @Override
    //получение информации только из списка
    public UserEntity getUserEntity(long chatId) {

        synchronized (userEntityMap) {
            if (!userEntityMap.containsKey(chatId)) {
                return null;
            }
            return userEntityMap.get(chatId);
        }
    }


    @Override
    //получение информации только из БД
    public UserEntity getUserEntityFromDB(long chatId) {

        UserEntity userEntity = null;

        synchronized (sessionFactory) {

            try(Session session = sessionFactory.openSession()) {
                Transaction transaction = session.beginTransaction();
                userEntity = session.get(UserEntity.class, chatId);
                transaction.commit();
            }
            catch (Exception ex) { ex.printStackTrace(); }
        }

        return userEntity;
    }


    @Override
    //Сохранение изменений в списке и БД
    public void saveOrUpdateUserEntity(UserEntity userEntity) {

        synchronized (sessionFactory) {

            try(Session session = sessionFactory.openSession()) {
                Transaction transaction = session.beginTransaction();
                session.saveOrUpdate(userEntity);
                transaction.commit();
            }
            catch (Exception ex) { ex.printStackTrace(); }
        }

        synchronized (userEntityMap) {
            userEntityMap.put(userEntity.getChatId(), userEntity);
        }
    }


    @Override
    //Обновление только параметра состояния
    public void updateStateForUserEntity(long chatId, String newState) {

        UserEntity userEntity;

        synchronized (userEntityMap) {

            userEntity = userEntityMap.get(chatId);
            userEntity.setBotState(newState);
        }
        saveOrUpdateUserEntity(userEntity);
    }


    @Override
    //Добавление одной монеты в список пользователя
    public void addCoinIdToUserCoinList(long chatId, int coinId) {

        UserEntity userEntity;

        synchronized (userEntityMap) {

            userEntity = userEntityMap.get(chatId);
            userEntity.addCoinIdToCoinIdList(coinId);
        }
        saveOrUpdateUserEntity(userEntity);
    }



    //Удаление одной монеты из списка пользователя
    public void deleteCoinIdFromUserCoinList(long chatId, int coinId) {

        UserEntity userEntity;

        synchronized (userEntityMap) {

            userEntity = userEntityMap.get(chatId);
            userEntity.deleteCoinIdFromCoinIdList(coinId);
        }
        saveOrUpdateUserEntity(userEntity);
    }


    @Override
    //Сохранение изменений в списке и БД
    public void deleteUserEntity(long chatId) {

        synchronized (this) {

            try(Session session = sessionFactory.openSession()) {
                Transaction transaction = session.beginTransaction();
                Query<UserEntity> query = session.createQuery("delete from Users where chatId =:currentChatId");
                query.setParameter("currentChatId", chatId);
                query.executeUpdate();
                transaction.commit();
            }
            catch (Exception ex) { ex.printStackTrace(); }

            userEntityMap.remove(chatId);
        }
    }

}
