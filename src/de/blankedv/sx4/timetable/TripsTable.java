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
import static de.blankedv.sx4.SX4.configFilename;
import de.blankedv.sx4.SXData;
import de.blankedv.sx4.timetable.Trip;
import de.blankedv.sx4.timetable.Trip.TripState;
import static de.blankedv.sx4.timetable.Vars.allCompRoutes;
import static de.blankedv.sx4.timetable.Vars.allRoutes;
import static de.blankedv.sx4.timetable.Vars.allTimetables;
import static de.blankedv.sx4.timetable.Vars.allTrips;
import static de.blankedv.sx4.timetable.Vars.panelName;
import java.util.Collections;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 *
 * @author mblank
 */
public class TripsTable extends Application {

    public static final TableView<Trip> tableView = new TableView<>();

    Scene tripTableScene;
    // New window (Stage)
    Stage tripWindow;

    private final Image green = new Image("/de/blankedv/sx4/res/greendot.png");
    private final Image red = new Image("/de/blankedv/sx4/res/reddot.png");
    private final Image refresh = new Image("/de/blankedv/sx4/res/refresh.png");
    private final ImageView ivPowerState = new ImageView();
    private final ImageView ivRefresh = new ImageView(refresh);
    private final Button btnRefresh = new Button();
    private final Button btnSetTrain = new Button("SetTrain");
    private final Button btnReset = new Button("Reset");
    final Button btnStart = new Button("Start");
    final Button btnStop = new Button("Stop");
    private Timetable t0;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {

        primaryStage = stage;
        BorderPane bp = new BorderPane();
        Label status = new Label();

        HBox hb = createButtonBar();

        bp.setTop(hb);
        //hb.setAlignment(Pos.CENTER);
        BorderPane.setMargin(hb, new Insets(8, 8, 8, 8));
        tripTableScene = new Scene(bp, 700, 300);
        bp.setCenter(tableView);
        bp.setBottom(status);

        t0 = allTimetables.get(0);
        if (t0 != null) {
            status.setText(t0.toString());
        } else {
            status.setText("kein Fahrplan vorhanden");
        }

        // New window (Stage)

        /* btnClose.setOnAction((e) -> {
            //sxAddress.addr = -1;
            tripWindow.close();
        }); */
        primaryStage.setTitle("Fahrplan " + panelName);
        primaryStage.setScene(tripTableScene);

        createDataTables();

        primaryStage.show();

        final Timeline second = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            // update power control icon
            if (SXData.getActualPower() == true) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }

            // look for active trip and enable/disable refresh button and start/stop buttons
            int tripsActive = INVALID_INT;
            for (Trip tr : allTrips) {
                if (tr.state != TripState.INACTIVE) {
                    tripsActive = tr.id;
                    tableView.getSelectionModel().select(tr);
                }
            }
            if (tripsActive != INVALID_INT) {
                btnRefresh.setDisable(true);
                status.setText(t0.toString() + " läuft.");
                btnStop.setDisable(false);
                btnStart.setDisable(true);
            } else {
                btnRefresh.setDisable(false);
                status.setText(t0.toString());
                btnStop.setDisable(true);
                btnStart.setDisable(false);
            }
        }));

        second.setCycleCount(Timeline.INDEFINITE);
        second.play();

    }

    public void show() {
        tripWindow.show();
    }

    public void selectTrip(Trip tr) {

    }

    private void createDataTables() {
        // <trip id="3100" routeid="2300" sens1="924" sens2="902" loco="29,1,126" stopdelay="1500" />
        TableColumn<Trip, Integer> idCol = new TableColumn<>("ID");
        TableColumn<Trip, Integer> routeidCol = new TableColumn<>("Fahrstr.");
        TableColumn<Trip, Integer> sens1Col = new TableColumn<>("Start");
        TableColumn<Trip, Integer> sens2Col = new TableColumn<>("Ende");
        TableColumn<Trip, String> locoCol = new TableColumn<>("Zug,Dir,Speed");
        TableColumn<Trip, Integer> stopDelayCol = new TableColumn<>("StopDelay");
        /* final TextFormatter<String> formatter = new TextFormatter<String>(change -> {
            change.setText(change.getText().replaceAll("[^0-9.,]", ""));
            return change;

        }); */
        StringConverter myStringIntConverter = new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == null) {
                    return "-";
                }
                return "" + object;
            }

            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        };

        tableView.getColumns().setAll(idCol, routeidCol, sens1Col, sens2Col, locoCol, stopDelayCol);
        tableView.setEditable(true);
        //idCol.setCellFactory(TextFieldTableCell.forTableColumn());
        /*idCol.setCellFactory(TextFieldTableCell.forTableColumn(myStringIntConverter));
        idCol.setOnEditCommit(new EventHandler<CellEditEvent<Route, Integer>>() {
            @Override
            public void handle(CellEditEvent<Route, Integer> ev) {
                ((Route) ev.getTableView().getItems().get(
                        ev.getTablePosition().getRow())).setRoute("" + ev.getNewValue());
            }
        }); */

 /*routeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        routeCol.setOnEditCommit(new EventHandler<CellEditEvent<Trip, String>>() {
            @Override
            public void handle(CellEditEvent<Route, String> ev) {
                ((Trip) ev.getTableView().getItems().get(
                        ev.getTablePosition().getRow())).setRoute(ev.getNewValue());
            }
        });
        sensorsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        sensorsCol.setOnEditCommit(new EventHandler<CellEditEvent<Trip, String>>() {
            @Override
            public void handle(CellEditEvent<Route, String> ev) {
                ((Trip) ev.getTableView().getItems().get(
                        ev.getTablePosition().getRow())).setSensors(ev.getNewValue());
            }
        });
            tableViewData[i].setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            chanCol.setMaxWidth(1f * Integer.MAX_VALUE * 18); // 30% width
            chanCol.setStyle("-fx-alignment: CENTER;");
            dataCol.setMaxWidth(1f * Integer.MAX_VALUE * 18); // 70% width
            dataCol.setStyle("-fx-alignment: CENTER;"); */
        tableView.setCenterShape(true);
        tableView.setRowFactory(new Callback<TableView<Trip>, TableRow<Trip>>() {
            @Override
            public TableRow<Trip> call(TableView<Trip> tableView) {
                final TableRow<Trip> row = new TableRow<>();
                final ContextMenu contextMenu = new ContextMenu();
                final MenuItem startMenuItem = new MenuItem("Starte diese Fahrt");
                startMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        final Trip tr = row.getItem();
                        startTrip(tr);
                    }
                });
                final MenuItem stopMenuItem = new MenuItem("Stoppe diese Fahrt");
                stopMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        final Trip tripShown = row.getItem();
                        tripShown.finish();
                    }
                });
                contextMenu.getItems().addAll(startMenuItem, stopMenuItem);
                // Set context menu on row, but use a binding to make it only show for non-empty rows:
                row.contextMenuProperty().bind(
                        Bindings.when(row.emptyProperty())
                                .then((ContextMenu) null)
                                .otherwise(contextMenu)
                );

                return row;
            }
        });
        /*   tableView.setRowFactory(new Callback<TableView<RouteData>, TableRow<RouteData>>() {
                @Override
                public TableRow<RouteData> call(TableView<RouteData> tableView) {
                    final TableRow<RouteData> row = new TableRow<RouteData>() {
                        @Override
                        protected void updateItem(Route sxv, boolean empty) {
                            super.updateItem(sxv, empty);
                            if (!empty) {
                                if (sxv.isMarked()) {
                                    setStyle("-fx-background-color: yellow;");
                                } else {
                                    setStyle("");
                                }
                            } else {
                                setStyle("");
                            }
                        }
                    };
                    return row;
                }
            }); */

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        routeidCol.setCellValueFactory(new PropertyValueFactory<>("routeid"));
        sens1Col.setCellValueFactory(new PropertyValueFactory<>("sens1"));
        sens2Col.setCellValueFactory(new PropertyValueFactory<>("sens2"));
        locoCol.setCellValueFactory(new PropertyValueFactory<>("locoString"));
        stopDelayCol.setCellValueFactory(new PropertyValueFactory<>("stopDelay"));

        tableView.setItems(allTrips);

        // textField.setTextFormatter(formatter);
        Utils.customResize(tableView);

    }

    private void startTrip(Trip trip) {

        if (trip != null) {
            if (SXData.getActualPower() == true) {
                trip.start();
            } else {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error alert");
                alert.setHeaderText(null);
                alert.setContentText("Start der Fahrt nicht möglich, da keine Gleisspannung!");
                alert.showAndWait();
            }
        }
    }

    private HBox createButtonBar() {

        final HBox hb = new HBox(15);

        // final ProgressIndicator pi = new ProgressIndicator();
        // pi.setVisible(false);
        btnRefresh.setGraphic(ivRefresh);
        btnStop.setDisable(true);

        btnStart.setOnAction(e -> {
            btnStop.setDisable(false);
            btnStart.setDisable(true);

            Timetable tt = allTimetables.get(0);
            boolean result = tt.start();
            if (result == false) {
                // reset button states if start was not successful
                btnStop.setDisable(true);
                btnStart.setDisable(false);
            }
        }
        );

        btnStop.setOnAction(e -> {
            btnStop.setDisable(true);
            btnStart.setDisable(false);
            Timetable tt = allTimetables.get(0);
            tt.stop();
            //tableView.getSelectionModel().clearAndSelect(0);
        }
        );

        btnRefresh.setOnAction(e -> {
            // doublecheck that no trip is active
            int tripsActive = INVALID_INT;
            for (Trip tr : allTrips) {
                if ((tr.state == TripState.ACTIVE) || (tr.state == TripState.WAITING)) {
                    tripsActive = tr.id;
                }
            }
            if (tripsActive != INVALID_INT) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error alert");
                alert.setHeaderText(null);
                alert.setContentText("Refresh nicht möglich, da Trip " + tripsActive + "gerade läuft!");
                alert.showAndWait();
            } else {
                //  pi.setVisible(true);  TIME TOO SHORT, cannot be seen

                ReadConfig.refreshXMLTrips(configFilename);
                Collections.sort(allTrips, (a, b) -> b.compareTo(a));
                //  pi.setVisible(false);
            }
        });

        btnReset.setOnAction(e -> {
            PanelElement.unlockAll();
            for (Route rt : allRoutes) {
                rt.clear();
            }
            for (CompRoute cr : allCompRoutes) {
                cr.clear();
            }
            btnStop.setDisable(true);
            btnStart.setDisable(false);
            Timetable tt = allTimetables.get(0);
            tt.stop();
        }
        );

        btnSetTrain.setOnAction(e -> {
            SensorLocoPair res = SetTrainDialog.open(primaryStage);
            if (res.sensor != INVALID_INT) {
                PanelElement.setTrain(res.sensor, res.loco);
            };
        }
        );

        ivPowerState.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // toggle global power
            if (SXData.getActualPower()) {
                SXData.setPowerToBe(false);
                ivPowerState.setImage(red);
            } else {
                SXData.setPowerToBe(true);
                ivPowerState.setImage(green);
            }
            event.consume();
        });

        /*globalPower.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }
        }); */
        Label lblNothing = new Label("  ");
        HBox.setHgrow(lblNothing, Priority.ALWAYS);
        lblNothing.setMaxWidth(Double.MAX_VALUE);
        hb.getChildren().addAll(ivPowerState, btnStart, btnStop, btnRefresh, lblNothing, btnSetTrain, btnReset); // , pi);
        return hb;
    }

}
