package com.example.chatgui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

public class HelloController {
    private DataOutputStream out;
    @FXML
    TextArea textArea;
    @FXML
    Button senBtn;
    @FXML
    Button connectBtn;
    @FXML
    TextField textField;
    @FXML
    VBox usersListVBox;
    boolean isAuth = false;
    String login = null;
    String pass = null;
    int to_id = 0;

    protected void aut() throws IOException {
        String token = "";
        try {
            FileReader reader = new FileReader("C//java/token.txt");
            int i;
            while ((i = reader.read()) != -1)
                token += (char) i;
        } catch (IOException e) {
            System.out.println("token not found");

        }
        if (token.equals("")) {
            textArea.appendText("Введите логин\n");

        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("login", "");
            jsonObject.put("pass", "");
            jsonObject.put("token", "");
            out.writeUTF(jsonObject.toJSONString());
        }
    }

    @FXML

    protected void handlerSend() throws IOException {
        String text = textField.getText();
        if (text.equals("")) return;

        textField.clear();
        textField.requestFocus();
        textArea.appendText(text + "\n");
        if (isAuth) {
            JSONObject request = new JSONObject();
            request.put("msg", text);
            request.put("to_id",to_id);
            out.writeUTF(request.toJSONString());
        } else {
            if (login == null) {
                login = text;
                textArea.appendText("Введите пароль:\n");
            } else if (pass == null) {
                pass = text;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("login", login);
                jsonObject.put("pass", pass);
                jsonObject.put("token", "");
                out.writeUTF(jsonObject.toJSONString());
                login = null;
                pass = null;
            }
        }
    }

    @FXML
    public void connect() {
        try {
            Socket socket = new Socket("127.0.0.1", 9443);
            this.out = new DataOutputStream(socket.getOutputStream());
            DataInputStream is = new DataInputStream(socket.getInputStream());
            connectBtn.setDisable(true);
            senBtn.setDisable(false);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            if (!isAuth) aut();

                            String response = is.readUTF();
                            JSONParser jsonParser = new JSONParser();
                            JSONObject jsonResponse = (JSONObject) jsonParser.parse(response);
                            System.out.println(jsonResponse.get("authResult"));
                            if (jsonResponse.get("users") != null) {
                                usersListVBox.getChildren().removeAll();
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        usersListVBox.getChildren().clear();
                                    }
                                });
                                JSONArray jsonArray = (JSONArray) jsonParser.parse(jsonResponse.get("users").toString());
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JSONObject jsonUserInfo = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                                    String name = jsonUserInfo.get("name").toString();
                                    int id = Integer.parseInt(jsonUserInfo.get("id").toString());
                                    Button userBtn = new Button();  //создаем кнопку
                                    userBtn.setText(name);
                                    userBtn.setOnAction(e -> {
                                        textArea.appendText("Нажата кнопка\n");
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("getHistoryMessage",id );
                                        to_id = id;
                                        textArea.clear();
                                        try {
                                            out.writeUTF(jsonObject.toJSONString());
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                    });
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            usersListVBox.getChildren().add(userBtn);
                                        }
                                    });

                                }
                                // проверяем не прислал ли нам сервер ключи
                            } else if (jsonResponse.get("msg") != null) { //если он есть он не равен нул
                                textArea.appendText(((JSONObject)jsonParser.parse(jsonResponse.get("msg").toString())).get("msg").toString()+ "\n");//отображаем на экране
                            } else if (jsonResponse.get("authResult") != null) {//если он есть он не равен нул
                                isAuth = jsonResponse.get("authResult").toString().equals("success");
                                String token = jsonResponse.get("token").toString();
                                FileOutputStream fos = new FileOutputStream("C//java/token.txt");
                                byte[] buffer = token.getBytes();
                                fos.write(buffer);
                                fos.close();
                            } else if (jsonResponse.get("messages") != null) {//если он есть он не равен нул
                                JSONArray messages = (JSONArray) jsonParser.parse(jsonResponse.get("messages").toString());//если он есть парсим в массив
                                for (int i = 0; i < messages.size(); i++) {        //считываем каждую по строчке
                                    JSONObject message = (JSONObject) jsonParser.parse(messages.get(i).toString()); // распарсит каждый элемент
                                    String name = message.get("name").toString(); //достаём имя,id,сообщение
                                    String msg = message.get("msg").toString();
                                    textArea.appendText(name + ":" + msg + "\n");//отображаем на экране
                                }
                            } else if (jsonResponse.get("privateMessages") != null) {
                                JSONArray messages = (JSONArray) jsonParser.parse(jsonResponse.get("privateMessages").toString());
                                for (int i = 0; i < messages.size(); i++) { //разбираем обьекты
                                    JSONObject singleJsonMessage=(JSONObject) jsonParser.parse(messages.get(i).toString());
                                    String msg=singleJsonMessage.get("msg").toString(); //сохраняем в переменную
                                    textArea.appendText(msg+"\n");
                                }
                            }
                        } catch (IOException | ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}