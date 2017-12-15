/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CSE_BUET
 */
public class NetworkLayerServer {
    static int clientCount = 1;

    static ArrayList<Router> routers = new ArrayList<>();
    static RouterStateChanger stateChanger = null;
    static Set<String> activeClients = new HashSet<>();
    /**
     * Each map entry represents number of client end devices connected to the interface
     */
    static Map<IPAddress,Integer> clientInterfaces = new HashMap<>();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /**
         * Task: Maintain an active client list
         */



        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(1234);
        } catch (IOException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Server Ready: "+serverSocket.getInetAddress().getHostAddress());
        
        System.out.println("Creating router topology");
        
        readTopology();
        printRouters();
        
        /**
         * Initialize routing tables for all routers
         */
        initRoutingTables();


        // WELL GIMME SOME TIME TO INITIATE PAL! ^^
        try {
            Thread.sleep(2000);
        }catch (Exception e){}





        /**
         * Update routing table using DVR until convergence
         */
        DVR(7);
        printRoutingTables();

        try {
            Thread.sleep(2000);
        }catch (Exception e){}

        System.out.println("STATE CHANGER ACTIVATED");

        /**
         * Starts a new thread which turns on/off routers randomly depending on parameter Constants.LAMBDA
         */
        stateChanger = new RouterStateChanger();


//        while(true){
//            try {
//                Thread.sleep(1000);
//                routers.get(5).updateRoutingTable( routers.get(6), false);
//
//            }catch (Exception e){}
//        }
        
        while(true){
            try {
                Socket clientSock = serverSocket.accept();
                //ObjectOutputStream objOut = new ObjectOutputStream(clientSock.getOutputStream());
                System.out.println("Client attempted to connect");

//                EndDevice endDevice = getClientDeviceSetup();
//                objOut.writeObject(endDevice);

                EndDevice endDevice = getClientDeviceSetup();

                activeClients.add(endDevice.getIp().getString());

                //printing active clients
//                System.out.println("ACTIVE CLIENTS: ");
//                for(String active: activeClients){
//                    System.out.print(active + " --- ");
//                }
//                System.out.println("\n\n\n");
                new ServerThread(clientSock, endDevice);
            } catch (IOException ex) {
                Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
    
    public static void initRoutingTables()
    {
        for(int i=0;i<routers.size();i++)
        {
            routers.get(i).initiateRoutingTable();
        }
    }
    
    /**
     * Task: Implement Distance Vector Routing with Split Horizon and Forced Update
     */
    public static void DVR(int startingRouterId) {

        while(true) {

            //System.out.println("ECHO");
            boolean isUpdated = false;


            // putting the router whose routerID = startingRouterId at the first
            {
                ArrayList<Router> neighborRouters = new ArrayList<>();
                Vector<Integer> neighborIds = routers.get(startingRouterId - 1).getNeighborRouterIds();

                // get the neighbors of r
                for (int i = 0; i < routers.size(); i++) {
                    if (neighborIds.contains(routers.get(i).getRouterId())) {
                        neighborRouters.add(routers.get(i));
                    }
                }

                //update all neighbor t of r
                for (Router t : neighborRouters) {
                    if(t.getState() != false) {
                        if (t.updateRoutingTable(routers.get(startingRouterId - 1), true)) {   // true for forcedUpdate and splithorizon
                            isUpdated = true;
                        }
                    }

                }
            }



            for (Router r : routers) {
                if(r.getRouterId()!= startingRouterId) {

                    ArrayList<Router> neighborRouters = new ArrayList<>();
                    Vector<Integer> neighborIds = r.getNeighborRouterIds();

                    // get the neighbors of r
                    for (int i = 0; i < routers.size(); i++) {
                        if (neighborIds.contains(routers.get(i).getRouterId())) {
                            neighborRouters.add(routers.get(i));
                        }
                    }

                    //update all neighbor t of r
                    for (Router t : neighborRouters) {
                        if (!t.getState()) {
                            continue;
                        }
                        if (t.updateRoutingTable(r, true)) {   // true for forcedUpdate and splithorizon
                            isUpdated = true;
                        }
//                    System.out.println("UPDATING " + t.getRouterId() + " : neighbor of " + r.getRouterId());
//                    printRoutingTables();
//                    System.out.println("\n\n\n\n\n\n\n\n");
                    }
                }

            }


            if(!isUpdated) {
                //System.out.println("CONVERGENCE ACHIEVED");
                break;
            }
        }


        //printRoutingTables();


    }


    public static void printRoutingTable(Router r){
        System.out.println("ROUTING TABLE FOR ROUTER : " + r.getRouterId());
        for (RoutingTableEntry routingTableEntry1 : r.getRoutingTable()) {
            System.out.println(routingTableEntry1.toString());
        }
        System.out.println("-----------------------------------------------------------------------------");
    }
    public static void printRoutingTables(){
        //         PRINTING INITIATED ROUTING TABLE

        for(int i = 0 ; i < routers.size(); i+=1) {
            System.out.println("ROUTING TABLE FOR ROUTER : " + (i+1));
            for (RoutingTableEntry routingTableEntry1 : routers.get(i).getRoutingTable()) {
                System.out.println(routingTableEntry1.toString());
            }
            System.out.println("-----------------------------------------------------------------------------");
        }
    }





    
    /**
     * Task: Implement Distance Vector Routing without Split Horizon and Forced Update
     */
    public static void simpleDVR(int startingRouterId)
    {
        
    }
    
    
    public static EndDevice getClientDeviceSetup()
    {
        Random random = new Random();
        int r =Math.abs(random.nextInt(clientInterfaces.size()));
        
        System.out.println("Size: "+clientInterfaces.size()+"\n"+r);
        
        IPAddress ip=null;
        IPAddress gateway=null;
        
        int i=0;
        for (Map.Entry<IPAddress, Integer> entry : clientInterfaces.entrySet()) {
            IPAddress key = entry.getKey();
            Integer value = entry.getValue();
            if(i==r)
            {
                gateway = key;
                ip = new IPAddress(gateway.getBytes()[0]+"."+gateway.getBytes()[1]+"."+gateway.getBytes()[2]+"."+(value+2));
                value++;
                clientInterfaces.put(key, value);
                break;
            }
            i++;
        }
        
        EndDevice device = new EndDevice(ip, gateway);
        System.out.println("Device : "+ip+"::::"+gateway);
        return device;
    }



    public static void printRouters() {
        for(int i=0;i<routers.size();i++)
        {
            System.out.println("------------------\n"+routers.get(i));
        }
    }



    
    public static void readTopology()
    {
        Scanner inputFile = null;
        try {
            inputFile = new Scanner(new File("mytopology.txt"));
            //skip first 27 lines
            int skipLines = 27;
            for(int i=0;i<skipLines;i++)
            {
                inputFile.nextLine();
            }
            
            //start reading contents
            while(inputFile.hasNext())
            {
                inputFile.nextLine();
                int routerId;
                Vector<Integer> neighborRouters = new Vector<>();
                ArrayList<IPAddress> interfaceAddrs = new ArrayList<>();
                
                routerId = inputFile.nextInt();
                
                int count = inputFile.nextInt();
                for(int i=0;i<count;i++)
                {
                    neighborRouters.add(inputFile.nextInt());
                }
                count = inputFile.nextInt();
                inputFile.nextLine();
                
                for(int i=0;i<count;i++)
                {
                    String s = inputFile.nextLine();
                    //System.out.println(s);
                    IPAddress ip = new IPAddress(s);
                    interfaceAddrs.add(ip);
                    
                    /**
                     * First interface is always client interface
                     */
                    if(i==0)
                    {
                        //client interface is not connected to any end device yet
                        clientInterfaces.put(ip, 0);
                    }
                }
                Router router = new Router(routerId, neighborRouters, interfaceAddrs, routers);
                routers.add(router);
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
