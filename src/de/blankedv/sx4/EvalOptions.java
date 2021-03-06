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

import static de.blankedv.sx4.Constants.*;
import static de.blankedv.sx4.SX4.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import static com.esotericsoftware.minlog.Log.*;

/**
 *
 * @author mblank
 */
public class EvalOptions {

    static public void sx4options(String[] args) {
        //for (String a : args) {
        //    error("option _" + a + "_");
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
                .longOpt("serial")
                .hasArg(true)
                .build();
        Option option_b = Option.builder("b")
                .required(false)
                .desc("Baudrate (not needed for FCC and SIM), default=9600")
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

        Option option_r = Option.builder("r")
                .required(false)
                .desc("enable routing")
                .hasArg(false)
                .longOpt("routing")
                .build();

        Option option_g = Option.builder("g")
                .required(false)
                .desc("enable timetable gui")
                .hasArg(false)
                .longOpt("gui")
                .build();

        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        options.addOption(option_h);
        options.addOption(option_t);
        options.addOption(option_d);
        options.addOption(option_s);
        options.addOption(option_b);
        options.addOption(option_v);
        options.addOption(option_r);
        options.addOption(option_g);

        HelpFormatter formatter = new HelpFormatter();

        try {
            commandLine = parser.parse(options, args);

            if (commandLine.hasOption("h")) {
                formatter.printHelp("SX4", options, true);
                System.exit(0);
            }

            if (commandLine.hasOption("v")) {
                info("SX4 version: " + VERSION);
                info("use option '-h' to get possible program options");
                System.exit(0);
            }

            if (commandLine.hasOption("r")) {
                info("routing is enabled");
                routingEnabled = true;
                if (commandLine.hasOption("g")) {
                    info("timetable gui will be started.");
                    guiEnabled = true;
                }
            }

            simulation = false;

            if (commandLine.hasOption("t")) {
                ifType = commandLine.getOptionValue("t");
                switch (ifType) {
                    case "SIM":
                        info("SX Interface Type=" + ifType);
                        simulation = true;
                        sxi = new SimulationInterface();
                        break;
                    case "SLX825":
                        info("SX Interface Type=" + ifType);
                        simulation = false;
                        portName = readSerialPortName(commandLine);
                        info("serial port device = " + portName);
                        baudrate = readBaudrate(commandLine);
                        info("serial baudrate = " + baudrate);
                        sxi = new SLX825Interface(portName, baudrate);
                        break;
                    case "FCC":
                        info("SX Interface Type=" + ifType);
                        simulation = false;
                        portName = readSerialPortName(commandLine);
                        info("serial port device = " + portName);
                        baudrate = 230400;
                        info("serial baudrate = " + baudrate);
                        sxi = new FCCInterface(portName);
                        break;
                    default:
                        error("ERROR: invalid interface type=" + ifType);
                        error("SX4 program ends.");
                        System.exit(1);
                }
            } else {
                simulation = true;
                ifType = "SIM";
                sxi = new SimulationInterface();
                info("option -t not set, using SIM as interface");
            }

            String[] remainder = commandLine.getArgs();
            if (remainder.length > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("invalid options: ");
                for (String argument : remainder) {
                    sb.append(argument);
                    sb.append(" ");
                }
                error(sb.toString());
            }

        } catch (ParseException exception) {
            System.out.print("Parse error: ");
            error(exception.getMessage());
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
                baud = Integer.parseInt(cl.getOptionValue("b"));
            } catch (NumberFormatException e) {
                error("ERROR in baudrate parameter, using 9600");
                baud = 9600;
            }
        }
        return baud;
    }
}
