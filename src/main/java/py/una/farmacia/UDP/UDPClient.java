package py.una.farmacia.UDP;

import java.io.*;
import java.net.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UDPClient {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String direccionServidor = "127.0.0.1";
        int puertoServidor = 9876;

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            InetAddress IPAddress = InetAddress.getByName(direccionServidor);

            System.out.println("=== FARMACIA - CONSULTA UDP DE RECETAS ===");
            System.out.print("Ingrese el número de cédula del paciente: ");
            String strCedula = inFromUser.readLine();

            Long cedula;
            try {
                cedula = Long.parseLong(strCedula);
            } catch (NumberFormatException e) {
                System.err.println("La cédula debe ser un número entero válido.");
                return;
            }

            // 1. Crear el objeto JSON de petición: {"cedula": 1234567}
            JSONObject jsonPeticion = new JSONObject();
            jsonPeticion.put("cedula", cedula);

            byte[] sendData = jsonPeticion.toJSONString().getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, puertoServidor);

            System.out.println("Enviando datagrama UDP a la Clínica (" + IPAddress.getHostAddress() + ":" + puertoServidor + ")...");
            clientSocket.send(sendPacket);

            // 2. Timeout de 10 segundos para prevenir bloqueos por pérdida de paquetes UDP
            clientSocket.setSoTimeout(10000);
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                clientSocket.receive(receivePacket);
                String respuestaStr = new String(receivePacket.getData()).trim();

                // 3. Parsear respuesta JSON
                JSONParser parser = new JSONParser();
                JSONObject jsonRespuesta = (JSONObject) parser.parse(respuestaStr);

                if (jsonRespuesta.containsKey("error")) {
                    System.out.println("\n[Respuesta Clínica]: " + jsonRespuesta.get("error"));
                } else {
                    String nombre = (String) jsonRespuesta.get("nombre");
                    String apellido = (String) jsonRespuesta.get("apellido");
                    JSONArray medicamentos = (JSONArray) jsonRespuesta.get("medicamentos");

                    System.out.println("\n========================================");
                    System.out.println(" Paciente: " + nombre + " " + apellido + " (C.I. " + cedula + ")");
                    System.out.println("----------------------------------------");
                    System.out.println(" Medicamentos Recetados:");

                    if (medicamentos != null && !medicamentos.isEmpty()) {
                        for (Object med : medicamentos) {
                            System.out.println("   * " + med);
                        }
                    } else {
                        System.out.println("   (No cuenta con recetas registradas)");
                    }
                    System.out.println("========================================\n");
                }

            } catch (SocketTimeoutException ste) {
                System.out.println("\n[Error Timeout]: El servidor UDP de la Clínica no respondió a tiempo.");
            }

        } catch (Exception ex) {
            System.err.println("Error en comunicación UDP: " + ex.getMessage());
        }
    }
}