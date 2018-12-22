/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        fileName = fname;
        if (fileName.isEmpty()) {
            return;
        }
        
        info("starting config server on port " + PORT + ", serving: " + fileName);
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
                } else if (requestURI.getPath().contains("lanbahnpanel.apk")) {
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
