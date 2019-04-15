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
import static com.esotericsoftware.minlog.Log.error;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.StageStyle;

/**
 *
 * @author mblank
 */
public class MainUI extends Application {

    public static Stage manualStage = new Stage();

    private Stage primaryStage;
    private Scene mainScene;

    private final ComboBox<String> cbSelectTimetable = new ComboBox<>();
    private final Button btnStart = new Button("anzeigen");
    private final ArrayList<String> cbTimetables = new ArrayList<>();
    private final Pane spacer = new Pane();
    private final Image imgHelp = new Image("/de/blankedv/sx4/res/help2.png");
    private final ImageView ivHelp = new ImageView(imgHelp);

    @Override
    public void start(Stage stage) {

        primaryStage = stage;

        Label status = new Label(" ");

        VBox vb = new VBox(5);
        vb.setStyle("-fx-background-color: #c0ffc0;");
        vb.setPadding(new Insets(15));
        HBox hb = new HBox(5);
        //hb.setPadding(new Insets(15));
        HBox hb2 = new HBox(5);
        //hb2.setPadding(new Insets(15));
        hb2.getChildren().addAll(new Label("Fahrplan auswählen:"), spacer, ivHelp);

        vb.getChildren().addAll(hb2, hb, status);
        //hb.setAlignment(Pos.CENTER);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hb.getChildren().addAll(cbSelectTimetable, btnStart);

        mainScene = new Scene(vb, 320, 100);
        manualStage.initOwner(primaryStage);

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
                        if (trTable.check()) {
                            trTable.start();
                        } else {
                            trTable.close();
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Error alert");
                            alert.setHeaderText(null);
                            alert.setContentText("Diese Fahrstrasse enthält Fahrten, die bereits aktiviert sind in anderen Fahrstraßen.");
                            alert.showAndWait();
                        }
                    }
                }
            }
        });

        ivHelp.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            showManual();
            event.consume();
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

    public static void showManual() {
        String content = MainUI.class.getResource("/de/blankedv/sx4/res/docs/index.html").toExternalForm();
        WebView mWebView = new WebView();
        WebEngine webEngine = mWebView.getEngine();
        webEngine.load(content);
        Scene scene = new Scene(mWebView, 900.0, 500.0);
        manualStage.setTitle("Handbuch SX4 Fahrplan");
        // Add  the Scene to the Stage
        manualStage.setScene(scene);
        // Display the Stage
        manualStage.show();
    }
}
