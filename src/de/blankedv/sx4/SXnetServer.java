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

import static de.blankedv.sx4.SX4.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import static com.esotericsoftware.minlog.Log.*;

/**
 *
 * @author mblank
 */
public class SXnetServer {

    private static final int SXNET_PORT = 4104;
    static SXnetServerThread server;

    // Preferences
    //Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    protected Thread t;
    private ServerSocket s;
    
    private ArrayList<SXnetClient> clients = new ArrayList<>();

    /**
     * Creates new form SRCPServerUI
     */
    public SXnetServer() {
        
        if (myips.isEmpty()) {
            error("ERROR, no usable network interface. EXITING");
            System.exit(1);
        }
        try {
            s = new ServerSocket(SXNET_PORT, 0, myips.get(0));
            info("new sxnet server listening at " + myips.get(0) + ":" + SXNET_PORT);
            //title.s

        } catch (IOException ex) {
            error("could not open server socket on port=" + SXNET_PORT);
            error("is another instance of the SX4 program running already?  now closing SX4");
            System.exit(0);
            return;
        }
        startSXnetServer();
    }

    private void startSXnetServer() {
        if (server == null) {
            server = new SXnetServerThread();
            t = new Thread(server);
            t.start();
        }

    }
    
    public void stopClients() {
        for (SXnetClient c : clients) {
            c.stop();
        }
        running = false;
    }


    /*The finalize() method is called by the Java virtual machine (JVM)* before 
    the program exits to give the program a chance to clean up and release resources.
    Multi-threaded programs should close all Files and Sockets they use before 
    exiting so they do not face resource starvation. The call to s.close() 
    in the finalize() method closes the Socket connection used by each thread 
    in this program.
     */
    protected void finalize() {
//Objects created in run method are finalized when
//program terminates and thread exits
        close();
    }

    public void close() {
        try {
            s.close();
        } catch (IOException e) {
            error("Could not close socket");
            System.exit(-1);
        }
    }

    class SXnetServerThread implements Runnable {

        public void run() {
            try {
                while (running) {
                    Socket incoming = s.accept();  // wait for client to connect

                    info("new client connected " + incoming.getRemoteSocketAddress().toString() + "\n");

                    // after new client has connected start new thread to handle this client
                    SXnetClient r = new SXnetClient(incoming);
                    clients.add(r);
                    Thread t = new Thread(r);
                    t.start();

                }
                info("SXnetServerThread closing.");
                s.close();
            } catch (InterruptedIOException e1) {
                try {
                    info("SXnetServerThread interrupted, closing socket");
                    s.close();
                } catch (IOException ex) {
                    error("closing error " + ex);
                }
            } catch (IOException ex) {
                error("SXnetServer error:" + ex);
            }

        }
    }

}
