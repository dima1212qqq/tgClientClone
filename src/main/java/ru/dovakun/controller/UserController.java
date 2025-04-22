package ru.dovakun.controller;

import org.json.JSONException;
import org.json.JSONObject;
import ru.dovakun.model.User;
import ru.dovakun.service.WebSocketService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UserController {

    private WebSocketService webSocketClient;
    private static final String API_BASE_URL = "http://localhost:8080/api/users";

    public UserController() {
        initWebSocketClient();
    }

    private void initWebSocketClient() {
        try {
            webSocketClient = new WebSocketService(
                    new URI("ws://localhost:8080/ws"),
                    users -> System.out.println("Контакты получены: " + users)
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException("Ошибка при создании WebSocket клиента", e);
        }
    }

    public Long authenticateUser(User user) throws IOException, JSONException {
        if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
            return null;
        }

        HttpURLConnection con = createPostConnection(API_BASE_URL + "/login", user.getUsername(), user.getPassword());

        int code = con.getResponseCode();
        if (code == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                if (response.length() > 0) {
                    return Long.parseLong(response.toString());
                }
            }
        }
        return null;
    }

    public boolean registerUser(User user) throws IOException, JSONException {
        if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
            return false;
        }

        if (!user.isPasswordMatching()) {
            return false;
        }

        HttpURLConnection con = createPostConnection(API_BASE_URL + "/register", user.getUsername(), user.getPassword());
        int code = con.getResponseCode();
        System.out.println("HTTP Response Code: " + code);
        System.out.println("Content-Type: " + con.getHeaderField("Content-Type"));

        if (code == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseString = response.toString();
                System.out.println("Server response: " + responseString);
                if (responseString.isEmpty()) {
                    System.err.println("Error: Empty response from server");
                    return false;
                }
                if (!responseString.trim().startsWith("{")) {
                    System.err.println("Error: Response is not a valid JSON object: " + responseString);
                    return true;
                }
                JSONObject responseJson = new JSONObject(responseString);
                return responseJson.getBoolean("success");
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
                System.err.println("Error response (HTTP " + code + "): " + errorResponse.toString());
            }
            return false;
        }
    }

    private static HttpURLConnection createPostConnection(String urlString, String username, String password) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setDoOutput(true);

        JSONObject request = new JSONObject();
        request.put("username", username);
        request.put("password", password);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = request.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return con;
    }

    public WebSocketService getWebSocketClient() {
        return webSocketClient;
    }
}