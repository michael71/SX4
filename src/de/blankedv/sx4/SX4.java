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
import static com.esotericsoftware.minlog.Log.*;
import java.util.logging.Level;

/**
 *
 * with main() function
 *
 * @author mblank
 */
public class SX4 {

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

        if (isDebugFlagSet(args)) {  // must be done first to log errors during command line eval)          
            set(LEVEL_DEBUG);
            debug("switching on debug output");
        } else {
             // only 2 different logging levels are used INFO or DEBUG (if "-d" on command line start)
            set(LEVEL_INFO);
        }
        
        // start simple logging
        setLogger(new MyLogger("log.txt"));
        info("starting "+VERSION);

        EvalOptions.sx4options(args);

        boolean result = false;
        sxi = initSXInterface(portName, baudrate);
        if (sxi != null) {
            result = sxi.open();
        }
        if (!result) {
            error("SX4 program ends.");
            System.exit(1);
        }

        try {
            new ConfigWebserver();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
        
        myips = NIC.getmyip();
        if (!myips.isEmpty()) {

            final SXnetServer serv = new SXnetServer();

            wifiThrottle = new WifiThrottle();

            shutdownHook(serv);

            while (running) {
                try {

                    Thread.sleep(300);
                    sxi.doUpdate();     // includes reading all SX data 

                    //Route.auto();
                    //CompRoute.auto();
                } catch (InterruptedException ex) {
                    error("ERROR" + ex.getMessage());
                    error(ex.getMessage());
                }
            }
        } else {
            error("no network - SX4 program ends.");
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
                    error("SX4 ends.");
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
        info("initConnectivityCheck");
        lastConnected = System.currentTimeMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // error("conn check");
                if ((System.currentTimeMillis() - lastConnected) >= 10 * 1000) {
                    error("lost connection.");
                    running = false;  // stop all threads
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        error("ERROR" + ex.getMessage());
                    }
                    error("SX4 shutdown.");
                    System.exit(1);
                }
            }
        }, 10 * 1000, 1000);
    }

    private static GenericSXInterface initSXInterface(String port, int baud) {
        GenericSXInterface sxInterface = null;
        if (simulation) {
            sxInterface = new SimulationInterface();
            info("simulation");
            // switch on power for simulation
            SXData.setPower(1, false);
            // no connectivityCheck for simulation
        } else if (ifType.contains("FCC")) { // fcc has different interface handling ! 
            sxInterface = new FCCInterface(port);
            // TODO initConnectivityCheck();
            info("FCC mode=" + sxi.getMode());

        } else if (ifType.contains("SLX825")) {
            //portName = "/dev/ttyUSB825";
            sxInterface = new SLX825Interface(port, baud);
            info("SLX825 interface");
            //TODO initConnectivityCheck();
        }
        return sxInterface;

    }

    private static boolean isDebugFlagSet(String[] args) {
        for (String s :args) {
            //System.out.println(s);
            if (s.equals("-d")) return true;
        }
        return false;
    }
}
