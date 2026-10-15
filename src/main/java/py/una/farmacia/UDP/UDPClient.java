package py.una.farmacia.UDP;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
public class UDPClient {
    @SuppressWarnings("unchecked")
    public static JSONObject consultar(String host,long ci) throws Exception {
        JSONObject req=new JSONObject();String token=UUID.randomUUID().toString();req.put("cedula",ci);req.put("solicitud",token);
        byte[] bytes=req.toJSONString().getBytes(StandardCharsets.UTF_8);
        try(DatagramSocket s=new DatagramSocket()) {
            s.connect(InetAddress.getByName(host),Integer.getInteger("clinica.udp.port",9876));s.setSoTimeout(10000);
            s.send(new DatagramPacket(bytes,bytes.length));
            DatagramPacket p=new DatagramPacket(new byte[65507],65507);s.receive(p);
            JSONObject resp=(JSONObject)new JSONParser().parse(new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8));
            if(!token.equals(resp.get("solicitud")))throw new java.io.IOException("Respuesta UDP no corresponde a la consulta.");
            return resp;
        }
    }
    public static void main(String[] args){py.una.farmacia.TCP.TCPClient.main(args);}
}
