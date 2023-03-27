package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;


public class Client extends Application {

    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private TextArea areaChat;
    private ComboBox<String> comboDestinatario;
    String userNameString;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        VBox panelPrincipal = new VBox();

        areaChat = new TextArea();
        areaChat.setEditable(false);
        ScrollPane scrollPane = new ScrollPane(areaChat);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        panelPrincipal.getChildren().add(scrollPane);

        HBox panelEnviar = new HBox();
        panelEnviar.setAlignment(Pos.CENTER_LEFT);
        panelEnviar.setPadding(new Insets(10));
        panelEnviar.setSpacing(10);

        comboDestinatario = new ComboBox<>();
        comboDestinatario.getItems().add("todos");
        comboDestinatario.getSelectionModel().selectFirst();
        panelEnviar.getChildren().add(comboDestinatario);

        TextField campoMensaje = new TextField();
        campoMensaje.setPrefWidth(400);
        panelEnviar.getChildren().add(campoMensaje);

        Button botonEnviar = new Button("Enviar");
        botonEnviar.setOnAction(event -> {
            String destinatario = comboDestinatario.getValue();
            String mensaje = campoMensaje.getText();
            enviarMensaje(destinatario, mensaje);
            campoMensaje.setText("");
        });
        panelEnviar.getChildren().add(botonEnviar);

        panelPrincipal.getChildren().add(panelEnviar);
        
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Nombre de usuario");
        dialogo.setHeaderText(null);
        dialogo.setContentText("Ingrese su nombre de usuario:");
        dialogo.showAndWait().ifPresent(nombre ->{
            try {
                Socket cliente = new Socket("localhost", 1234);
                salida = new ObjectOutputStream(cliente.getOutputStream());
                entrada = new ObjectInputStream(cliente.getInputStream());
                salida.writeObject(nombre);
                salida.flush();
                userNameString = nombre;
                Thread t = new Thread(new ManejadorServidor());
                t.start(); 
                primaryStage.setOnCloseRequest(e -> {
                    try {
                        cliente.close();
                    } catch (IOException e1) {
                        System.out.println("Desconectado del servidor");
                    }
                });
            } catch (IOException err) {
                System.out.println("Desconectado del servidor");
                //err.printStackTrace();
            }
        });
        
        Scene scene = new Scene(panelPrincipal, 600, 300);
        primaryStage.setTitle("Cliente " + userNameString);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    private void enviarMensaje(String destinatario, String mensaje) {
        try {
            salida.writeObject(destinatario + ":" + mensaje);
            salida.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] removeElement(String[] arr, String item) {
        return Arrays.stream(arr)
                .filter(s -> !s.equals(item))
                .toArray(String[]::new);
    }

    private void actualizarListaUsuarios(String[] usuarios) {
        usuarios = removeElement(usuarios, userNameString);
        comboDestinatario.getItems().clear();
        comboDestinatario.getItems().add("todos");
        comboDestinatario.getItems().addAll(usuarios);
        comboDestinatario.getSelectionModel().selectFirst();
    }
    
    private class ManejadorServidor implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Object mensaje = entrada.readObject();
                    if (mensaje instanceof String) {
                        // mensaje de chat
                        areaChat.appendText((String) mensaje + "\n");
                    } else if (mensaje instanceof String[]) {
                        // lista de usuarios
                        actualizarListaUsuarios((String[]) mensaje);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error al enviar el mensaje o al actualizar lista de usuarios");
                //e.printStackTrace();
            }
        }
    }
}

