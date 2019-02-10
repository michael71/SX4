/*
SX4
Copyright (C) 2019 Michael Blank

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.blankedv.sx4;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import static de.blankedv.sx4.SX4.*;
import static com.esotericsoftware.minlog.Log.*;
import static de.blankedv.sx4.Constants.INVALID_INT;

/**
 *
 * @author mblank
 *
 * Nov 2018 : Simplified - only for Funkr2 Throttles (and only receive multicast
 * lanbahn messages)
 *
 */
public class WifiThrottle {

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
                info("new lanbahn multicast socket " + multicastsocket.getInterface().toString() + ":" + LANBAHN_PORT);
                info("interface= " + multicastsocket.getNetworkInterface().toString());
                // DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
            } catch (IOException ex) {
                error("could not open server socket on port=" + LANBAHN_PORT + " - closing lanbahn window.");
                return;
            }
            startLanbahnServer();  // for receiving multicast messages

            //        Timer timer = new Timer();  // for sending multicast messages
            //        timer.schedule(new MCSendTask(), 1000, 1000);
            //new Thread(new RegisterJMDNSService("lanbahn", LANBAHN_PORT, myip.get(0))).start();
        } else {
            error("no network adapter, cannot listen to lanbahn messages.");
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

                while (running) {
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
                info("lanbahn Server closing.");
                multicastsocket.leaveGroup(mgroup);
                multicastsocket.close();

            } catch (IOException ex) {
                error("lanbahnServer error:" + ex);
            }

        }

        private void interpretLanbahnMessage(String msg, String ip) {
            if (msg == null) {
                return;
            }
            int sxaddr, sxdata;
            debug("MC read: " + msg);

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
                            SXData.update(sxaddr, sxdata, true);
                        }
                    } catch (Exception e) {
                        error("could not understand SX command format: " + msg + " error=" + e.getMessage());
                    }
                    break;
                case "R":
                    // read status of selectrix channel
                    try {
                        if (cmd.length >= 2) {
                            sxaddr = Integer.parseInt(cmd[1]);
                            if (!SXUtils.isValidSXAddress(sxaddr)) {
                                // ignore invalid sx addresses
                                return;
                            }
                            int data = SXData.get(sxaddr);
                            if ((data != INVALID_INT) && (multicastsocket != null)) {
                                String str ="X "+sxaddr+" "+data+"\n";
                                byte[] buf = str.getBytes();
                                DatagramPacket packet = new DatagramPacket(buf, buf.length, mgroup, LANBAHN_PORT);
                                multicastsocket.send(packet);
                                debug("MC sent: "+str.substring(0, str.length()-1));  // newline removed from string
                            }
                            
                        }
                    } catch (Exception e) {
                        error("could not understand SX command format: " + msg + " error=" + e.getMessage());
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
                                info("wiThrottle status: " + msg);

                            }
                        }
                    } catch (Exception e) {
                        error("could not understand A command format: " + msg + " error=" + e.getMessage());
                    }
                    break;

            }

        }

        private boolean isOwnIP(String ip) {
            //error(ip);
            for (int i = 0; i < myips.size(); i++) {
                //error(myip.get(i).toString());
                if (myips.get(i).toString().contains(ip)) {
                    return true;
                }
            }
            return false;
        }

    }

}
