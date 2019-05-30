package unimelb.bitbox;


import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONObject;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.Document;


import org.json.simple.JSONObject;


import unimelb.bitbox.util.Configuration;

public class Client
{
    private static String operation;

    private static String serverIP;
    private static int serverPort;

    private static String targetIP;
    private static int targetPort;

    private static String identityName;

    private static HashMap<String, Integer> List_Peers;

    //private Queue<FileSystemEvent> eventBuffer;


    private static Logger log = Logger.getLogger(Client.class.getName());
    public static void main( String[] args ) {



        /*
        System.setProperty("serverPort", serverPort);
        System.setProperty("clientPort", clientPort);
        System.setProperty("identityName", identityName);

         */

        log.info("BitBox Client starting...");
        //Configuration.getConfiguration();

        try(Socket socket = new Socket(serverIP, serverPort);){

            for (int i = 0; i < args.length; i++){

                //list_peers, connect_peer, disconnect_peer
                if(args[i].equals("-c")){
                    operation = args[i+1];
                }


                // The host & port of the peer who is going to establish the connection
                //e.g. server.com:3000
                if(args[i].equals("-s")){
                    serverIP = args[i+1].substring(0,args[i+1].indexOf(':'));
                    serverPort = Integer.parseInt(args[i+1].substring(args[i+1].indexOf(':')+1,
                            args[i+1].length()));
                }


                // The target connection peer
                //e.g. bigdata.cis.unimelb.edu.au:8500
                if(args[i].equals("-p")){
                    targetIP = args[i+1].substring(0,args[i+1].indexOf(':'));
                    targetPort = Integer.parseInt(args[i+1].substring(args[i+1].indexOf(':')+1,
                            args[i+1].length()));
                }

                // The clients idnetity name
                //e.g. aaron@krusty
                if(args[i].equals("-i")){
                    identityName = args[i+1];
                }

            }





            // Output and Input Stream
            DataInputStream input = new DataInputStream(socket.
                    getInputStream());
            DataOutputStream output = new DataOutputStream(socket.
                    getOutputStream());


            //list_peers, connect_peer, disconnect_peer
            switch(operation){
                case "list_peers":
                    output.writeUTF(JsonUtils.AUTH_REQUEST(identityName));
                    output.flush();
                    break;
                case "connect_peer":
                    output.writeUTF(JsonUtils.CONNECT_PEER_REQUEST(targetIP,targetPort));
                    output.flush();
                    break;
                case "disconnect_peer":
                    output.writeUTF(JsonUtils.DISCONNECT_PEER_REQUEST(targetIP,targetPort));
                    output.flush();
                    break;
                default:
                    System.out.println("Unknown command");
                    break;
            }


            //参考用，记得删除
            /*
            JSONObject newCommand = new JSONObject();
            newCommand.put("command_name", "Math");
            newCommand.put("method_name","multiply");
            newCommand.put("first_integer",3);
            newCommand.put("second_integer",2);

            System.out.println(newCommand.toJSONString());

                        // Send RMI to Server

            String jsonString = newCommand.toJSONString();

                        //sendEncrypted(jsonString,output);
            output.writeUTF(newCommand.toJSONString());

            output.flush();

            // Print out results received from server..
            String result = input.readUTF();
            System.out.println("Received from server: "+result);

            */


            // Read hello from server..
            String message = input.readUTF();
            System.out.println(message);




        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }

    }




    /*

    // Handles the operation
    private String translateEventToCommand(FileSystemEvent e){
        switch(e.event){
            case AUTH_REQUEST:
                return JsonUtils.FILE_CREATE_REQUEST(e.fileDescriptor,e.pathName);
            case AUTH_RESPONSE_SUCCESS:
                return JsonUtils.FILE_DELETE_REQUEST(e.fileDescriptor,e.pathName);
            case AUTH_RESPONSE_FAIL:
                return JsonUtils.FILE_MODIFY_REQUEST(e.fileDescriptor,e.pathName);
        }
        // technically speaking, the event should never returns a null string
        return null;
    }





    */




}


