package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class Server extends Application {
    private static HashMap<String, ObjectOutputStream> clientes;
    TextArea logTextArea = new TextArea();
    Label listUsers = new Label();
    private static HashMap<String, List<Mensaje>> mensajesPorUsuario = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("Server GUI");
        Label logLabel = new Label("Log:");
        VBox logBox = new VBox(logLabel, logTextArea);
        logBox.setAlignment(Pos.CENTER);
        logBox.setSpacing(10);
        logBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane(logBox);
        Scene scene = new Scene(root, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
        ServerTask serverTask = new ServerTask();
        new Thread(serverTask).start();   
        
        Button agregarUsuarioButton = new Button("Agregar usuario");
        agregarUsuarioButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Agregar usuario");
            dialog.setHeaderText("Agregar usuario");
            dialog.setContentText("Por favor ingrese el nombre del usuario:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(nombreUsuario -> {
                try (ObjectOutputStream salida = new ObjectOutputStream(new ByteArrayOutputStream())) {
                    clientes.put(nombreUsuario, salida);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                logTextArea.appendText("Usuario " + nombreUsuario + " agregado.\n");
                enviarListaUsuarios();
            });
        });
        HBox buttonBox = new HBox(agregarUsuarioButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);
        buttonBox.setPadding(new Insets(10));
        root.setBottom(buttonBox);


        Button eliminarUsuarioButton = new Button("Eliminar usuario");
        eliminarUsuarioButton.setOnAction(e -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(clientes.keySet().iterator().next(), clientes.keySet());
            dialog.setTitle("Eliminar usuario");
            dialog.setHeaderText("Eliminar usuario");
            dialog.setContentText("Por favor seleccione el usuario a eliminar:");
        
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(nombreUsuario -> {
                cerrarConexion(nombreUsuario);
                clientes.remove(nombreUsuario);
                
                logTextArea.appendText("Usuario " + nombreUsuario + " eliminado.\n");
                enviarListaUsuarios();
                actualizarListaUsuariosConectados();

            });
        });
        
        buttonBox.getChildren().add(eliminarUsuarioButton);

        // Botones para listar mensajes de un usuario o destinatario
        Button listarUsuarioBtn = new Button("Listar mensajes de usuario");
        Button listarDestinatarioBtn = new Button("Listar mensajes de destinatario");
        Button borrarMensajesUsuarioBtn = new Button("Borrar mensajes de un usuario");

        // Acciones al hacer clic en los botones
        listarUsuarioBtn.setOnAction(e -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(clientes.keySet().iterator().next(), clientes.keySet());
            dialog.setTitle("Listar mensajes de usuario");
            dialog.setHeaderText("Listar mensajes de usuario");
            dialog.setContentText("Por favor seleccione el usuario:");
            Optional<String> result = dialog.showAndWait();

            result.ifPresent(nombreUsuario -> {
                listarMensajesUsuario(nombreUsuario);
            });
        });

        listarDestinatarioBtn.setOnAction(e -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(clientes.keySet().iterator().next(), clientes.keySet());
            dialog.setTitle("Listar mensajes de destinatario");
            dialog.setHeaderText("Listar mensajes de destinatario");
            dialog.setContentText("Introduce el nombre del destinatario:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(nombreUsuario -> {
                listarMensajesDestinatario(nombreUsuario);
            });
        });

        borrarMensajesUsuarioBtn.setOnAction(e -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(clientes.keySet().iterator().next(), clientes.keySet());
            dialog.setTitle("Borrar mensajes de un usuario");
            dialog.setHeaderText("Borrar mensajes de un usuario");
            dialog.setContentText("Introduce el nombre del usuario:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(nombreUsuario -> {
                borrarMensajesUsuario(nombreUsuario);
            });
        });

        // Añadir botones al GUI
        VBox upperBox = new VBox();
        HBox btnBox = new HBox(listarUsuarioBtn, listarDestinatarioBtn, borrarMensajesUsuarioBtn);
        upperBox.getChildren().addAll(btnBox,listUsers);

        btnBox.setAlignment(Pos.TOP_CENTER);
        btnBox.setSpacing(10);
        btnBox.setPadding(new Insets(10));

        upperBox.setAlignment(Pos.TOP_LEFT);
        upperBox.setSpacing(10);
        upperBox.setPadding(new Insets(10));
        actualizarListaUsuariosConectados();
        root.setTop(upperBox);
    }
    private void actualizarListaUsuariosConectados() {
        String usuariosConectados = clientes.keySet().stream().collect(Collectors.joining(", "));
        Platform.runLater(() -> listUsers.setText("Usuarios conectados: " + "\n" + usuariosConectados));
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
                    Thread t = new Thread(new ManejadorCliente(cliente));
                    t.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    // Método para listar los mensajes de un usuario en particular
    private void listarMensajesUsuario(String usuario) {
        logTextArea.appendText(String.format("Mensajes de %s:\n", usuario));

        if (clientes.containsKey(usuario)) {
            mensajesPorUsuario.forEach((i, m) ->{
                if(i.equals(usuario)) {
                    for (Mensaje mensaje : m) {
                        System.out.println(mensaje.getMensaje() + " " + mensaje.getOrigen());
                        logTextArea.appendText(mensaje.getOrigen() + " -> " + mensaje.getDestino() + ": " + mensaje.getMensaje() + "\n");
                    }
                }
            });
        } else {
            logTextArea.appendText(String.format("El usuario %s no tiene mensajes.\n", usuario));
        }
    }
    
    // Método para listar los mensajes de un destinatario en particular
    private void listarMensajesDestinatario(String destinatario) {
        logTextArea.appendText(String.format("Mensajes para %s:\n", destinatario));
        mensajesPorUsuario.forEach((u, m) -> {
            m.forEach(message -> {
                if(message.getDestino().equals(destinatario)){
                    logTextArea.appendText(message.getOrigen() + " -> " + message.getDestino() + ": " + message.getMensaje() + "\n");
                }
            });
        });
    }

    private void borrarMensajesUsuario(String usuario) {
        if (mensajesPorUsuario.containsKey(usuario)) {
            mensajesPorUsuario.remove(usuario);
            logTextArea.appendText(String.format("Mensajes de %s borrados.\n", usuario));
        } else {
            logTextArea.appendText(String.format("El usuario %s no tiene mensajes que borrar.\n", usuario));
        }
    }

    public void cerrarConexion(String origen){
        ObjectOutputStream cliente = clientes.get(origen);
        try {
            cliente.writeObject("eliminado");
            cliente.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void enviarMensaje(String origen, String destino, String mensaje) {
        if (destino.equals("todos")) {
            for (ObjectOutputStream cliente : clientes.values()) {
                try {
                    cliente.writeObject( "(Todos) " + origen + ": " + mensaje);
                    cliente.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            agregarRegistro(origen, "todos", mensaje, true);                    
        } else {
            ObjectOutputStream origenMessage = clientes.get(origen);
            ObjectOutputStream cliente = clientes.get(destino);
            if (cliente != null) {
                try {
                    cliente.writeObject("(Direct message)" + origen + " -> " + destino  + ": " + mensaje);
                    cliente.flush();
                    origenMessage.writeObject("(Direct message)" + origen + " -> " + destino  + ": " + mensaje);
                    origenMessage.flush();
                    agregarRegistro(origen, destino, mensaje, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                agregarRegistro(origen, destino, mensaje, false);
            }
        }
    }

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

        List<Mensaje> mensajesUser = new ArrayList<Mensaje>();
        @Override
        public void run() {
            try {
                entrada = new ObjectInputStream(cliente.getInputStream());
                salida = new ObjectOutputStream(cliente.getOutputStream());
                identificador = (String) entrada.readObject();
                clientes.put(identificador, salida);
                logTextArea.appendText("Cliente " + identificador + " registrado." + "\n");
                enviarListaUsuarios();
                actualizarListaUsuariosConectados();
                while (true) {
                    String mensaje = (String) entrada.readObject();
                    String[] partes = mensaje.split(":", 2);
                    String destino = partes[0];
                    mensaje = partes[1];

                    Mensaje mensajeObj = new Mensaje(identificador, destino, mensaje);
                    mensajesUser.add(mensajeObj);
                    mensajesPorUsuario.put(identificador, mensajesUser);
                    enviarMensaje(identificador, destino, mensaje);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Cliente " + identificador + " desconectado");
                actualizarListaUsuariosConectados();
            } finally {
                clientes.remove(identificador);
                actualizarListaUsuariosConectados();
                logTextArea.appendText("Cliente " + identificador + " desconectado." + "\n");
            }
        }
    }

    public class Mensaje implements Serializable {
        private String origen;
        private String destino;
        private String mensaje;
    
        Mensaje(String identificador, String destino, String mensaje){
            this.origen = identificador;
            this.destino = destino;
            this.mensaje = mensaje;
        }
        // constructor, getters, setters
        public void setDestino(String destino) {
            this.destino = destino;
        }
        public void setOrigen(String origen) {
            this.origen = origen;
        }
        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }

        public String getDestino() {
            return destino;
        }

        public String getMensaje() {
            return mensaje;
        }
        public String getOrigen() {
            return origen;
        }
    }

}
