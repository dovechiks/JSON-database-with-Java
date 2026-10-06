package client;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Scanner;

//java Main -in testSet.json
public class Main {
    static class Args {
        @Parameter(names = "-in", description = "File")
        String file;

        @Parameter(names = "-t", description = "Type")
        String type;

        @Parameter(names = "-k", description = "Key")
        String key;

        @Parameter(names = "-v", description = "Optional message")
        String value;

        @Parameter(names = {"-h", "--help"}, help = true, description = "Show help")
        boolean help;
    }
    public static class CaseOne{ // set/get/delete
        String type ;
        private String key;
        String value ;

        public CaseOne(String type, String key ,String value ){
            this.type = type;
            this.key=key;
            this.value = value;

        }
    }

    public class JsonFileReader {
        public static String readJsonFromFile(String fileName) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String jsonLine;

                // Reads until the newline character. Returns null when the end of the file is reached.
                while ((jsonLine = reader.readLine()) != null) {
                    return jsonLine;
                }

            } catch (IOException e) {
                System.out.println("Attempting to read file from: " + fileName);
                return null;
            }
            return null;

        }
    }


    public static void main(String[] argm) {
        Scanner sc = new Scanner(System.in);

        String line = sc.nextLine();
        while (true) {

            String[] firstCase = new String[8];// when we have message
            String text = "";
            String[] tokens;
            String[] fileCase = new String[4];
            String content0fFile = null;
            String[] secondCase = new String[6]; // when we don't have message , have index
            String[] tempArray = line.trim().split("\\s+"); /* {java, main, -t , set , -i[4] , 1[5] , -m[6],"Hello, world"}
            OR {java, main, -in, test/get/set/delete.json}
            */
            if(tempArray.length == 4 && tempArray[2].equals("-in")){
                String fileName = tempArray[3];
                String path = System.getProperty("user.dir") + "/JSON Database with Java/task/src/client/data/" + fileName;
                if (Files.exists(Path.of(path))) {
                    // read existing JSON
                    // convert JSON → LinkedHashMap
                    content0fFile  = JsonFileReader.readJsonFromFile(path);

                } else {
                    try {
                        Files.createFile(Path.of(path));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
               // Path filePath = Path.of("JSON Database with Java","task","src", "client", "data",);

                /*try {
                    // 1. Ensure the parent directories (src/server/data) exist
                    if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                        Files.createDirectories(filePath.getParent());
                        System.out.println("Created missing directories.");
                    }

                    // 2. Ensure the db.json file itself exists. If not, create an empty one.
                    if (!Files.exists(filePath)) {
                        Files.createFile(filePath);
                        // Write an empty JSON object so it's valid from the start
                        Files.writeString(filePath, "{}");
                        System.out.println("Created new empty db.json file.");
                    }

                    // 3. Now safely read the existing JSON file
                    String existingJson = Files.readString(filePath);
                    Gson gson = new Gson();
                    Map<String, String> loadedMap = gson.fromJson(existingJson, Map.class);

                } catch (IOException e) {
                    System.out.println("Error initializing database file: " + e.getMessage());
                }*/
            }

            if (tempArray.length > 7 && tempArray[6].equals("-v")) { // if there is no message ->skip
                for (int i = 0; i < tempArray.length; i++) {
                    if (i >= 7) {
                        text += " " + tempArray[i];
                        continue;
                    }
                    if (i < 7) {
                        firstCase[i] = tempArray[i];
                    }
                }
                firstCase[7] = text;
                tokens = Arrays.copyOf(firstCase, firstCase.length);

            }
            else if (tempArray.length > 5 &&  tempArray[4].equals("-i")) { // second case when we do have an index
                for (int i = 0; i < tempArray.length; i++) {
                    secondCase[i] = tempArray[i];
                }
                tokens = Arrays.copyOf(secondCase, secondCase.length);

            }//rewrite this part so the socket will be able to manage the input is without index
            else{
                tokens = Arrays.copyOf(tempArray,tempArray.length);
            }


            int start = 2;
            //String[] tokens = Arrays.copyOf(firstCase, firstCase.length);

            String[] argv = Arrays.copyOfRange(tokens, start, tokens.length);

            Args args = new Args();
            JCommander jc = JCommander.newBuilder().addObject(args).programName("java Main").build();

            try {
                jc.parse(argv);

                if (args.help) { jc.usage(); return; }


            } catch (ParameterException e) {
                System.err.println(e.getMessage());
                jc.usage();
                System.exit(3);
            }


            //server.Main.main(args);
            String address = "127.0.0.1";
            int port = 33333;
            

            System.out.println("Client started!");
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.disableHtmlEscaping().create();
            String jsonCsOne;

            try (
                    Socket socket = new Socket(InetAddress.getByName(address), port)) {

                try (DataInputStream input = new DataInputStream(socket.getInputStream());
                     DataOutputStream output = new DataOutputStream(socket.getOutputStream())
                ) {
                    if ("exit".equals(args.type)) {
                        CaseOne csOne = new CaseOne(args.type, null,null);
                        jsonCsOne = gson.toJson(csOne);
                        output.writeUTF(jsonCsOne);
                        System.out.println("Sent: " + jsonCsOne);
                        String outputFromServer = input.readUTF();
                        System.out.println("Received: " + outputFromServer);
                        break;

                    } else if (args.value != null) {
                        CaseOne csOne = new CaseOne(args.type, String.valueOf(args.key),args.value);
                        jsonCsOne = gson.toJson(csOne);

                    }else if("delete".equals(args.type) || "get".equals(args.type)){
                        CaseOne csOne = new CaseOne(args.type,String.valueOf(args.key), null); // delete is the same as get
                        jsonCsOne = gson.toJson(csOne);

                    }
                    else {
                        jsonCsOne = content0fFile;
                    }
                    output.writeUTF(jsonCsOne);
                    System.out.println("Sent: " + jsonCsOne);
                   String outputFromServer = input.readUTF();
                    System.out.println("Received: " + outputFromServer);
                    line = sc.nextLine();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (
                    Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

}


