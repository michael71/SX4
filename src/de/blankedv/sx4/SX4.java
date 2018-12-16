/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static de.blankedv.sx4.Constants.*;

/**
 *
 * with main() function
 *
 * @author mblank
 */
public class SX4 {

    public static boolean debug = false;
    public static volatile boolean running = true;
    public static ArrayBlockingQueue<IntegerPair> dataToSend = new ArrayBlockingQueue<>(400);
    public static AtomicInteger powerToBe = new AtomicInteger(INVALID_INT);

    public static ArrayList<Integer> locoAddresses = new ArrayList<Integer>();
    public static List<InetAddress> myips;

    public static GenericSXInterface sxi;
    static WifiThrottle wifiThrottle;

    public static int baudrate;
    public static String portName;
    public static boolean simulation = false;
    public static String ifType;
    public static int timeoutCounter = 0;
    public static final int TIMEOUT_SECONDS = 10;  // check for connection every 30secs

    public static boolean connectionOK = false;  // watchdog for connection

    private static int updateCount = 0;
    private static Timer timer = new Timer();
    public static volatile long lastConnected = System.currentTimeMillis();

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) {

        EvalOptions.sx4options(args);

        boolean result = false;
        sxi = initSXInterface(portName, baudrate);
        if (sxi != null) {
            result = sxi.open();
        }
        if (!result) {
            System.out.println("ERROR - SX4 program ends.");
            System.exit(1);
        }

        myips = NIC.getmyip();
        if (!myips.isEmpty()) {

            final SXnetServer serv = new SXnetServer();

            wifiThrottle = new WifiThrottle();

            shutdownHook(serv);

            while (running) {
                try {
                  /*  Thread.sleep(50);
                    sxi.doSendUpdate(); // only check sendqueue
                    Thread.sleep(50);
                    sxi.doSendUpdate();
                    Thread.sleep(50);
                    sxi.doSendUpdate();
                    Thread.sleep(50);
                    sxi.doSendUpdate(); */
                    Thread.sleep(100);
                    sxi.doUpdate();     // includes reading all SX data 
                    
                    //Route.auto();
                    //CompRoute.auto();
                } catch (InterruptedException ex) {
                    System.out.println("ERROR" + ex.getMessage());
                }
            }
        } else {
            System.out.println("ERROR: no network - SX4 program ends.");
            System.exit(1);
        }
    }

    private static void shutdownHook(SXnetServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    running = false;  // shutdown all threads
                    server.stopClients();
                    Thread.sleep(200);
                    System.out.println("SX4 ends.");
                    //some cleaning up code...

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                }
            }
        });
    }

    private static void initConnectivityCheck() {
        // init timer for connectivity check (if not in simulation)
        // this program is shutdown, if there is no connection for 10 seconds

        lastConnected = System.currentTimeMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // System.out.println("conn check");
                if ((System.currentTimeMillis() - lastConnected) >= 10 * 1000) {
                    System.out.println("ERROR: lost connection.");
                    running = false;  // stop all threads
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        System.out.println("ERROR" + ex.getMessage());
                    }
                    System.out.println("SX4 shutdown.");
                    System.exit(1);
                }
            }
        }, 10 * 1000, 1000);
    }

    private static GenericSXInterface initSXInterface(String port, int baud) {
        GenericSXInterface sxInterface = null;
        if (simulation) {
            sxInterface = new SimulationInterface();
            // switch on power for simulation
            SXData.setPower(1, false);
            // no connectivityCheck for simulation
        } else if (ifType.contains("FCC")) { // fcc has different interface handling ! 
            sxInterface = new FCCInterface(port);
            initConnectivityCheck();
            System.out.println("FCC mode=" + sxi.getMode());

        } else if (ifType.contains("SLX825")) {
            //portName = "/dev/ttyUSB825";
            sxInterface = new SLX825Interface(port, baud);
            initConnectivityCheck();
        }
        return sxInterface;

    }

}
