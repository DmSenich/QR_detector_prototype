package org.example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public final class DBHelper {
    static String DB_URL;
    static String DB_NAME;
    static String LOGIN;
    static String PASSWORD;
    static String TABLE;

    static Properties properties;
    static final String url_prop = "database.properties";

    /// 1 Данные
    /// 2 Проверка наличия таблицы
    /// 3 Создание таблицы при необходимости
    /// 4 Внесение данных
    public static boolean toInit(){
        boolean res = false;
        URL url = DBHelper.class.getClassLoader().getResource(url_prop);
        properties = new Properties();
        FileInputStream fis;
        try {
            fis = new FileInputStream(url.getFile());
            properties.load(fis);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        DB_URL = properties.getProperty("sql.url");
        DB_NAME = properties.getProperty("sql.db_name");
        LOGIN = properties.getProperty("sql.login");
        PASSWORD = properties.getProperty("sql.password");
        TABLE = properties.getProperty("sql.table_name");
        Connection connection;
        try {
            connection = DriverManager.getConnection(DB_URL + DB_NAME, LOGIN, PASSWORD);
            Statement statement = null;
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from first");
            if(resultSet != null){
                System.out.println("Прочитаны данные");
            }
            connection.close();
            tableExist();
            res = true;
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    private static void tableExist(){
        String sql_table = properties.getProperty("sql.create_table");
        try(Connection connection = DriverManager.getConnection(DB_URL + DB_NAME, LOGIN, PASSWORD)){
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql_table);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public static void insert(Date date, String qrcode){
        Timestamp timestamp = new Timestamp(date.getTime());
        String sql_insert = properties.getProperty("sql.insert");
//        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        try (Connection connection = DriverManager.getConnection(DB_URL + DB_NAME, LOGIN, PASSWORD)){
            PreparedStatement statement = connection.prepareStatement(sql_insert);
//            statement.setString(1, TABLE);
            statement.setTimestamp( 1, timestamp);
            statement.setString(2, qrcode);
            statement.executeUpdate();
            System.out.println("Новая запись");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
