package telegram.epsilon_robot.database;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/*
Этот класс реализует логику подключения к базе данных.
При инициализации конструктора производится поиск необходимой базы данных
на компьютере. Если нужная БД не найдена, выбрасывается исключение
NullPointerException ( метод isDataBaseExist())
 */
class DatabaseConnector {

    private static DatabaseConnector databaseConnector = null;
    private static SessionFactory sessionFactory = null;

    private final String CONNECTION_URL = "jdbc:mysql://localhost:3306/";
//            "?useSSL=false&amp;serverTimezone=UTC"
    private final String USERNAME = "testuser";
    private final String PASSWORD = "testpass123123";
    private final String DATABASE_NAME = "epsilon_bot";
    private final String HIBERNATE_CONFIGURE_FILE = "hibernate.cfg.xml";


    private DatabaseConnector() {

        //Инициализация SessionFactory для подключения к hibernate
        sessionFactory = createSessionFactory(HIBERNATE_CONFIGURE_FILE);
    }


    public boolean isDatabaseExist() {
        boolean isDatabaseExist = false;
        try {
             isDatabaseExist = isDatabaseCreated(
                    DATABASE_NAME,
                    CONNECTION_URL,
                    USERNAME,
                    PASSWORD);
        }
        catch (SQLException throwables) { throwables.printStackTrace(); }
        return isDatabaseExist;
    }



    public static DatabaseConnector getInstance() {

        if(databaseConnector == null) {
            databaseConnector = new DatabaseConnector();
        }
        return databaseConnector;

    }



    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }



    //Метод проверяет наличие нужной SQL базы данных на компьютере
    private boolean isDatabaseCreated(String databaseName, String connectionUrl,
                                             String username, String password) throws SQLException {

        Connection connection = null;
        Statement statement = null;

        connection = DriverManager.getConnection(connectionUrl, username, password);
        statement = connection.createStatement(
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY
        );

        String SQLQuery = "SHOW DATABASES";
        ResultSet resultSet = statement.executeQuery(SQLQuery);

        List<String> databasesList = new ArrayList<>();
        while (resultSet.next()) {
            databasesList.add(resultSet.getString("Database"));
        }

        statement.close();
        connection.close();

        if(databasesList.contains(databaseName)) {
            return true;
        }
        return false;
    }



    private SessionFactory createSessionFactory(String configureFile) {

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .configure(configureFile)
                .build();
        Metadata metadata = new MetadataSources(serviceRegistry)
                .getMetadataBuilder()
                .build();
        return metadata.getSessionFactoryBuilder()
                .build();
    }
}
