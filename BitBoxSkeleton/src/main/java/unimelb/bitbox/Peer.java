package unimelb.bitbox;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;


import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;



public class Peer 
{

    private ResponseHandler rh;
    private static String advertisedName;
    private static int PeerPort;

    private static HashMap<Integer, Integer> List_Client;

    //private static ServerMain s;
    private static ServerMain s;

    // Identifies the user number con
    private static int counter = 0;



    private static HashMap<String, Integer> List_Peers;



    private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {

        //Information of this Peer
        advertisedName = Configuration.getConfigurationValue("advertisedName");
        PeerPort = Integer.parseInt(Configuration.getConfigurationValue("port"));




        //java -cp bitbox.jar unimelb.bitbox.Peer
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();


        try{
            s = new ServerMain();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }



        ServerSocketFactory factory = ServerSocketFactory.getDefault();

        try(ServerSocket server = factory.createServerSocket(PeerPort)){

            // Wait for connections.
            while(true){


                Socket client = server.accept();

                // Start a new thread for a connection
                Thread t = new Thread(() -> serveClient(client));
                t.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }



    }


    private static void serveClient(Socket client){
        try(Socket clientSocket = client){

            // The JSON Parser
            JSONParser parser = new JSONParser();
            // Input stream
            DataInputStream input = new DataInputStream(clientSocket.
                    getInputStream());
            // Output Stream
            DataOutputStream output = new DataOutputStream(clientSocket.
                    getOutputStream());


            //read Auth requet from client
            String message = input.readUTF();
            JSONObject command = (JSONObject) parser.parse(message);
            String identifyName = (String) command.get("identity");


            //测试使用，记得更改！！在此处加上密码的算法
            String encrKey = "TEST";
            boolean status = true;
            // Automatically send Auth request first
            output.writeUTF(JsonUtils.AUTH_RESPONSE_SUCCESS(encrKey,identifyName, status));
            output.flush();
            System.out.println("Auth public key success!");


            // Attempt to convert read data to JSON
            message = input.readUTF();
            System.out.println(message);

            //message = decryptMessage(message);

            // Receive connection request from the client
            command = (JSONObject) parser.parse(message);
            String request = (String) command.get("command");
            String targetIP = (String) command.get("host");
            Integer targetPort = Integer.parseInt(command.get("port").toString());
            boolean connectStatus = false;
            boolean disconnectStatus = false;


            switch (request) {
                case "CONNECT_PEER_REQUEST":

                    System.out.println("=================Connect peer request===============");
                    s.connectTo(targetIP, targetPort);
                    connectStatus = true;

                    if(connectStatus){
                        output.writeUTF(JsonUtils.CONNECT_PEER_RESOPONSE_SUCCESS(targetIP,targetPort,connectStatus));
                        output.flush();
                        List_Peers.put(targetIP,targetPort);

                    }else{
                        output.writeUTF(JsonUtils.CONNECT_PEER_RESOPONSE_FAIL(targetIP,targetPort,connectStatus));
                        output.flush();
                    }
                    break;
                case "DISCONNECT_PEER_REQUEST":

                    System.out.println("=================Disconnect peer request===============");
                    s.disconnectTo(targetIP, targetPort);
                    disconnectStatus = true;

                    if(disconnectStatus){
                        output.writeUTF(JsonUtils.DISCONNECT_PEER_RESOPONSE_SUCCESS(targetIP, targetPort, disconnectStatus));
                        output.flush();
                        List_Peers.remove(targetIP,targetPort);
                    }else{
                        output.writeUTF(JsonUtils.DISCONNECT_PEER_RESOPONSE_FAIL(targetIP, targetPort, disconnectStatus));
                        output.flush();
                    }


                    break;
                case "LIST_PEERS_REQUEST":

                    System.out.println("=================List peers request================");
                    output.writeUTF(JsonUtils.LIST_PEERS_RESPOND(List_Peers));
                    output.flush();

                    break;
                default:
                    System.out.println("Unknown command from client");
                    break;
            }



        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }









}
