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
package de.blankedv.sx4.timetable;

import static com.esotericsoftware.minlog.Log.debug;
import static de.blankedv.sx4.Constants.NUM_VERSION;
import static de.blankedv.sx4.timetable.VarsFX.allTimetables;
import static de.blankedv.sx4.timetable.Vars.panelName;

import java.util.ArrayList;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import static de.blankedv.sx4.timetable.Vars.allTimetableUIs;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

/**
 *
 * @author mblank
 */
public class MainUI extends Application {

    private Stage primaryStage;
    private Scene mainScene;

    private final ComboBox<String> cbSelectTimetable = new ComboBox<>();
    private final Button btnStart = new Button("anzeigen");
    private final ArrayList<String> cbTimetables = new ArrayList<>();

    @Override
    public void start(Stage stage) {

        primaryStage = stage;
        GridPane gp = new GridPane();
        Label status = new Label(" ");

        VBox vb = new VBox(5);
        vb.setPadding(new Insets(15));
        HBox hb = new HBox(5);
        hb.setPadding(new Insets(15));

        vb.getChildren().addAll(new Label("Fahrplan auswählen:"), hb, status);
        //hb.setAlignment(Pos.CENTER);

        hb.getChildren().addAll(cbSelectTimetable, btnStart);

        mainScene = new Scene(vb, 320, 130);

        debug("starting MainUI");

        if (!allTimetables.isEmpty()) {
            for (Timetable tt : allTimetables) {
                cbTimetables.add("" + tt.getAdr());
            }
            cbSelectTimetable.getItems().addAll(cbTimetables);
            cbSelectTimetable.getSelectionModel().select(0);
            cbSelectTimetable.setDisable(false);
        } else {
            status.setText("kein Fahrplan vorhanden");
            debug("MainUI: kein Fahrplan vorhanden");
            cbSelectTimetable.setDisable(true);
        }

        btnStart.setOnAction(e -> {          
            int ttAddress = Integer.parseInt(cbSelectTimetable.getValue());
            boolean alreadyRunning = false;

            // check, if a Timetable with this address is already displayed
            for (TimetableUI trTable : allTimetableUIs) {
                if (trTable.getAddress() == ttAddress) {
                    alreadyRunning = true;
                    trTable.show();  // just redisplay it
                    break;
                }
            }

            if (!alreadyRunning) {
                // create a new TimetableUI Window
                for (Timetable tt : allTimetables) {
                    if (tt.getAdr() == ttAddress) {
                        TimetableUI trTable = new TimetableUI(primaryStage, tt);
                        trTable.start();
                    }
                }
            }
        });

        primaryStage.setTitle("Fahrpläne  " + panelName + " (rev. " + NUM_VERSION + ")");
        primaryStage.setScene(mainScene);
        primaryStage.setX(100);
        primaryStage.setY(100);
        //primaryStage.initStyle(StageStyle.UTILITY);

        primaryStage.show();

        primaryStage.setOnCloseRequest((WindowEvent e) -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Programm beenden");
            alert.setHeaderText("SX4 Programm beenden?");
            alert.setContentText("");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                debug("program closing");
                System.exit(0);
            } else {
                e.consume();   // ==> do nothing
            }
        });

    }

}
