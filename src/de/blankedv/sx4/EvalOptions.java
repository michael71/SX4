/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author mblank
 */
public class EvalOptions {

    static public void sx4options(String[] args) {
        //for (String a : args) {
        //    System.out.println("option _" + a + "_");
        //}
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
                .desc("Serial Device - default=ttyUSB0")
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
                debug = true;
            } else {
                debug = false;
            }

            simulation = false;

            if (commandLine.hasOption("t")) {
                ifType = commandLine.getOptionValue("t");
                switch (ifType) {
                    case "SIM":
                        System.out.println("SX Interface Type=" + ifType);
                        simulation = true;
                        sxi = new SimulationInterface();
                        break;
                    case "SLX825":
                        System.out.println("SX Interface Type=" + ifType);
                        simulation = false;
                        portName = readSerialPortName(commandLine);
                        System.out.println("serial port device = " + portName);
                        baudrate = readBaudrate(commandLine);
                        System.out.println("serial baudrate = " + baudrate);
                        sxi = new SLX825Interface(portName, baudrate);
                        break;
                    case "FCC":
                        System.out.println("SX Interface Type=" + ifType);
                        simulation = false;
                        portName = readSerialPortName(commandLine);
                        System.out.println("serial port device = " + portName);
                        baudrate = 230400;
                        System.out.println("serial baudrate = " + baudrate);
                        sxi = new FCCInterface(portName);
                        break;
                    default:
                        System.out.println("ERROR: invalid interface type=" + ifType);
                        System.out.println("SX4 program ends.");
                        System.exit(1);
                }
            } else {
                simulation = true;
                ifType = "SIM";
                sxi = new SimulationInterface();
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
