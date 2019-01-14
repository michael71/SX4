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
import java.text.SimpleDateFormat;

import static de.blankedv.sx4.Constants.*;
import static com.esotericsoftware.minlog.Log.*;
import de.blankedv.sx4.timetable.CompRoute;
import de.blankedv.sx4.timetable.PanelElement;

import de.blankedv.sx4.timetable.ReadConfig;
import de.blankedv.sx4.timetable.Route;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 *
 * with main() function
 *
 * @author mblank
 */
public class SX4 {

    public static volatile boolean running = true;
    public static boolean routingEnabled = false;
    
    public static ArrayBlockingQueue<IntegerPair> dataToSend = new ArrayBlockingQueue<>(400);
    public static AtomicInteger powerToBe = new AtomicInteger(INVALID_INT);

    public static ArrayList<Integer> locoAddresses = new ArrayList<Integer>();

    public static List<InetAddress> myips;
    public static String configFilename = "";

    public static GenericSXInterface sxi;
    static WifiThrottle wifiThrottle;

    public static int baudrate;
    public static String portName;
    public static boolean simulation = false;
    public static String ifType;
    public static int timeoutCounter = 0;
    public static final int TIMEOUT_SECONDS = 10;  // check for connection every 30secs

    public static boolean connectionOK = false;  // watchdog for connection
    public static final int NUMBER_OF_FILES_TO_RETAIN = 5;

    private static int updateCount = 0;
    private static Timer timer = new Timer();
    public static volatile long lastConnected = System.currentTimeMillis();
    
   
   @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) {
        SX4 app = new SX4();
        app.runSX4(args);
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void runSX4(String[] args) {

        if (isDebugFlagSet(args)) {  // must be done first to log errors during command line eval)          
            set(LEVEL_DEBUG);
        } else {
            // only 2 different logging levels are used: INFO or DEBUG (if "-d" on command line start)
            set(LEVEL_INFO);
        }

        startLogging();  // start simple logging


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
        configFilename = SXUtils.getConfigFilename();


        
        if (configFilename.isEmpty()) {
            error("no panel...xml file found, NOT starting config server");
        } else {
            ReadConfig.readXML(configFilename);
            loadTrainNumbers();  // sensors must be know when setting train numbers
            
            try {
                new ConfigWebserver(configFilename);
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        }

        myips = NIC.getmyip();
        if (!myips.isEmpty()) {

            final SXnetServer serv = new SXnetServer();

            wifiThrottle = new WifiThrottle();

            shutdownHook(serv);
            int count = 0;
            while (running) {
                try {

                    Thread.sleep(300);
                    sxi.doUpdate();     // includes reading all SX data 

                    if (routingEnabled) {
                        Route.auto();
                        CompRoute.auto();
                    }
                    // TrainNumberData.auto();  will be actively reset by fahrstrassensteurung
                    count++;
                    if (count > 10) {  // save current state every few seconds
                        count = 0;
                        saveTrainNumbers();
                    }
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

    private void shutdownHook(SXnetServer server) {
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

    /** save current train numbers for all sensors
     * 
     */
    private void saveTrainNumbers() {
       StringBuilder state = new StringBuilder();
       Collections.sort(panelElements);
       for (PanelElement pe : panelElements) {
           if (pe.isSensor() ) {
                state.append(pe.getAdr());
                state.append(" ");
                state.append(pe.getTrain());
                state.append(";");
            }
        }
        final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.put("trainNumbers", state.toString());
    }
    
    private void loadTrainNumbers() {
        final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String trainState = prefs.get("trainNumbers", "");
        if (trainState.isEmpty()) return;
        
        info("read previous state="+trainState);     
        String[] states = trainState.split(";");
        for (String s : states) {
            String[] keyValue = s.split(" ");
            if (keyValue.length == 2) {
                try {
                int addr = Integer.parseInt(keyValue[0]);
                int data = Integer.parseInt(keyValue[1]);              
                PanelElement.setTrain(addr,data);
                } catch (NumberFormatException e) {
                    // invalid data
                }
            }
        }
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
        for (String s : args) {
            //System.out.println(s);
            if (s.equals("-d")) {
                return true;
            }
        }
        return false;
    }

    private static void deleteOlderLogfiles() {
        File curDir = new File(".");
        ArrayList<File> logFiles = new ArrayList<>();
        File[] filesList = curDir.listFiles();

        for (File f : filesList) {
            if (f.isFile() && f.getName().startsWith("log") && f.getName().endsWith(".txt")) {
                logFiles.add(f);
            }
        }
        Collections.sort(logFiles);
        if (!logFiles.isEmpty() && (logFiles.size() > NUMBER_OF_FILES_TO_RETAIN)) {
            for (int i = 0; i < (logFiles.size() - NUMBER_OF_FILES_TO_RETAIN); i++) {
                //debug("deleting old log file" + logFiles.get(i).getName());
                logFiles.get(i).delete();
            }
        }
    }

    private static void startLogging() {
        // start simple logging
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currDateTime = df.format(new Date());
        String logFileName = "log." + currDateTime + ".txt";
        setLogger(new MyLogger(logFileName));
        info("starting " + VERSION);
        info("datetime=" + currDateTime);
        deleteOlderLogfiles();

    }
}
