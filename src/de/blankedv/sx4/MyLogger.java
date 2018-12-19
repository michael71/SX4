/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4;

import static com.esotericsoftware.minlog.Log.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;


/**
 *
 * @author mblank
 */
public class MyLogger extends Logger {

    Writer writer;

    MyLogger(String path)  {
        try {
            writer = new OutputStreamWriter(new FileOutputStream(path));
        } catch (FileNotFoundException ex) {
            error(ex.getMessage());
            System.out.println("could not open "+path+" - using log.txt instead");
            try {
                writer = new OutputStreamWriter(new FileOutputStream("log.txt"));
            } catch (FileNotFoundException ex1) {
                error(ex.getMessage());
            }
        }
    }
    
    @Override
    protected void print(String message) {
        System.out.println(message);
        try {
            writer.write(message);
            writer.write('\n');
            writer.flush();
        } catch (IOException ex) {
            error(ex.getMessage());
        }
    }
}
