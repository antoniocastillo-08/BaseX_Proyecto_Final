package org.example.basexfinal;

import org.basex.api.client.ClientSession;
import org.basex.core.cmd.Open;

public class BaseXConnector {
    private static final String HOST = "localhost";
    private static final int PORT = 1984;
    private static final String USER = "Castillo";
    private static final String PASSWORD = "2118808";
    private static final String DATABASE_NAME = "Library";

    private static ClientSession session;

    public static void connect() {
        try {
            session = new ClientSession(HOST, PORT, USER, PASSWORD);
            session.execute(new Open(DATABASE_NAME));
        } catch (Exception e) {
            System.err.println("Error conectando a BaseX: " + e.getMessage());
        }
    }

    public static String executeQuery(String query) {
        try {
            return session.query(query).execute();
        } catch (Exception e) {
            System.err.println("Error ejecutando XQuery: " + e.getMessage());
            return null;
        }
    }

    public static void close() {
        try {
            if (session != null) session.close();
        } catch (Exception e) {
            System.err.println("Error cerrando sesión: " + e.getMessage());
        }
    }
}
