package ru.myorg.server.authentication;

import io.netty.channel.ChannelHandlerContext;
import ru.myorg.server.password_hasher.PasswordService;

import java.io.ByteArrayInputStream;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис по авторизации пользователей, проверке паролей
 */
public class UsersAuthHandler implements UsersStorageService {
    //принимаем объект для операций шифрования
    private final PasswordService secureHasher;
    //объявляем множество авторизованных клиентов <логин, соединение>
    private final ConcurrentHashMap<String, ChannelHandlerContext> authorizedUsers;

    // Пул соединений с базой данных
    private DBCPDataSource dbPool;

    public UsersAuthHandler(PasswordService secureHasher, DBCPDataSource dbPool) {
        this.secureHasher = secureHasher;
        //инициируем множество авторизованных клиентов
        this.authorizedUsers = new ConcurrentHashMap<>();
        //инициируем пул соединений с БД
        this.dbPool = dbPool;
    }

    /**
     * Метод проверяет, авторизован ли пользователя. Если нет - регистрирует его. Если да - проверяет пароль
     * @param login - логин пользователя
     * @param password - пароль пользователя
     * @param ctx - текущее соединение пользователя
     * @return - возвращает результат проверки: пользователь авторизован; неверный логин/пароль; авторизует пользователя.
     */
    @Override
    public synchronized VerificationResult authorizeUser(String login, String password, ChannelHandlerContext ctx){
        VerificationResult result = isUserAuthorized(login, ctx);
        if (result.getStatus() == VerificationStatus.ERROR) {
            return result;
        }
        else if (result.getStatus() == VerificationStatus.AUTHORIZED) {
            return result;
        }
        else if (result.getStatus() == VerificationStatus.NOT_AUTHORIZED) {
            //проверяем пароль
            if (isUserRegistered(login)) {
                if(checkLoginAndPasswordInDB(login, password)){
                    authorizedUsers.put(login, ctx);
                    return new VerificationResult(VerificationStatus.AUTHORIZED);
                }
                else {
                    return new VerificationResult(VerificationStatus.ERROR, "Invalid username or password");
                }
            }
            //регистрируем пользователя
            else {
                if (insertUserIntoDB(login, password)) {
                    authorizedUsers.put(login, ctx);
                    return new VerificationResult(VerificationStatus.AUTHORIZED);
                }
                else {
                    return new VerificationResult(VerificationStatus.ERROR, "Failed to register a user");
                }
            }
        }
        else {
            return new VerificationResult(VerificationStatus.ERROR, "Unknown error");
        }
    }

    /**
     * Метод удаляет клиента из списка авторизованных(по ключу), если оно было авторизовано.
     * @param login - -ключ - логин пользователя
     */
    @Override
    public void deAuthorizeUser(String login) {
        authorizedUsers.remove(login);
    }

    /**
     * Метод проверяет не авторизован ли уже пользователь с таким логином и объектом соединения.
     * @param login - логин пользователя
     * @param ctx - сетевое соединение
     * @return - результат проверки
     */
    @Override
    public VerificationResult isUserAuthorized(String login, ChannelHandlerContext ctx) {
        boolean correctLogin = authorizedUsers.containsKey(login);

        boolean correctCtx = false;
        for (Map.Entry<String, ChannelHandlerContext> user: authorizedUsers.entrySet()) {
            if(user.getValue().channel().equals(ctx.channel())){
                correctCtx = true;
            }
        }

        if (correctLogin) {
            if (!correctCtx) {
                return new VerificationResult(VerificationStatus.ERROR, "User is already logged in");
            } else {
                return new VerificationResult(VerificationStatus.AUTHORIZED);
            }
        }
        else {
            if (correctCtx) {
                return new VerificationResult(VerificationStatus.ERROR, "Incorrect connection");
            } else {
                return new VerificationResult(VerificationStatus.NOT_AUTHORIZED);
            }
        }
    }

    /**
     * Метод проверяет введенный логин в БД на уникальность(не зарегистрирован ли уже такой логин?).
     * @param login - проверяемый логин
     * @return - результат проверки
     */
    private boolean isUserRegistered(String login) {
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = dbPool.getConnection();
            statement = connection.createStatement();
            String sql = String.format("SELECT login FROM users WHERE login = '%s'", login);
            rs = statement.executeQuery(sql);
            if(rs.next()) {
                return true;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (connection != null) dbPool.putConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Метод безопасно верифицирует заданные логин и пароль с данными пользователя в БД.
     * @param login - заданный логин пользователя
     * @param password - заданный пароль пользователя
     * @return - результат проверки данных в БД
     */
    private boolean checkLoginAndPasswordInDB(String login, String password) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            connection = dbPool.getConnection();
            String sql = "SELECT * FROM users WHERE login = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, login);
            rs = preparedStatement.executeQuery();
            if(rs.next()) {
                byte[] secure_hash = rs.getBytes("secure_hash");
                byte[] secure_salt = rs.getBytes("secure_salt");
                if (secureHasher.compareSecureHashes(password, secure_hash, secure_salt)) {
                    return true;
                }
            }
        }
        catch (SQLException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (preparedStatement != null) preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (connection != null) dbPool.putConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Метод безопасно добавляет нового пользователя в БД.
     * @param login - логин пользователя
     * @param password - пароль пользователя
     * @return - результат добавления новой строки в БД.
     */
    private boolean insertUserIntoDB(String login, String password){
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = dbPool.getConnection();
            //генерирует "соль" - случайный байтовый массив
            byte[] secure_salt = secureHasher.generateSalt();
            //генерируем байтовый массив - безопасный хэш с "солью" для заданного пароля и "соли"
            byte[] secure_hash = secureHasher.generateSecureHash(password, secure_salt);
            String sql = "INSERT INTO users (login, secure_hash, secure_salt) VALUES (?, ?, ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, login);
            preparedStatement.setBinaryStream(2, new ByteArrayInputStream(secure_hash));
            preparedStatement.setBinaryStream(3, new ByteArrayInputStream(secure_salt));
            int rs = preparedStatement.executeUpdate();
            if(rs != 0) {
                return true;
            }
        }
        catch (SQLException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (connection != null) dbPool.putConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Метод возвращает каталог пользователя (для синхронизации с сервером)
     * @param login - логин пользователя
     * @return - возвращает строку, содержащую путь к каталогу на компьютере пользователя
     */
    @Override
    public String getCatalog(String login) {
        String sql = "SELECT catalog FROM users WHERE login = ?";
        String catalog = "";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            connection = dbPool.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, login);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                catalog = rs.getString("catalog");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (preparedStatement != null) preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (connection != null) dbPool.putConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return catalog;
    }

    /**
     * Метод сохраняет путь к каталогу пользователя (для синхронизации с сервером) в базе данных
     * @param catalog - путь к каталогу
     * @param login - логин пользователя
     */
    @Override
    public void setCatalog(String catalog, String login) {
        String sql = "UPDATE users SET catalog = ? where login = ?";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = dbPool.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, catalog);
            preparedStatement.setString(2, login);
            preparedStatement.execute();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                if (connection != null) dbPool.putConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}