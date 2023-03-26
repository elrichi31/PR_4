package com.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;

public class Server {

    private static HashMap<String, ObjectOutputStream> clientes;
    
    public static void main(String[] args) {
        clientes = new HashMap<>();
        try {
            ServerSocket servidor = new ServerSocket(1234);
            System.out.println("Servidor iniciado. Esperando conexiones...");
            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress().getHostName());
                Thread t = new Thread(new ManejadorCliente(cliente));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enviarMensaje(String origen, String destino, String mensaje) {
        if (destino.equals("todos")) {
            for (ObjectOutputStream cliente : clientes.values()) {
                try {
                    cliente.writeObject( "(Todos) " + origen + ": " + mensaje);
                    cliente.flush();
                    agregarRegistro(origen, "todos", mensaje, true);
                    System.out.println(origen + "todos" + mensaje);
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
    private static void agregarRegistro(String origen, String destino, String mensaje, boolean confirmacion) {
        if (origen != null && destino != null && mensaje != null) {
            LocalDateTime hora = LocalDateTime.now();
            String registro = String.format("%s - %s -> %s: %s (Confirmación: %s)",
                    hora, origen, destino, mensaje, confirmacion);
            System.out.println(registro);
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

    public static synchronized void crearUsuario(String nombre, ObjectOutputStream salida) {
        if (!clientes.containsKey(nombre)) {
            clientes.put(nombre, salida);
            System.out.println("Usuario " + nombre + " creado.");
            enviarListaUsuarios();
        }
    }
    
    public static synchronized void borrarUsuario(String nombre) {
        clientes.remove(nombre);
        System.out.println("Usuario " + nombre + " eliminado.");
        enviarListaUsuarios();
    }


    private static class ManejadorCliente implements Runnable {

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
                System.out.println("Cliente " + identificador + " registrado.");
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
                System.out.println("Cliente " + identificador + " desconectado.");
            }
        }
    }
    private static class Registro {
        private String origen;
        private String destino;
        private String hora;
        private String mensaje;
        private boolean confirmacion;

        @Override
        public String toString() {
            return origen + " -> " + destino + " [" + hora + "]: " + mensaje + " (" + (confirmacion ? "Confirmado" : "No confirmado") + ")";
        }
    }
}
