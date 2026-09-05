package py.una.farmacia.TCP;

import java.io.*;
import java.net.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class TCPClient {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int puerto = 4444;

        System.out.println("Conectando con el Servidor TCP de la Clínica...");

        try (
            Socket socket = new Socket(host, puerto);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
        ) {
            JSONParser parser = new JSONParser();

            // Leer mensaje de bienvenida inicial enviado por TCPServerHilo ("Bienvenido!")
            String mensajeServidor = in.readLine();
            System.out.println("Servidor: " + mensajeServidor);

            boolean ejecutar = true;

            while (ejecutar) {
                System.out.println("\n=== FARMACIA - VALIDACION TCP DE RECETAS ===");
                System.out.println("1. Validar Receta de Paciente");
                System.out.println("2. Cerrar Conexión (Bye)");
                System.out.println("3. Apagar Servidor Remoto (Terminar todo)");
                System.out.print("Seleccione una opción: ");

                String opcion = stdIn.readLine();
                if (opcion == null) break;

                switch (opcion) {
                    case "1":
                        System.out.print("Ingrese la Cédula del Paciente: ");
                        String cedulaStr = stdIn.readLine();

                        Long cedula;
                        try {
                            cedula = Long.parseLong(cedulaStr);
                        } catch (NumberFormatException e) {
                            System.out.println("La cédula ingresada debe ser numérica.");
                            continue;
                        }

                        // Armar JSON de validación
                        JSONObject jsonPeticion = new JSONObject();
                        jsonPeticion.put("operacion", "validar");
                        jsonPeticion.put("cedula", cedula);

                        out.println(jsonPeticion.toJSONString());

                        // Procesar respuesta
                        mensajeServidor = in.readLine();
                        if (mensajeServidor != null) {
                            try {
                                JSONObject jsonRespuesta = (JSONObject) parser.parse(mensajeServidor);
                                Boolean valido = (Boolean) jsonRespuesta.get("valido");

                                System.out.println("\n----------------------------------------");
                                if (Boolean.TRUE.equals(valido)) {
                                    String paciente = (String) jsonRespuesta.get("paciente");
                                    JSONArray medicamentos = (JSONArray) jsonRespuesta.get("medicamentos");

                                    System.out.println("Resultado de validacion: APROBADO");
                                    System.out.println("Receta autorizada para: " + paciente);
                                    System.out.println("Detalle:");
                                    
                                    if (medicamentos != null && !medicamentos.isEmpty()) {
                                        for (Object med : medicamentos) {
                                            System.out.println("  * " + med);
                                        }
                                    } else {
                                        System.out.println("  (Sin remedios especificados)");
                                    }
                                } else {
                                    String mensaje = (String) jsonRespuesta.get("mensaje");
                                    System.out.println("Resultado de validacion: RECHAZADO");
                                    System.out.println("Detalle: " + mensaje);
                                }
                                System.out.println("----------------------------------------");

                            } catch (Exception e) {
                                System.out.println("Respuesta del servidor: " + mensajeServidor);
                            }
                        }
                        break;

                    case "2":
                        out.println("Bye");
                        mensajeServidor = in.readLine();
                        System.out.println("Servidor: " + mensajeServidor);
                        ejecutar = false;
                        break;

                    case "3":
                        out.println("Terminar todo");
                        mensajeServidor = in.readLine();
                        System.out.println("Servidor: " + mensajeServidor);
                        ejecutar = false;
                        break;

                    default:
                        System.out.println("Opción no válida.");
                        break;
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("Host desconocido: " + host);
        } catch (IOException e) {
            System.err.println("Error de I/O en la conexión TCP con la Clínica: " + e.getMessage());
        }
    }
}