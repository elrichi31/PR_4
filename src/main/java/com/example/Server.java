package com.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
}
