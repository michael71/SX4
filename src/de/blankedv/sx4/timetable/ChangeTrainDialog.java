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

import static de.blankedv.sx4.Constants.INVALID_INT;
import static de.blankedv.sx4.timetable.Vars.allLocos;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;
import java.util.ArrayList;
import java.util.Collections;
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
public class ChangeTrainDialog {

    final static Spinner<Integer> speed2 = new Spinner<>(1, 31, 0);
    final static Label speed1 = new Label();

    static LocoSpeedPairs open(Stage primaryStage) {

        final LocoSpeedPairs result = new LocoSpeedPairs();

        ArrayList<Integer> locoAddresses = new ArrayList<>();
        // get all locos from all trips
        for (Loco lo : allLocos) {
            int a = lo.getAddr();
            if (!locoAddresses.contains(a)) {
                locoAddresses.add(a);
            }
        }
        if (locoAddresses.isEmpty()) {
            return new LocoSpeedPairs();
        }

        Collections.sort(locoAddresses);

        final ChoiceBox<Integer> locos1 = 
                new ChoiceBox<>(FXCollections.observableArrayList(locoAddresses)
        );
        locos1.getSelectionModel().select(0);
        speed1.setText("" + getSpeed(locoAddresses.get(0)));

        locos1.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Integer> observableValue, Integer oIndex, Integer nIndex) -> {
                    int loco = nIndex;
                    int s = getSpeed(loco);
                    System.out.println("new loco1 sel=" + loco + " s=" + s);
                    speed1.setText("" + s);
                });

        final ChoiceBox<Integer> locos2 = 
                new ChoiceBox<>(FXCollections.observableArrayList(locoAddresses)
        );
        locos2.getSelectionModel().select(0);
        locos2.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Integer> observableValue, Integer oIndex, Integer nIndex) -> {
                    int loco = nIndex;
                    int s = getSpeed(loco);
                    speed2.getValueFactory().setValue(s);
                });
        
        speed2.getValueFactory().setValue(getSpeed(locoAddresses.get(0)));

        Label lblSensor = new Label("Sensor");
        lblSensor.setAlignment(Pos.CENTER);
        Label lblLoco = new Label("Zug");
        lblLoco.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setVgap(20);
        grid.setHgap(20);
        grid.add(new Label("von"), 1, 0);
        grid.add(new Label("ändern zu"), 2, 0);
        grid.add(new Label("Zug-Nr."), 0, 1);
        grid.add(new Label("Geschw."), 0, 2);

        grid.add(locos1, 1, 1);
        grid.add(locos2, 2, 1);
        grid.add(speed1, 1, 2);
        grid.add(speed2, 2, 2);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30);
        col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(30);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(30);
        col3.setHalignment(HPos.CENTER);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(10);
        col4.setHalignment(HPos.CENTER);

        grid.getColumnConstraints().addAll(col1, col2, col3, col4);

        GridPane.setMargin(grid, new Insets(5, 5, 5, 5));

        Button btnCancel = new Button("zurück");

        Button btnSave = new Button("  OK  ");

        grid.add(btnCancel, 1, 3);
        grid.add(btnSave, 2, 3);
        //GridPane.setMargin(btnCancel, new Insets(5, 5, 5, 5));

        Scene secondScene = new Scene(grid, 320, 160);
        // New window (Stage)
        Stage newWindow = new Stage();
        btnCancel.setOnAction((e) -> {
            result.loco1 = INVALID_INT;
            newWindow.close();
        });
        btnSave.setOnAction((e) -> {
            result.loco1 = locos1.getSelectionModel().getSelectedItem();
            result.speed1 = getSpeed(result.loco1);
            result.loco2 = locos2.getSelectionModel().getSelectedItem();
            result.speed2 = speed2.getValue();
            newWindow.close();
        });
        newWindow.setTitle("Zug-Nummer temporär ändern");
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

    public static int getSpeed(int loco) {
        for (Trip tr : allTrips) {
            Pair<Integer, Integer> pAS = getLocoAndSpeed(tr);
            if (pAS.getKey() == loco) {
                return pAS.getValue();
            }
        }
        return 1;
    }

    public static void update(int loco) {

    }

    public static Pair<Integer, Integer> getLocoAndSpeed(Trip tr) {
        // convert locoString string to int values for address, direction and speed
        String[] lData = tr.locoString.split(",");
        if (lData.length < 2) {
            return new Pair<> (INVALID_INT, 0);
        }

        int locoAddr, locoSpeed;
        try {
            locoAddr = Integer.parseInt(lData[0]);

            if (lData.length >= 3) {
                locoSpeed = Integer.parseInt(lData[2]);
            } else {
                locoSpeed = 28;
            }
            return new Pair<>(locoAddr, locoSpeed);

        } catch (NumberFormatException e) {
            return new Pair<>(INVALID_INT, 0);

        }
    }
}
