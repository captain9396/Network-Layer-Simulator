/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import com.sun.jmx.remote.internal.ArrayQueue;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class ServerThread implements Runnable {
    private Thread t;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private EndDevice endDevice;
    private Queue<Integer> route = new LinkedList<>();

    
    public ServerThread(Socket socket, EndDevice endDevice){

        this.socket = socket;
        this.endDevice = endDevice;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());


//            System.out.println("RECEIVER LIST ^^^");
//
//            for(String s: NetworkLayerServer.activeClients){
//                if(!s.equals(endDevice.getIp().getString())) {
//                    randomActiveEnds.add(s);
//                    System.out.println(s);
//                }
//
//            }
//            System.out.println("\n\n\n");
            
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Server Ready for client "+ NetworkLayerServer.clientCount);
        NetworkLayerServer.clientCount++;
        
        t=new Thread(this);
        t.start();
    }

    @Override
    public void run() {

        try {
            output.writeObject(endDevice);
        }catch (Exception e){}

        /**
         * Synchronize actions with client.
         */
        /*
        Tasks:
        1. Upon receiving a packet server will assign a recipient.
        [Also modify packet to add destination]
        2. call deliverPacket(packet)
        3. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        4. Either send acknowledgement with number of hops or send failure message back to client
        */

        String messageString = "";

        while(true) {
            try {
                messageString = input.readUTF();
                if(messageString.equalsIgnoreCase("yes") || messageString.equalsIgnoreCase("y")){
//                    System.out.println("CLIENT " + endDevice.getIp() + " SENDING PACKETS");
                    Random random = new Random();

                    ArrayList<String> randomActiveEnds = new ArrayList<>();
                    for(String s: NetworkLayerServer.activeClients){
                        if(!s.equals(endDevice.getIp().getString())) {
                            randomActiveEnds.add(s);
                            System.out.println(s);
                        }
                    }


                    double drop = 0.0;
                    double success = 0.0;
                    double hop = 0.0;
                    for(int i = 0; i < Constants.totalPackets; i++){
                        int index = random.nextInt(10000);
                        int ind = index % randomActiveEnds.size();


                        String randomReceiver = randomActiveEnds.get(ind);

                        output.writeUTF(randomReceiver);
                        output.flush();

                        Packet packet = (Packet)input.readObject();

                        Pair<Boolean, Double> deliver = deliverPacket(packet);

                        if(deliver.getKey()){
                            System.out.println("## PACKET " + i + " DELIVERED TO DESTINATION");
                            hop += deliver.getValue();
                            success++;
                            output.writeUTF("$$ sent :: hop count = " + String.valueOf(hop));
                            output.flush();
                        }
                        else {
                            System.out.println("!!! PACKET " + i + " DROPPED");
                            drop++;
                            output.writeUTF("not sent");
                            output.flush();
                        }



                    }





                    System.out.println("drop rate = " + (drop / 100.0));
                    System.out.println("Average Hop Count = " + (hop / success));





                }
                else if(messageString.equals("going offline")){
                    NetworkLayerServer.activeClients.remove(endDevice.getIp().getString());
                    break;
                }




            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    /**
     * Returns true if successfully delivered
     * Returns false if packet is dropped
     * @param p
     * @return 
     */



    public Pair<Boolean, Double> deliverPacket(Packet p) {

        Router sourceRouter = null;
        Router destinationRouter = null;

        double hop = 0.0;
        // FINDING ROUTER ID OF SOURCE ROUTER
        for (Router r : NetworkLayerServer.routers) {

            for (IPAddress ipAddress : r.getInterfaceAddrs()) {
                String networkAddrofInterface = ipAddress.getString().substring(0, ipAddress.getString().lastIndexOf('.'));
                String networkAddrofSource = p.getSourceIP().getString().substring(0, p.getSourceIP().getString().lastIndexOf('.'));
                if (networkAddrofSource.equals(networkAddrofInterface)) {
                    sourceRouter = r;
                    break;
                }
            }
        }


        // FINDING ROUTER ID OF DESTINATION ROUTER
        for (Router r : NetworkLayerServer.routers) {

            for (IPAddress ipAddress : r.getInterfaceAddrs()) {
                String networkAddrofInterface = ipAddress.getString().substring(0, ipAddress.getString().lastIndexOf('.'));
                String networkAddrofDestination = p.getDestinationIP().getString().substring(0, p.getDestinationIP().getString().lastIndexOf('.'));
                if (networkAddrofDestination.equals(networkAddrofInterface)) {
                    destinationRouter = r;
                    break;
                }
            }
        }

        if(sourceRouter.getRouterId() == destinationRouter.getRouterId()) return new Pair(true, 0.0);

//        System.out.println("SOURCE ROUTER: " + sourceRouter.getRouterId() + "" +
//                "  --- DESTINATION ROUTER: " + destinationRouter.getRouterId());

        Router s = sourceRouter;
        route.add(s.getRouterId());
        Router prev = null;

        while(s.getRouterId() != destinationRouter.getRouterId()){
            if(s.getState() == false){
                if(prev != null){
                    int sid = s.getRouterId() - 1;
                    prev.getRoutingTable().get(sid).setGatewayRouterId(Constants.INFTY);
                    prev.getRoutingTable().get(sid).setDistance(Constants.INFTY);
                    NetworkLayerServer.stateChanger.setDvrActivated(true);

                    //NetworkLayerServer.printRoutingTable(prev);

                    NetworkLayerServer.DVR(prev.getRouterId());
                    NetworkLayerServer.stateChanger.setDvrActivated(false);


                }
                route.clear();
                return new Pair(false,0.0);
            }

            if(prev != null) {
                if (s.getRoutingTable().get(prev.getRouterId() - 1).getDistance() == Constants.INFTY) {
                    s.getRoutingTable().get(prev.getRouterId() - 1).setDistance(1);
                    NetworkLayerServer.stateChanger.setDvrActivated(true);
                    NetworkLayerServer.DVR(s.getRouterId());
                    NetworkLayerServer.stateChanger.setDvrActivated(false);
                }
            }


            int next = 0;
            try {
                next = s.getRoutingTable().get(destinationRouter.getRouterId() - 1).getGatewayRouterId() - 1;
                prev = s;
                s = NetworkLayerServer.routers.get(next);
                hop += (1.0);
                route.add(s.getRouterId());
               // System.out.println("HELLOP " + hop);
            }catch(Exception e){
                NetworkLayerServer.stateChanger.setDvrActivated(true);
                NetworkLayerServer.DVR(s.getRouterId());
                NetworkLayerServer.stateChanger.setDvrActivated(false);
                route.clear();
                return new Pair(false, 0.0);
            }



        }


//


        while(!route.isEmpty()) {
            System.out.print(route.poll() + " >> ");
        }
        System.out.println();
        return new Pair(true, hop);
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }

}
