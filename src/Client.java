/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class Client {
    public static void main(String[] args)
    {
        Socket socket = null;
        ObjectInputStream input = null;
        ObjectOutputStream output = null;
        EndDevice myEndDevice;
        Scanner scanner = new Scanner(System.in);
        
        try {
            socket = new Socket("localhost", 1234);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

        
            System.out.println("Connected to server");

            // receive end device configuration
            myEndDevice = (EndDevice) input.readObject();

            String message = "";

//            System.out.println("CLIENT GOT ENDDEVICE: ip = " + myEndDevice.getIp() +
//                    "  -- gateway = " + myEndDevice.getGateway());

            while(true) {
                System.out.println("DO YOU WANT TO SEND PACKETS? ");
                String prompt = scanner.nextLine();
                if(prompt.equalsIgnoreCase("yes") || prompt.equalsIgnoreCase("y")){
                    message = prompt;
                    output.writeUTF(message);
                    output.flush();


                    for(int i = 0 ; i < Constants.totalPackets; i++){
                        String randomReceiver = input.readUTF();

                        Packet packet;
                        if(i == 20){
                            packet = new Packet("HELLO WORLD", "SHOW_ROUTE", myEndDevice.getIp(), new IPAddress(randomReceiver));
                            NetworkLayerServer.printRoutingTables();
                        }
                        else {
                            packet = new Packet("HELLO WORLD", "", myEndDevice.getIp(), new IPAddress(randomReceiver));
                        }


//                        System.out.println("SENDING PACKET TO DESTINATION " + randomReceiver);
                        output.writeObject(packet);
                        output.flush();


                        String msg = input.readUTF();
                        System.out.println(msg);

                    }




                    /*
                    for(int i=0;i<100;i++)
                    4. {
                    5.      Generate a random message
                    6.      [Adjustment in ServerThread.java] Assign a random receiver from active client list
                    7.      if(i==20)
                    8.      {
                    9.            Send the messageto server and a special request "SHOW_ROUTE"
                    10.           Display routing path, hop count and routing table of each router [You need to receive
                                        all the required info from the server in response to "SHOW_ROUTE" request]
                    11.     }
                    12.     else
                    13.     {
                    14.           Simply send the message and recipient IP address to server.
                    15.     }
                    16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                                Otherwise, client will get a failure message [dropped packet]
                    17. }
                     */
                }
                else {
                    //disonnecting
                    output.writeUTF("going offline");
                    output.flush();
                    break;
                }
            }



        } catch (Exception ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        /**
         * Tasks
         *
        /*
        1. Receive EndDevice configuration from server
        2. [Adjustment in NetworkLayerServer.java: Server internally
            handles a list of active clients.]        
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      [Adjustment in ServerThread.java] Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the messageto server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive 
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.   
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */
    }
}
