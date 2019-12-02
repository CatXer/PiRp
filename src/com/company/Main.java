package com.company;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    private static String host = "jdroid.ru";
    private static int port = 2000;
    private static Socket s;
    private static Scanner reader;
    private static BufferedWriter writer;

    public static void main(String[] args) {

        if (Arrays.asList(args).contains("-i")) {
            if (args.length < 3 && args[1] != null && args[1].split(":").length == 2) {
                String[] hp = args[1].split(":");
                host = hp[0];
                port = Integer.parseInt(hp[1]);
            } else {
                System.err.println("PiRp: Err Invalid args, follow next syntax ->\n<host_or_ip>:<port>");
                System.exit(1);
            }
        } else if (Arrays.asList(args).contains("--help")) {
            System.out.println("java -jar CameraDev.jar -i <host_or_ip>:<port>");
        } else {
            System.err.println("PiRp: Warning no -i option -> default");
        }

        try {
            while (true) {
                try {
                    System.out.println("PiRp: Attempt to connect on <" + host + ":" + port + ">...");
                    s = new Socket(host, port);
                    reader = new Scanner(s.getInputStream());
                    writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                } catch (IOException e) {
                    System.err.println("PiRp: Err can`t connect on <" + host + ":" + port + "> abort.");
                    System.exit(1);
                }

                System.out.println("PiRp: Connected to <" + host + ":" + port + ">");
                System.out.println("PiRp[Server]: Command -> " + reader.nextLine());

                writer.write("[{\"client_type\":\"1\", \"method\":\"login\", \"device_id\":\"4\"}]\n");
                writer.flush();
                System.out.println("PiRp[Server]: " + reader.nextLine());

                while (!s.isClosed()) {
                    Thread.sleep(3000);
                    String[] request;
                    try {
                        request = reader.nextLine().split("=");
                    } catch (NoSuchElementException e) {
                        System.out.println("PiRp[Server]: Timeout -> disconnecting");
                        break;
                    }
                    System.out.println("PiRp[Server]: " + Arrays.toString(request));
                    switch (request[0]) {
                        //println("i do: ${request[0]} with args: ${request[1]} for user with id: ${request[2]}")
                        case "6": {
                            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "raspistill -w 640 -h 480 -q 75 -o /home/pi/Pictures/Camera/snap2.jpg");
                            pb.inheritIO();
                            pb.start();

                            Thread.sleep(8000);

                            InputStream is = new FileInputStream(new File("/home/pi/Pictures/Camera/snap2.jpg"));
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int nRead;
                            byte[] data = new byte[16384];
                            while ((nRead = is.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }

                            String allBytes = Base64.getEncoder().encodeToString(buffer.toByteArray());

                            writer.write("[{\"method\":\"sent_to_user\", \"user_id\":\"" + request[2] + "\", \"response\":\"" + allBytes + "\"}]\n");
                            writer.flush();
                            break;
                        }
                        case "7": {

                            writer.write("[{\"method\":\"disconnect\"}]\n");
                            writer.flush();

                            reader.close();
                            writer.close();
                            s.close();
                            System.out.println("PiRp: bye");
                            System.exit(0);
                        }
                    }
                }
                reader.close();
                writer.close();
                s.close();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}