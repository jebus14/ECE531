package com.sanchez;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;



public class DatabaseConnection {

    private static final String DB_CONNECTION = "jdbc:mysql://127.0.0.1:3306/my_database";
    private static final String ROOT = "root";
    private static final String PASSWORD = "Rocketman!";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_CONNECTION, ROOT, PASSWORD);
    }

    public int createData(DataObject dataObject) {
        String sql = "INSERT INTO test_table (test_word, test_number) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dataObject.getTestWord());
            statement.setInt(2, dataObject.getTestNumber());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating data failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating data failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; // Return -1 to indicate an error
        }
    }

    public boolean updateData(DataObject dataObject) {
        String sql = "UPDATE test_table SET test_word = ?, test_number = ? WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataObject.getTestWord());
            statement.setInt(2, dataObject.getTestNumber());
            statement.setInt(3, dataObject.getId());

            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public DataObject getData(int id) {
        String sql = "SELECT test_word, test_number FROM test_table WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    DataObject dataObject = new DataObject();
                    dataObject.setId(id);
                    dataObject.setTestWord(resultSet.getString("test_word"));
                    dataObject.setTestNumber(resultSet.getInt("test_number"));
                    return dataObject;
                } else {
                    return null; // Data not found
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null; // Return null to indicate an error
        }
    }

    public boolean deleteData(int id) {
        String sql = "DELETE FROM test_table WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}