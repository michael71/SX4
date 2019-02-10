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

/**
 *
 * @author mblank
 */
import static com.esotericsoftware.minlog.Log.*;
import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.blankedv.sx4.timetable.ReadConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * very simple http server to be able for update the layout config file in
 * client applications like lanbahnPanel (Android) from the SX4 program
 *
 * @author mblank
 */
public class ConfigWebserver {

    String fileName = "";
    HttpServer server;
    private final int PORT = 8000;  // fixed port used by android software

    public ConfigWebserver(String fname) throws Exception {

        if (fname.isEmpty()) {
            return;
        }
        fileName = fname;  // store locally for later use in http server
        
        info("starting config server on port " + PORT + ", serving: " + fileName);
        
        // double check name inside XML file
        String pName = ReadConfig.readPanelName(fileName);
         if (!pName.equals(fileName)) {
            error("wrong 'filename' attribute="+pName+" in layout-config file="+fileName);
        }
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public void stop() {
        server.stop(0);

    }

    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) {
            String response;
            URI requestURI = t.getRequestURI();
            Headers h = t.getResponseHeaders();
            debug("config server URI=" + requestURI.getPath());
            OutputStream os;
            try {
                if (requestURI.getPath().contains("config")) {
                    response = new String(Files.readAllBytes(Paths.get(fileName)));
                    h.add("Content-Type", "text/xml ; charset=utf-8");
                    t.sendResponseHeaders(200, response.length());
                    os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else if (requestURI.getPath().contains("apk")) {
                    h.add("Content-Type", "application/vnd.android.package-archive");
                    File file = new File("lanbahnpanel.apk");
                    byte[] bytearray = new byte[(int) file.length()];
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(bytearray, 0, bytearray.length);

                    // ok, we are ready to send the response.
                    t.sendResponseHeaders(200, file.length());
                    os = t.getResponseBody();
                    os.write(bytearray, 0, bytearray.length);
                    os.close();

                } else {
                    response = "ERROR:  use URL :8000/config or :8000/lanbahnpanel.apk";
                    h.add("Content-Type", "text/html ; charset=utf-8");
                    t.sendResponseHeaders(200, response.length());
                    os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }

            } catch (IOException ex) {
                error(ex.getMessage());
            }

        }
    }

}
