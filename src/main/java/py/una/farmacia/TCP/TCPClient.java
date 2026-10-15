package py.una.farmacia.TCP;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import py.una.farmacia.UDP.UDPClient;
@SuppressWarnings("unchecked")
public class TCPClient {
    private static final BufferedReader teclado=new BufferedReader(new InputStreamReader(System.in));
    private static String leer(String texto) throws IOException {System.out.print(texto);String s=teclado.readLine();if(s==null)throw new EOFException();return s.trim();}
    private static long numero(String texto) throws IOException {try{long n=Long.parseLong(leer(texto));if(n>0)return n;}catch(NumberFormatException e){}throw new IllegalArgumentException("Ingrese un numero entero positivo.");}
    private static JSONObject req(String op,long ci){JSONObject r=new JSONObject();r.put("operacion",op);r.put("cedula",ci);return r;}
    private static JSONObject enviar(PrintWriter out,BufferedReader in,JSONObject req) throws Exception {
        out.println(req.toJSONString());if(out.checkError())throw new IOException("No se pudo enviar la solicitud.");
        String line=in.readLine();if(line==null)throw new EOFException("La clinica cerro la conexion.");
        JSONObject r=(JSONObject)new JSONParser().parse(line);verificar(r);return r;
    }
    private static void verificar(JSONObject r){if(!Boolean.TRUE.equals(r.get("ok")))throw new IllegalArgumentException(String.valueOf(r.get("error")));}
    private static void medicamentos(JSONObject r) {
        JSONArray a=(JSONArray)r.get("medicamentos");
        for(Object o:a){JSONObject x=(JSONObject)o;System.out.println("  "+x.get("id")+". "+x.get("medicamento")+" - Gs. "+x.get("precio"));}
        if(a.isEmpty())System.out.println("No tiene medicamentos pendientes.");
    }
    private static void comprobante(JSONObject r){System.out.println("\nCOMPROBANTE ACADEMICO N. "+r.get("id")+" | "+r.get("fecha"));System.out.println(r.get("paciente")+" - CI "+r.get("cedula"));medicamentos(r);System.out.println("TOTAL: Gs. "+r.get("total"));}
    private static void tcp(String host,long ci,boolean compra) throws Exception {
        boolean confirmacionEnviada=false;
        try(Socket s=new Socket()) {
            s.connect(new InetSocketAddress(host,Integer.getInteger("clinica.tcp.port",4444)),5000);s.setSoTimeout(10000);
            try(PrintWriter out=new PrintWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8),true);
                BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8))) {
                if(compra) {
                    JSONObject p=enviar(out,in,req("pendientes_compra",ci));System.out.println("Paciente: "+p.get("paciente"));medicamentos(p);
                    if(((JSONArray)p.get("medicamentos")).isEmpty())return;
                    String seleccion=leer("IDs separados por coma (0 para cancelar): ");if("0".equals(seleccion))return;
                    JSONArray ids=new JSONArray();for(String id:seleccion.split(",",-1)){long n;try{n=Long.parseLong(id.trim());}catch(NumberFormatException e){throw new IllegalArgumentException("Seleccion invalida.");}if(n<=0||ids.contains(n))throw new IllegalArgumentException("IDs invalidos o repetidos.");ids.add(n);}
                    JSONObject r=req("preparar_compra",ci);r.put("ids",ids);JSONObject resumen=enviar(out,in,r);medicamentos(resumen);System.out.println("TOTAL: Gs. "+resumen.get("total"));
                    if(!"S".equalsIgnoreCase(leer("Confirmar compra? S/N: "))){System.out.println(enviar(out,in,req("cancelar_compra",ci)).get("mensaje"));return;}
                    JSONObject confirmar=req("confirmar_compra",ci);confirmar.put("solicitud",resumen.get("solicitud"));confirmacionEnviada=true;
                    comprobante(enviar(out,in,confirmar));confirmacionEnviada=false;
                }else {
                    JSONArray a=(JSONArray)enviar(out,in,req("listar_comprobantes",ci)).get("comprobantes");
                    if(a.isEmpty()){System.out.println("No tiene comprobantes registrados.");return;}
                    System.out.println("Comprobantes del mas reciente al mas antiguo:");
                    for(Object o:a){JSONObject x=(JSONObject)o;System.out.println(x.get("id")+" | "+x.get("fecha")+" | Gs. "+x.get("total"));}
                    String elegido=leer("Numero de comprobante (0 para volver): ");if("0".equals(elegido))return;
                    long id;try{id=Long.parseLong(elegido);}catch(NumberFormatException e){throw new IllegalArgumentException("Numero invalido.");}
                    JSONObject r=req("detalle_comprobante",ci);r.put("id",id);comprobante(enviar(out,in,r));
                }
            }
        }catch(IOException e){if(confirmacionEnviada)System.out.println("No se pudo conocer el resultado. Consulte sus comprobantes antes de repetir la compra.");throw e;}
    }
    public static void main(String[] args) {
        String host=args.length>0?args[0]:"127.0.0.1";
        while(true) {
            try {
                System.out.println("\n=== FARMACIA ===\n1. Consultar recetas pendientes (UDP)\n2. Comprar medicamentos con receta (TCP)\n3. Consultar comprobantes (TCP)\n4. Salir");
                String op=leer("Seleccione una opcion: ");if("4".equals(op))return;
                if(!"1".equals(op)&&!"2".equals(op)&&!"3".equals(op)){System.out.println("Opcion invalida.");continue;}
                long ci=numero("CI del paciente: ");
                if("1".equals(op)){JSONObject r=UDPClient.consultar(host,ci);verificar(r);System.out.println("Paciente: "+r.get("paciente"));medicamentos(r);}
                else tcp(host,ci,"2".equals(op));
            }catch(SocketTimeoutException e){System.out.println("La clinica no respondio dentro del tiempo limite. Puede volver a consultar.");}
            catch(ConnectException|PortUnreachableException e){System.out.println("No se pudo contactar con la clinica. Verifique que el servidor correspondiente este iniciado.");}
            catch(EOFException e){if(e.getMessage()==null)return;System.out.println(e.getMessage());}
            catch(Exception e){System.out.println("No se pudo completar la operacion: "+e.getMessage());}
        }
    }
}
