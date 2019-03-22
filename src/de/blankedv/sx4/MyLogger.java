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
            try {
                writer = new OutputStreamWriter(new FileOutputStream("log.txt"));
                error("could not open "+path+" - using log.txt instead");
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
