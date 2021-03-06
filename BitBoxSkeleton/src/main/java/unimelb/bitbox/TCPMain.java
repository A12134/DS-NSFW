package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class TCPMain {

    private static Logger log = Logger.getLogger(TCPMain.class.getName());
    private HashMap<String, Connection> Incomming;
    private HashMap<String, Connection> Outgoing;
    private ServerSocket listenSocket;
    private Queue<FileSystemEvent> eventBuffer;
    private boolean serverActive, communicationActive;

    // listening for incoming connections
    private Thread server = new Thread(){
        @Override
        public void run(){
            while(serverActive){
                try{
                    Socket incommingConnection = listenSocket.accept();
                    Connection c = new Connection(incommingConnection);
                    if(c.flagActive){
                        Incomming.put(c.getPeerInfo().toString(), c);
                    }
                }catch(IOException e){
                    log.warning(e.getMessage());
                }
            }
        }
    };

    // broadcast File system event to other peers
    private Thread communication = new Thread(){
        @Override
        public void run(){
            while(communicationActive){
                // when eventBuffer is not empty, broadcast the command to all connected peers
                synchronized (this) {
                    while (eventBuffer.size() > 0) {
                        FileSystemEvent event = eventBuffer.poll();
                        log.info("broadcast command " + event.event);
                        String command = translateEventToCommand(event);
                        Incomming.forEach((key, value) -> value.sendCommand(command));
                        Outgoing.forEach((key, value) -> value.sendCommand(command));
                    }
                }
            }

        }
    };

    public boolean peerConnectWith(String ip, int port){
        HostPort p = new HostPort(ip, port);
        if(connectionExist(p)){
            log.info("Already connected with " + p.toString());
            return true;
        } else {
            Connection c = new Connection(p, "tcp");
            c.TCPmainPatch(this);
            if(c.flagActive){
                Outgoing.put(p.toString(), c);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean peerDisconnectWith(String ip, int port){
        HostPort p = new HostPort(ip,port);
        if (!connectionExist(p)){
            log.info("Connection with " + p.toString() + " does not exist.");
            return true;
        } else {
            if (Incomming.containsKey(p.toString())){
                return Incomming.get(p.toString()).disconnect();
            } else {
                return Outgoing.get(p.toString()).disconnect();
            }
        }
    }

    public boolean forceDisconnection(String ip, int port){
        HostPort p = new HostPort(ip, port);
        if(!connectionExist(p)){
            log.info("Connection with " + p.toString() + " does not exist.");
            return true;
        } else{
            if (Incomming.containsKey(p.toString())){
                Incomming.get(p.toString()).closeSocket();
            } else {
                Outgoing.get(p.toString()).closeSocket();
            }
            return true;
        }
    }



    public TCPMain(){
        // initalize the event buffer
        eventBuffer = new LinkedList<>();
        Incomming = new HashMap<>();
        Outgoing = new HashMap<>();
        Connection.TCPmain = this;

        // initialize server socket
        try{
            listenSocket = new ServerSocket(JsonUtils.getSelfHostPort().port);
        }catch(IOException e){
            log.warning(e.getMessage());
        }

        // Start Connect with peers
        String[] peersStr = Configuration.getConfigurationValue("peers").split(",");
        for (String peer : peersStr){
            HostPort tmp = new HostPort(peer);
            // if connection does not exist
            if(!connectionExist(tmp)){
                // try connect with the peer
                Connection c = new Connection(tmp, "tcp");
                c.TCPmainPatch(this);
                // if connect successful, add to Outgoing hashmap
                if(c.flagActive){
                    Outgoing.put(tmp.toString(), c);
                }
            }
        }

        if(listenSocket != null) {
            // start listening for incoming connection
            serverActive = true;
            server.start();

            // start the thread for managing all connections
            communicationActive = true;
            communication.start();
        } else {
            log.warning("Please change the port and restart the bitbox peer!");
        }
    }

    public boolean connectionExist(HostPort tmp){
        return Incomming.containsKey(tmp.toString()) || Outgoing.containsKey(tmp.toString());
    }

    public boolean maximumConnectionReached(){
        return Incomming.size() >= Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
    }

    public void removeConnection(String key){
        if(Incomming.containsKey(key)){
            Incomming.remove(key);
        } else Outgoing.remove(key);
    }

    public void disconnectAll(){
        Incomming.forEach((key, value) -> value.closeSocket());
        Outgoing.forEach((key,value) -> value.closeSocket());
    }

    public ArrayList<String> getAllConnections(){
        ArrayList<String> connections = new ArrayList<>();

        Incomming.forEach((key, value) -> connections.add(key));
        Outgoing.forEach((key, value) -> connections.add(key));
        return connections;

    }

    // Handles the file system event
    private String translateEventToCommand(FileSystemEvent e){
        switch(e.event){
            case FILE_CREATE:
                return JsonUtils.FILE_CREATE_REQUEST(e.fileDescriptor,e.pathName);
            case FILE_DELETE:
                return JsonUtils.FILE_DELETE_REQUEST(e.fileDescriptor,e.pathName);
            case FILE_MODIFY:
                return JsonUtils.FILE_MODIFY_REQUEST(e.fileDescriptor,e.pathName);
            case DIRECTORY_CREATE:
                return JsonUtils.DIRECTORY_CREATE_REQUEST(e.pathName);
            case DIRECTORY_DELETE:
                return JsonUtils.DIRECTORY_DELETE_REQUEST(e.pathName);
        }

        // technically speaking, the event should never returns a null string
        return null;
    }

    // add event to eventBuffer
    public void addEvent(FileSystemEvent event){
        synchronized (this) {
            this.eventBuffer.add(event);
        }
    }
}
