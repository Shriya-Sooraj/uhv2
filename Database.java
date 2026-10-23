
package com.powerhub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL =
            "jdbc:mysql://localhost:3306/powerhub_db";

    private static final String USER = "root";

    // Replace with your own MySQL password.
    private static final String PASSWORD = "shri@007";


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
