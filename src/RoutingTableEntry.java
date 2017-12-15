/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author samsung
 */
public class RoutingTableEntry {
    private int routerId;
    private double distance;
    private int gatewayRouterId;

    public RoutingTableEntry(int routerId, double distance, int gatewayRouterId) {
        this.routerId = routerId;
        this.distance = distance;
        this.gatewayRouterId = gatewayRouterId;
    }

    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getGatewayRouterId() {
        return gatewayRouterId;
    }

    public void setGatewayRouterId(int gatewayRouterId) {
        this.gatewayRouterId = gatewayRouterId;
    }


    @Override
    public String toString() {
        String debug = routerId + "  ---  " + distance + "  ---  "  + gatewayRouterId ;
        return debug;
    }

    @Override
    public boolean equals(Object obj) {
        RoutingTableEntry r = (RoutingTableEntry) obj;
        return ((this.routerId == r.routerId) &&
                (this.distance == r.distance) &&
                (this.gatewayRouterId == r.gatewayRouterId));
    }
}
