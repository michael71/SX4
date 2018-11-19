package de.blankedv.sx4;

import static de.blankedv.sx4.SX4.running;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.prefs.Preferences;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Creates new form SRCPServerUI
     */
    public SXnetServer() {
        List<InetAddress> myips = NIC.getmyip();
        if (myips.isEmpty()) {
            System.out.println("ERROR, no usable network interface. EXITING");
            System.exit(0);
        }
        try {
            s = new ServerSocket(SXNET_PORT, 0, myips.get(0));
            System.out.println("new sxnet server listening at " + myips.get(0) + ":" + SXNET_PORT);
            //title.s

        } catch (IOException ex) {
            System.out.println("could not open server socket on port=" + SXNET_PORT);
            System.out.println("is another instance of the SX4 program running already?  now closing SX4");
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
            System.out.println("Could not close socket");
            System.exit(-1);
        }
    }

    class SXnetServerThread implements Runnable {

        public void run() {
            try {
                while (running.get() == true) {
                    Socket incoming = s.accept();  // wait for client to connect

                    System.out.println("new client connected " + incoming.getRemoteSocketAddress().toString() + "\n");

                    // after new client has connected start new thread to handle this client
                    SXnetClient r = new SXnetClient(incoming);
                    Thread t = new Thread(r);
                    t.start();

                }
                System.out.println("SXnetServerThread closing.");
                s.close();
            } catch (InterruptedIOException e1) {
                try {
                    System.out.println("SXnetServerThread interrupted, closing socket");
                    s.close();
                } catch (IOException ex) {
                    System.out.println("closing error " + ex);
                }
            } catch (IOException ex) {
                System.out.println("SXnetServer error:" + ex);
            }

        }
    }

}
