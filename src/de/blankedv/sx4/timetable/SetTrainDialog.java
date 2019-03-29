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
import static de.blankedv.sx4.Constants.INVALID_INT;
import static de.blankedv.sx4.timetable.Vars.allLocos;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;
import static de.blankedv.sx4.timetable.Vars.panelElements;
import java.util.ArrayList;
import java.util.Collections;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

/**
 *
 * @author mblank
 */
public class SetTrainDialog {

    final static Spinner<Integer> loco = new Spinner<>(0, 9, 0);
    final static Spinner<Integer> spinner10 = new Spinner<>(0, 9, 0);
    final static Spinner<Integer> spinner1 = new Spinner<>(0, 9, 0);
    final static Label lblAdr = new Label(" Adresse");

    static SensorLocoPair open(Stage primaryStage) {

        final SensorLocoPair result = new SensorLocoPair();

        ArrayList<Integer> sensorAddresses = new ArrayList<>();
        // get all sensors
        for (PanelElement pe : panelElements) {
            if (pe.isSensor()) {
                sensorAddresses.add(pe.getAdr());
            }
        }
        
        if (sensorAddresses.size() == 0) {
            error("no sensors, cannot set train");
            return result;
        }
        Collections.sort(sensorAddresses);
        ArrayList<Integer> locoAddresses = new ArrayList<>();
        // get all locos from all trips
        locoAddresses.add(0);  // 0 == no loco
        for (Loco lo : allLocos) {
            int a = lo.getAddr();
            if (!locoAddresses.contains(a)) {
                locoAddresses.add(a);
            }
        }
        Collections.sort(locoAddresses);

        final ChoiceBox<Integer> sensors = new ChoiceBox<Integer>(FXCollections.observableArrayList(
                sensorAddresses)
        );
        final ChoiceBox<Integer> locos = new ChoiceBox<Integer>(FXCollections.observableArrayList(
                locoAddresses)
        );
        
        sensors.getSelectionModel().select(0);
        sensors.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                debug("sens="+newValue+" train="+PanelElement.getTrainFromSensor(newValue));
                int l = PanelElement.getTrainFromSensor(newValue);
                locos.getSelectionModel().select(new Integer(l));
            }
        });
       

        
        if (sensorAddresses.size() >= 1) {
            int l = PanelElement.getTrainFromSensor(sensorAddresses.get(0));
            locos.getSelectionModel().select(new Integer(l));
            debug("sens=" + sensorAddresses.get(0) + " loco=" + l);
        } else {
            locos.getSelectionModel().select(0);
            debug("no sensorAddresses");
        }

        Label lblSensor = new Label("Sensor (Gleis)");
        lblSensor.setAlignment(Pos.CENTER);
        Label lblLoco = new Label("Lok/Zug");
        lblLoco.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setVgap(20);
        grid.setHgap(20);
        grid.add(lblSensor, 0, 1);
        grid.add(lblLoco, 1, 1);

        grid.add(sensors, 0, 2);
        grid.add(locos, 1, 2);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHalignment(HPos.CENTER);

        grid.getColumnConstraints().addAll(col1, col2);

        GridPane.setMargin(grid, new Insets(5, 5, 5, 5));

        Button btnCancel = new Button("zurück");

        Button btnSave = new Button("  OK  ");

        grid.add(btnCancel, 0, 3);
        grid.add(btnSave, 1, 3);
        //GridPane.setMargin(btnCancel, new Insets(5, 5, 5, 5));

        Scene secondScene = new Scene(grid, 320, 160);
        // New window (Stage)
        Stage newWindow = new Stage();
        btnCancel.setOnAction((e) -> {
            result.sensor = INVALID_INT;
            newWindow.close();
        });
        btnSave.setOnAction((e) -> {
            result.sensor = sensors.getSelectionModel().getSelectedItem();
            result.loco = locos.getSelectionModel().getSelectedItem();
            newWindow.close();
        });
        newWindow.setTitle("Sensor / Zug (Lok) Zuordnung setzen");
        newWindow.setScene(secondScene);

        // Specifies the modality for new window.
        newWindow.initModality(Modality.WINDOW_MODAL);

        // Specifies the owner Window (parent) for new window
        newWindow.initOwner(primaryStage);

        // Set position of second window, related to primary window.
        newWindow.setX(primaryStage.getX() + 200);
        newWindow.setY(primaryStage.getY() + 100);

        newWindow.showAndWait();

        return result;
    }
}
