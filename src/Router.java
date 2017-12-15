/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import sun.nio.ch.Net;

import java.util.*;

/**
 *
 * @author samsung
 */
public class Router {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<Router> allRoutersInTheTopology;
    private ArrayList<IPAddress> interfaceAddrs;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private Vector<Integer> neighborRouterIds;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state

    public Router() {
        interfaceAddrs = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIds = new Vector<>();
        
        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;
        
        numberOfInterfaces = 0;
    }
    
    public Router(int routerId, Vector<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddrs, ArrayList<Router> routers)
    {
        this.routerId = routerId;
        this.interfaceAddrs = interfaceAddrs;
        this.neighborRouterIds = neighborRouters;
        routingTable = new ArrayList<>();
        allRoutersInTheTopology = routers;
        
        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;
        
        numberOfInterfaces = this.interfaceAddrs.size();
    }

    @Override
    public String toString() {
        String temp = "";
        temp+="Router ID: "+routerId+"\n";
        temp+="Intefaces: \n";
        temp+= "-----------STATE: " + this.getState() + "\n";
        for(int i=0;i<numberOfInterfaces;i++)
        {
            temp+=interfaceAddrs.get(i).getString()+"\t";
        }
        temp+="\n";
        temp+="Neighbors: \n";
        for(int i=0;i<neighborRouterIds.size();i++)
        {
            temp+=neighborRouterIds.get(i)+"\t";
        }
        return temp;
    }
    
    
    
    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable()
    {
        int totalRouters = NetworkLayerServer.routers.size();
        if(routingTable.size() == 0) {
            for (int i = 0; i < totalRouters; i++) {
                RoutingTableEntry routingTableEntry = new RoutingTableEntry(NetworkLayerServer.routers.get(i).getRouterId(),
                        Constants.INFTY, Constants.INFTY);
                routingTable.add(routingTableEntry);
            }
        }

        if (this.getState()) {

            for(int i = 0; i < routingTable.size(); i++){
                if(routingTable.get(i).getRouterId() == this.getRouterId()){
                    routingTable.get(i).setDistance(0);
                    routingTable.get(i).setGatewayRouterId(this.getRouterId());
                }
                if(neighborRouterIds.contains(routingTable.get(i).getRouterId())
                        && NetworkLayerServer.routers.get(routingTable.get(i).getRouterId()-1).getState()){
                    routingTable.get(i).setDistance(1);
                    routingTable.get(i).setGatewayRouterId(routingTable.get(i).getRouterId());
                }
            }

        }



//         PRINTING INITIATED ROUTING TABLE
//        System.out.println("ROUTING TABLE FOR ROUTER : " + this.getRouterId());
//        for (RoutingTableEntry routingTableEntry1 : routingTable) {
//            System.out.println(routingTableEntry1.toString());
//        }
//        System.out.println("-----------------------------------------------------------------------------");




    }
    
    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable()
    {

        for(RoutingTableEntry routingTableEntry: routingTable){
            routingTableEntry.setGatewayRouterId(Constants.INFTY);
            routingTableEntry.setDistance(Constants.INFTY);
        }

    }
    
    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor 
     */
    public boolean updateRoutingTable(Router neighbor, boolean splitAndForced)
    {
        ArrayList<RoutingTableEntry> neighborRoutingTable = neighbor.getRoutingTable();


        boolean flag = false;

        if(splitAndForced){



            // UPDATE WITH SPLIT HORIZON AND FORCED UPDATE
            for (int i = 0; i < routingTable.size(); i++) {
                if(routingTable.get(i).getRouterId() != neighbor.getRouterId()) {

                    double distanceFromNeighbor = neighborRoutingTable.get(i).getDistance();
                    double distanceFromThisRouter = routingTable.get(i).getDistance();
                    int nextHopFromNeighbor = neighborRoutingTable.get(i).getGatewayRouterId();
                    int nextHopFromThisRouter = routingTable.get(i).getGatewayRouterId();

                    double d = routingTable.get(neighbor.getRouterId() - 1).getDistance() + distanceFromNeighbor;
                    if ((d < distanceFromThisRouter && this.getRouterId() != nextHopFromNeighbor) ||
                            nextHopFromThisRouter == neighbor.getRouterId()) {
                        if(d == routingTable.get(i).getDistance()) {
                            flag = false;
                        }else {
                            routingTable.get(i).setDistance(d);
                            routingTable.get(i).setGatewayRouterId(neighbor.getRouterId());
//                        System.out.println("**********************************update dude***************************************************");
//                        System.out.println("updating entry of " + routingTable.get(i).getRouterId() + " routing table of " + this.getRouterId() + " when neighbor is " + neighbor.getRouterId());
                            flag = true;
                        }

                    }
                }

            }

        }
        else {

            // SIMPLE UPDATE
            for (int i = 0; i < routingTable.size(); i++) {
                if(routingTable.get(i).getRouterId() != neighbor.getRouterId()) {
                    double distanceFromNeighbor = neighborRoutingTable.get(i).getDistance();
                    double distanceFromThisRouter = routingTable.get(i).getDistance();
                    int nextHopFromNeighbor = neighborRoutingTable.get(i).getGatewayRouterId();
                    int nextHopFromThisRouter = routingTable.get(i).getGatewayRouterId();

                    double d = routingTable.get(neighbor.getRouterId() - 1).getDistance() + distanceFromNeighbor;
                    if (d < distanceFromThisRouter) {
                        routingTable.get(i).setDistance(d);
                        routingTable.get(i).setGatewayRouterId(neighbor.getRouterId());
                        flag = true;
                    }
                }

            }

        }
        return flag;
    }



    
    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState()
    {
        state=!state;
        if(state==true) this.initiateRoutingTable();
        else this.clearRoutingTable();
    }
    
    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddrs() {
        return interfaceAddrs;
    }

    public void setInterfaceAddrs(ArrayList<IPAddress> interfaceAddrs) {
        this.interfaceAddrs = interfaceAddrs;
        numberOfInterfaces = this.interfaceAddrs.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public Vector<Integer> getNeighborRouterIds() {
        return neighborRouterIds;
    }

    public void setNeighborRouterIds(Vector<Integer> neighborRouterIds) {
        this.neighborRouterIds = neighborRouterIds;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }
    
    
}
