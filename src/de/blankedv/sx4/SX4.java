/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class SX4 {

    public static int INVALID_INT = -1;
    public static boolean DEBUG = false;

    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_NOT_CONNECTED = 0;

    /**
     * {@value #VERSION} = program version, displayed in HELP window
     */
    public static final String VERSION = "1.01 - 20 Nov 2018";
    public static final String S_XNET_SERVER_REV = "SX4 -" + VERSION;

    /**
     * {@value #SX_MIN} = minimale SX adresse angezeigt im Monitor
     */
    public static final int SXMIN = 0;
    /**
     * maximale SX adresse (SX0), maximale adr angezeigt im Monitor
     */
    public static final int SXMAX = 111;
    /**
     * {@value #SX_MAX_USED} = maximale Adresse für normale Benutzung (Loco,
     * Weiche, Signal) higher addresses reserved for command stations/loco
     * programming
     */
    public static final int SXMAX_USED = 106;

    /**
     * {@value #LBMIN} =minimum lanbahn channel number
     */
    public static final int LBMIN = 10;
    public static final int LBPURE = (SXMAX + 1) * 10; // lowest pure lanbahn addres
    /**
     * {@value #LBMAX} =maximum lanbahn channel number
     */
    public static final int LBMAX = 9999;
    /**
     * {@value #LBDATAMIN} =minimum lanbahn data value
     */
    public static final int LBDATAMIN = 0;
    /**
     * {@value #LBDATAMAX} =maximum lanbahn data value (== 2 bits in SX world)
     */
    public static final int LBDATAMAX = 3;  // 

    static SXnetServer serv;
    static WifiThrottle wifiThrottle;

    public static AtomicBoolean running = new AtomicBoolean(false);
    public static BlockingQueue<IntegerPair> dataToSend = new ArrayBlockingQueue(400);
    public static AtomicInteger powerToBe = new AtomicInteger(INVALID_INT);

    public static ArrayList<Integer> locoAddresses = new ArrayList<Integer>();
    public static List<InetAddress> myips;

    public static GenericSXInterface sxi;
    public static int baudrate;
    public static String portName;
    public static boolean simulation = false;
    public static String ifType;
    public static int timeoutCounter = 0;
    public static final int TIMEOUT_SECONDS = 10;  // check for connection every 30secs

    public static boolean connectionOK = false;  // watchdog for connection

    private static int updateCount = 0;

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) {

        evalOptions(args);

        initSXInterface();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    running.set(false);  // shutdown all threads
                    Thread.sleep(200);
                    System.out.println("SX4 shutdown.");
                    //some cleaning up code...

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                }
            }
        });

        boolean result = sxi.open();
        if (!result) {
            System.out.println("ERROR - SX4 program ends.");
            System.exit(1);
        }

        if (sxi instanceof SXFCCInterface) {
            System.out.println("FCC mode=" + sxi.getMode());
        } else if (sxi instanceof SXSimulationInterface) {
            SXData.setPower(1, false);
        }
        myips = NIC.getmyip();
        if (!myips.isEmpty()) {
            running.set(true);
            serv = new SXnetServer();

            wifiThrottle = new WifiThrottle();

            while (running.get()) {
                try {
                    Thread.sleep(250);
                    doUpdate();    // TODO?? put on different thread?
                } catch (InterruptedException ex) {
                    System.out.println("ERROR" + ex.getMessage());
                }
            }
        } else {
            System.out.println("ERROR: no network");
        }
    }
        /**
         * called every 250 msecs + duration of sxi.doUpdate()
         *
         */
    public static void doUpdate() {
        String result = sxi.doUpdate();

        updateCount++;
        if (updateCount < 4) {  // do connection check only every second
            return;
        }

        updateCount = 0;
        checkConnection();

        //Route.auto();
        //CompRoute.auto();
    }

    /**
     * called once a second to do a connection check works for SLX825 TODO
     * implement for FCC
     *
     */
    private static void checkConnection() {

        timeoutCounter++;

        if ((timeoutCounter > TIMEOUT_SECONDS) && (sxi.isConnected())) {
            sxi.requestPower();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            // wait a few milliseconds for response
            // check if connectionOK flag was reset
            if (!connectionOK) {
                System.out.println("Verbindung verloren !! ");
                closeConnection();
            } else {
                connectionOK = false; // will be set to true again in receive routine
            }
            timeoutCounter = 0; // reset counter
        }
    }

    private static void closeConnection() {
        if (sxi.isConnected()) {
            sxi.close();

        }
        connectionOK = false;
    }


    /*The finalize() method is called by the Java virtual machine (JVM)* before the program exits to give the program a chance to clean up and release resources. Multi-threaded programs should close all Files and Sockets they use before exiting so they do not face resource starvation. The call to server.close() in the finalize() method closes the Socket connection used by each thread in this program.
     */
    protected void finalize() {
//Objects created in run method are finalized when
//program terminates and thread exits

        running.set(false);
        serv.close();

    }

    private static void initSXInterface() {
        if (sxi != null) {
            sxi.close();
        }

        if (simulation) {
            sxi = new SXSimulationInterface();
        } else if (ifType.contains("FCC")) { // fcc has different interface handling ! 
            sxi = new SXFCCInterface(portName);
        } else {
            //portName = "/dev/ttyUSB825";
            sxi = new SXInterface(portName, baudrate);
        }
    }

    private static void evalOptions(String[] args) {
        CommandLine commandLine;
        Option option_h = Option.builder("h")
                .required(false)
                .desc("show help")
                .longOpt("help")
                .hasArg(false)
                .build();
        Option option_t = Option.builder("t")
                .required(false)
                .desc("Interface Type (SLX825, FCC, SIM), default=SIM")
                .longOpt("type")
                .hasArg(true)
                .build();
        Option option_s = Option.builder("s")
                .required(false)
                .desc("Serial Device - default=/dev/ttyUSB0")
                .longOpt("Device")
                .hasArg(true)
                .build();
        Option option_b = Option.builder("b")
                .required(false)
                .desc("Baudrate (only needed for SLX825), default=9600")
                .hasArg(true)
                .longOpt("baudrate")
                .build();
        Option option_v = Option.builder("v")
                .required(false)
                .desc("program version and date")
                .hasArg(false)
                .longOpt("version")
                .build();
        Option option_d = Option.builder("d")
                .required(false)
                .desc("debug output on")
                .hasArg(false)
                .longOpt("debug")
                .build();

        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        // example options
        // = {"-D", "/dev/ttyUSB0", "-b", "9600", "-t", "SLX825"};
        //= {"-D", "/dev/ttyUSB0", "-t", "FCC"};
        // = { "-t", "SIM"};
        options.addOption(option_h);
        options.addOption(option_t);
        options.addOption(option_d);
        options.addOption(option_s);
        options.addOption(option_b);
        options.addOption(option_v);

        HelpFormatter formatter = new HelpFormatter();

        try {
            commandLine = parser.parse(options, args);

            if (commandLine.hasOption("h")) {
                formatter.printHelp("SX4", options, true);
                System.exit(0);
            }

            if (commandLine.hasOption("v")) {
                System.out.println("SX4 version: " + VERSION);
                System.out.println("use option '-h' to get possible program options");
                System.exit(0);
            }

            if (commandLine.hasOption("d")) {
                System.out.println("switching on debug output");
                DEBUG = true;
            } else {
                DEBUG = false;
            }

            simulation = false;

            if (commandLine.hasOption("t")) {
                ifType = commandLine.getOptionValue("t");
                switch (ifType) {
                    case "SIM":
                        System.out.println("SX Interface Type=" + ifType);
                        simulation = true;
                        sxi = new SXSimulationInterface();
                        break;
                    case "SXL825":
                        System.out.println("SX Interface Type=" + ifType);
                        simulation = false;
                        portName = readSerialPortName(commandLine);
                        System.out.println("serial port device = " + portName);
                        baudrate = readBaudrate(commandLine);
                        System.out.println("serial baudrate = " + baudrate);
                        sxi = new SXInterface(portName, baudrate);
                        break;
                    case "FCC":
                        System.out.println("SX Interface Type=" + ifType);
                        simulation = false;
                        portName = readSerialPortName(commandLine);
                        System.out.println("serial port device = " + portName);
                        baudrate = 230400;
                        System.out.println("serial baudrate = " + baudrate);
                        sxi = new SXFCCInterface(portName);
                        break;
                    default:
                        System.out.println("ERROR: invalid interface type=" + ifType);
                        System.out.println("SX4 program ends.");
                        System.exit(1);
                }
            } else {
                simulation = true;
                ifType = "SIM";
                sxi = new SXSimulationInterface();
                System.out.println("option -t not set, using SIM as interface");
            }

            String[] remainder = commandLine.getArgs();
            if (remainder.length > 0) {
                System.out.print("invalid options: ");
                for (String argument : remainder) {
                    System.out.print(argument);
                    System.out.print(" ");
                }
                System.out.println();
            }

        } catch (ParseException exception) {
            System.out.print("Parse error: ");
            System.out.println(exception.getMessage());
        }

    }

    private static String readSerialPortName(CommandLine cl) {
        String port = "/dev/ttyUSB0";
        if (cl.hasOption("s")) {

            port = cl.getOptionValue("s");
        }
        return port;
    }

    private static int readBaudrate(CommandLine cl) {
        int baud = 9600;

        if (cl.hasOption("b")) {
            try {
                baud = Integer.parseInt(cl.getOptionValue("r"));
            } catch (NumberFormatException e) {
                System.out.println("ERROR in baudrate parameter, using 9600");
                baud = 9600;
            }
        }
        return baud;
    }
}
