/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class RouterStateChanger implements Runnable{

    Thread t;
    boolean dvrActivated = false;
    
    public RouterStateChanger() {
        t=new Thread(this);
        t.start();
    }
    
    @Override
    public void run() {
        Random random = new Random();
        while(true)
        {
            if(dvrActivated){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(RouterStateChanger.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.println("*** STATE CHANGER IS BLOCKED ***");
                continue; // if DVR is running then no state change is done;
            }

            double p = random.nextDouble();
            if(p<Constants.LAMBDA)
            {
                revertRandomRouter();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(RouterStateChanger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void revertRandomRouter()
    {
        /**
        * Randomly select a router and revert its state
        */
        Random random = new Random();
        int id = random.nextInt(NetworkLayerServer.routers.size());
        NetworkLayerServer.routers.get(id).revertState();
        System.out.println("State Changed; Router ID: "+NetworkLayerServer.routers.get(id).getRouterId());
        System.out.println("******************* ACTIVE ROUTERS **********************");
        for(Router router: NetworkLayerServer.routers){
            if(router.getState()) System.out.print(router.getRouterId() + " ");
        }
        System.out.println("\n\n\n");
    }

    public Thread getT() {
        return t;
    }

    public void setDvrActivated(boolean dvrActivated) {
        this.dvrActivated = dvrActivated;
    }
}
