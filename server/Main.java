package server;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;



//java Main -t get -k name java Main -t set -k name -v "Sorabh Tomar" java Main -t set -k name -v Sorabh java Main -t get -k name
//java Main -in testSet.json
public class Main {
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();
    public static class Client{ // set/get/delete
        String type ;
        private String key;
        String value ;

        public Client(String type, String key ,String value){
            this.type = type;
            this.key=key;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
    public static class Response{
        String response;
        String reason ;
        String value;
        public Response(String response , String reason,String value){
            this.response = response;
            this.reason = reason ;
            this.value = value;
        }
    }
    public static void main(String[] args) {
        Map<String, String> map = new LinkedHashMap<>();
        Path filePath = Path.of("/Users/dianafatykhova/java/JSON Database with Java/JSON Database with Java/task/src/server/data/db.json");

        if (Files.exists(filePath)) {
            try {
                String existingJson = Files.readString(filePath);

                if (!existingJson.isBlank()) {
                    Gson gson = new Gson();

                    Type mapType = new TypeToken<Map<String, String>>() {}.getType();

                    Map<String, String> loadedMap =
                            gson.fromJson(existingJson, mapType);

                    if (loadedMap != null) {
                        map.putAll(loadedMap);
                    }
                }

            } catch (IOException e) {
                System.out.println("Error reading JSON file: " + e.getMessage());
            }
        }

        String address = "127.0.0.1";
        int port = 33333;

       try( ServerSocket server = new ServerSocket(port,50,InetAddress.getByName(address))){

           server.setSoTimeout(14999); //
           System.out.println("Server started!");
           GsonBuilder gsonBuilderr = new GsonBuilder();
           Gson gson = gsonBuilderr.disableHtmlEscaping().create();
           String file = "db.json";
           String jsonResponse;
           while(true) {
               try (Socket socket = server.accept(); DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                   String clientOutput = input.readUTF().trim();
                       while(clientOutput  == null){
                           break ;
                       }
                       Client request = gson.fromJson(clientOutput, Client.class);
                   //System.out.println("Recieved: " + clientOutput);
                       String result = "";
                       if (request.getType().equals("exit")) {
                           writeMapToFile(map);
                           Response response = new Response("OK", null,null);
                           jsonResponse = gson.toJson(response);//converting response to json format
                           output.writeUTF(jsonResponse);
                           return; //stops the entire server program
                       }
                       while (!request.getType().equals("exit")) {
                           if (request.getType().equals("set")) {
                               String res = set(request.getKey(), request.getValue(), map);
                               Response response = new Response(res, null, null);
                               jsonResponse = gson.toJson(response);
                               output.writeUTF(jsonResponse);
                               break;

                           } else if (request.getType().equals("get")) {
                               String res = get(request.getKey(), map);
                               if(res.equals("ERROR")) {
                                   Response response = new Response(res,"No such key",null);
                                   jsonResponse = gson.toJson(response);
                                   output.writeUTF(jsonResponse);
                                   break;
                               }
                               else {
                                   Response response = new Response("OK", null, res);
                                   jsonResponse = gson.toJson(response);
                                   output.writeUTF(jsonResponse);
                                   break;
                               }

                           } else if (request.getType().equals("delete")) {

                               String res = delete(request.getKey(), map);
                               if(res.equals("ERROR")){
                                   Response response = new Response(res, "No such key",null);
                                   jsonResponse = gson.toJson(response);
                                   output.writeUTF(jsonResponse);

                               }
                               else {
                                   Response response = new Response("OK", null, null);
                                   jsonResponse = gson.toJson(response);
                                   output.writeUTF(jsonResponse);
                               }
                               break;
                           }
                       }
                       output.writeUTF(result);// sent to the client
                       //System.out.println("Sent: A record # "+arrayClientInput[5]+" was sent!");

                   }catch (SocketTimeoutException e) {
                   // No client connected in time -> stop server (for tests like #1)
                   break;
               }


           }


       } catch (IOException e) {
           e.printStackTrace();
       }


    }
    public static synchronized void writeMapToFile(Map<String,String> map ) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        String jsonString = gson.toJson(map);

        try {
            Path filePath = Path.of("/Users/dianafatykhova/java/JSON Database with Java/JSON Database with Java/task/src/server/data/db.json");

            Files.writeString(
                    filePath,
                    jsonString + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            // System.out.println("Successfully wrote Map to JSON file: " + filePath.toAbsolutePath());

        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }
        public static synchronized String set ( String key, String text, Map<String,String> map){
        writeLock.lock();

            try{
                map.put(key, text);
                return "OK";

            }finally {
                writeLock.unlock();
            }


        }

        public static  String get ( String key, Map<String,String> map ){
        readLock.lock();
            //if (index < 1 || index > 1000) return "ERROR";
            try {
                if (!map.containsKey(key)) return "ERROR";
                return map.get(key);
            } catch (NullPointerException e) {
                return "ERROR";

            }finally{
                readLock.unlock();
            }
        }


        public static synchronized String delete ( String key, Map<String,String> map ){
        writeLock.lock();
           try {
               if (!map.containsKey(key)) {
                   return "ERROR";
               }
               map.remove(key);

               return "OK";
           }finally{
               writeLock.unlock();
           }
        }
    }


