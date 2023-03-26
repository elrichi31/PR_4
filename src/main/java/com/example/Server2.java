package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;


public class Server2 extends Application {
    private static HashMap<String, ObjectOutputStream> clientes;
    TextArea logTextArea = new TextArea();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("Server GUI");

        // Create GUI components
        Label logLabel = new Label("Log:");

        // Create a layout for the GUI
        VBox logBox = new VBox(logLabel, logTextArea);
        logBox.setAlignment(Pos.CENTER);
        logBox.setSpacing(10);
        logBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane(logBox);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        ServerTask serverTask = new ServerTask();
        new Thread(serverTask).start();

        // Create the scene and show it
        
        
    }
    private class ServerTask extends Task<Void> {

        @Override
        protected Void call() throws Exception {
            clientes = new HashMap<>();
            try {
                ServerSocket servidor = new ServerSocket(1234);
                Platform.runLater(() -> logTextArea.appendText("Servidor iniciado. Esperando conexiones...\n"));
                while (true) {
                    Socket cliente = servidor.accept();
                    String clienteHostName = cliente.getInetAddress().getHostName();
                    //Platform.runLater(() -> logTextArea.appendText("Cliente conectado: " + clienteHostName + "\n"));
                    Thread t = new Thread(new ManejadorCliente(cliente));
                    t.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    

    public void enviarMensaje(String origen, String destino, String mensaje) {
        if (destino.equals("todos")) {
            for (ObjectOutputStream cliente : clientes.values()) {
                try {
                    cliente.writeObject( "(Todos) " + origen + ": " + mensaje);
                    cliente.flush();
                    agregarRegistro(origen, "todos", mensaje, true);
                    //System.out.println(origen + "todos" + mensaje);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ObjectOutputStream cliente = clientes.get(destino);
            if (cliente != null) {
                try {
                    cliente.writeObject(origen + ": " + mensaje);
                    cliente.flush();
                    agregarRegistro(origen, destino, mensaje, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                agregarRegistro(origen, destino, mensaje, false);
            }
        }
    }

    // }
    private void agregarRegistro(String origen, String destino, String mensaje, boolean confirmacion) {
        if (origen != null && destino != null && mensaje != null) {
            LocalDateTime hora = LocalDateTime.now();
            String registro = String.format("%s - %s -> %s: %s (Confirmación: %s)",
                    hora, origen, destino, mensaje, confirmacion);
            logTextArea.appendText(registro + "\n");
            escribirLog(registro);
        }
    }
    
    
    private static void escribirLog(String registro) {
        try {
            FileWriter fw = new FileWriter("log.txt", true);
            fw.write(registro + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void enviarListaUsuarios() {
        String[] usuarios = clientes.keySet().toArray(new String[0]);
        for (ObjectOutputStream cliente : clientes.values()) {
            try {
                cliente.writeObject(usuarios);
                cliente.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class ManejadorCliente implements Runnable {

        private Socket cliente;
        private ObjectInputStream entrada;
        private ObjectOutputStream salida;
        private String identificador;

        public ManejadorCliente(Socket cliente) {
            this.cliente = cliente;
        }

        @Override
        public void run() {
            try {
                entrada = new ObjectInputStream(cliente.getInputStream());
                salida = new ObjectOutputStream(cliente.getOutputStream());
                identificador = (String) entrada.readObject();
                clientes.put(identificador, salida);
                //System.out.println("Cliente " + identificador + " registrado.");
                logTextArea.appendText("Cliente " + identificador + " registrado." + "\n");
                enviarListaUsuarios();
                while (true) {
                    String mensaje = (String) entrada.readObject();
                    String[] partes = mensaje.split(":", 2);
                    String destino = partes[0];
                    mensaje = partes[1];
                    enviarMensaje(identificador, destino, mensaje);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                clientes.remove(identificador);
                //System.out.println("Cliente " + identificador + " desconectado.");
                logTextArea.appendText("Cliente " + identificador + " desconectado." + "\n");
            }
        }
    }

}
