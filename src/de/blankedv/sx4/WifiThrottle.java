/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import static de.blankedv.sx4.SX4.*;
import javax.swing.text.BadLocationException;

/**
 *
 * @author mblank
 * 
 * Nov 2018 : Simplified - only for Funkr2 Throttles (and only receive multicast lanbahn messages)
 * 
 */
public class WifiThrottle  {

    private static final int LANBAHN_PORT = 27027;
    private static final String LANBAHN_GROUP = "239.200.201.250";
    private static String TEXT_ENCODING = "UTF8";
    protected InetAddress mgroup;
    protected MulticastSocket multicastsocket;
    static LanbahnServer lbServer;

    // Preferences
    protected Thread t;
    //protected RegisterJMDNSService serv;
    byte[] buf = new byte[1000];

    private long announceTimer = 0;

    /**
     * Creates new form LanbahnUI
     */
    public WifiThrottle() {
       
        if (!myips.isEmpty()) {
            try {
                multicastsocket = new MulticastSocket(LANBAHN_PORT);
                multicastsocket.setInterface(myips.get(0));
                mgroup = InetAddress.getByName(LANBAHN_GROUP);
                multicastsocket.joinGroup(mgroup);
                // s = new ServerSocket(SXNET_PORT,0,myip.get(0));  
                // only listen on 1 address on multi homed systems
                System.out.println("new lanbahn multicast socket " + multicastsocket.getInterface().toString() + ":" + LANBAHN_PORT);
                System.out.println("interface= " + multicastsocket.getNetworkInterface().toString());
                // DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
            } catch (IOException ex) {
                System.out.println("could not open server socket on port=" + LANBAHN_PORT + " - closing lanbahn window.");
                return;
            }
            startLanbahnServer();  // for receiving multicast messages

            //        Timer timer = new Timer();  // for sending multicast messages
            //        timer.schedule(new MCSendTask(), 1000, 1000);
            //new Thread(new RegisterJMDNSService("lanbahn", LANBAHN_PORT, myip.get(0))).start();
        } else {
            System.out.println("no network adapter, cannot listen to lanbahn messages.");
        }
    }

    private void startLanbahnServer() {
        if (lbServer == null) {
            lbServer = new LanbahnServer();
            t = new Thread(lbServer);
            t.start();

        }

    }

    class LanbahnServer implements Runnable {

        public void run() {
            try {

                byte[] bytes = new byte[100];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

                while (running.get()) {
                    // Warten auf Nachricht
                    multicastsocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), TEXT_ENCODING);
                    message = message.replace("\n", ""); // .replace("  ", " ");
                    String ipAddr = packet.getAddress().toString().substring(1);
                    // don't react on "self" messages
                    //if (!isOwnIP(ipAddr)) {
                    //lanbahnTa.insert(message + " (" + ipAddr + ")\n", 0);

                    interpretLanbahnMessage(message, ipAddr);
                    //FunkreglerUI.setAliveByIP(ipAddr);
                    //}

                }
                System.out.println("lanbahn Server closing.");
                multicastsocket.leaveGroup(mgroup);
                multicastsocket.close();

            } catch (IOException ex) {
                System.out.println("lanbahnServer error:" + ex);
            }

        }

        private void interpretLanbahnMessage(String msg, String ip) {
            if (msg == null) {
                return;
            }
            int sxaddr, sxdata;
            if (DEBUG) {
                System.out.println("MCast: "+msg);
            }
            String cmd[] = msg.split(" ");

            switch (cmd[0].toUpperCase()) {
                case "SX":
                    // selectrix command, no further processing of sxdata byte
                    try {
                        if (cmd.length >= 3) {
                            sxaddr = Integer.parseInt(cmd[1]);
                            if (!SXUtils.isValidSXAddress(sxaddr)) {
                                // ignore invalid sx addresses
                                return;
                            }
                            sxdata = Integer.parseInt(cmd[2]);
                           // SXData.update(sxaddr, sxdata, true);
                        }
                    } catch (Exception e) {
                        System.out.println("could not understand SX command format: " + msg + " error=" + e.getMessage());
                    }
                    break;

                case "A":
                    // announcement of name / ip-address / battery-state
                    try {
                        if (cmd.length >= 4) {
                            String name = cmd[1];
                            if (name.contains("FUNKR")) {  // only "FUNKR" is a "lanbahn FREDI"

                                // check, if it is known already
                                // if (FunkreglerUI.isKnown(name)) {
                                //    FunkreglerUI.updateByName(name, cmd);
                                //} else {
                                //    FunkreglerUI fu1 = new FunkreglerUI(name, cmd);
                                //}
                                System.out.println("wiThrottle status: "+cmd);

                            }
                        }
                    } catch (Exception e) {
                        System.out.println("could not understand A command format: " + msg + " error=" + e.getMessage());
                    }
                    break;

            }

        }

        private boolean isOwnIP(String ip) {
            //System.out.println(ip);
            for (int i = 0; i < myips.size(); i++) {
                //System.out.println(myip.get(i).toString());
                if (myips.get(i).toString().contains(ip)) {
                    return true;
                }
            }
            return false;
        }

      
    }

   }
