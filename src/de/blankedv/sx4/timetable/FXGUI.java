/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import javafx.application.Application;
import javafx.embed.swing.JFXPanel;

/**
 *
 * @author mblank
 */
public class FXGUI {

    public static boolean init() {
        try {
            JFXPanel fakePanel = new JFXPanel();   // initializes JAVAFX
            return true;
        } catch (NoClassDefFoundError e) {
            // could not initialize JavaFX - fx-runtime missing?
            debug(e.getMessage());
            return false;
        }
    }

    public static void start(String[] args) {
        new Thread() {
            @Override
            public void run() {
                Application.launch(TripsTable.class, args);
            }
        }.start();
    }
}
