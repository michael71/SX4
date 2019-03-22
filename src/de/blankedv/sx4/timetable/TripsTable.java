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
import static de.blankedv.sx4.Constants.INVALID_INT;
import static de.blankedv.sx4.SX4.configFilename;
import de.blankedv.sx4.SXData;
import de.blankedv.sx4.timetable.Trip.TripState;
import static de.blankedv.sx4.timetable.Vars.allCompRoutes;
import static de.blankedv.sx4.timetable.Vars.allRoutes;
import static de.blankedv.sx4.timetable.VarsFX.allTimetables;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;
import static de.blankedv.sx4.timetable.Vars.panelName;
import java.util.ArrayList;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    private final Button btnRefresh = new Button("Fahrten neu laden");
    private final ComboBox cbSelectTimetable = new ComboBox();
    private final Button btnChangeTrain = new Button("Zug temp. austauschen");
    private final Button btnSetTrain = new Button("Zug setzen");
    private final Button btnReset = new Button("Reset");
    final Button btnStart = new Button("Start");
    final Button btnStop = new Button("Stop");
    private static Timetable ttSelected = null;  // TODO enable multiple timetables
    private Stage primaryStage;
    private final Label status = new Label();
    private final ArrayList<String> cbTimetables = new ArrayList<>();
    private final ObservableList<Trip> allTimetableTrips = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {

        primaryStage = stage;
        BorderPane bp = new BorderPane();

        VBox buttons = createButtonBar();

        bp.setTop(buttons);
        //hb.setAlignment(Pos.CENTER);
        BorderPane.setMargin(buttons, new Insets(8, 8, 8, 8));
        tripTableScene = new Scene(bp, 700, 300);
        bp.setCenter(tableView);
        bp.setBottom(status);
        debug("starting TripsTable");
        // already done in SX4 (if guiEnabled): ReadConfigTrips.readTripsAndTimetables(configFilename);
        checkTimetables();

        cbSelectTimetable.valueProperty().addListener((ov, oldV, newV) -> {
            if (newV != null) {
                status.setText(newV.toString());
                // extract number and load new timetable
                if (allTimetables.size() > 0) {
                    ttSelected = allTimetables.get(getTTIndex(newV.toString()));
                    orderTrips(ttSelected);
                }
            }
        });
        // New window (Stage)

        /* btnClose.setOnAction((e) -> {
            //sxAddress.addr = -1;
            tripWindow.close();
        }); */
        primaryStage.setTitle("Fahrplan " + panelName);
        primaryStage.setScene(tripTableScene);

        createDataTables();

        primaryStage.show();

        // timeline for updating display
        final Timeline second = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            // update power control icon
            if (SXData.getActualPower() == true) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }

            // look for active trip and enable/disable refresh button and start/stop buttons
            int tripsActive = INVALID_INT;
            for (Trip tr : allTimetableTrips) {
                if (tr.state != TripState.INACTIVE) {
                    tripsActive = tr.adr;
                    tableView.getSelectionModel().select(tr);
                }
            }
            if (ttSelected == null) {
                btnStop.setDisable(true);
                btnStart.setDisable(true);
                status.setText("kein Fahrplan.");
            } else if (ttSelected.isActive() == true) {
                btnRefresh.setDisable(true);
                status.setText(ttSelected.toString() + " läuft.");
                btnStop.setDisable(false);
                btnStart.setDisable(true);
                cbSelectTimetable.setDisable(true);
            } else {
                btnRefresh.setDisable(false);
                status.setText(ttSelected.toString());
                btnStop.setDisable(true);
                btnStart.setDisable(false);
                cbSelectTimetable.setDisable(false);
            }
        }));

        second.setCycleCount(Timeline.INDEFINITE);
        second.play();

    }

    private void checkTimetables() {
        if (!allTimetables.isEmpty()) {
            ttSelected = allTimetables.get(0);
            status.setText(ttSelected.toString());
            cbTimetables.clear();
            for (Timetable tt : allTimetables) {
                cbTimetables.add("Fahrplan " + tt.adr);
            }
            cbSelectTimetable.getItems().clear();
            cbSelectTimetable.getItems().addAll(cbTimetables);
            cbSelectTimetable.getSelectionModel().select(0);
            cbSelectTimetable.setDisable(false);
            orderTrips(ttSelected);
        } else {
            status.setText("kein Fahrplan vorhanden");
            cbSelectTimetable.setDisable(true);
        }
    }

    private void orderTrips(Timetable tt) {
        allTimetableTrips.clear();
        for (Integer a : tt.tripAdrs) {
            for (Trip tr : allTrips) {
                if (a == tr.adr) {
                    allTimetableTrips.add(tr);
                }
            }
        }
    }

    public void show() {
        tripWindow.show();
    }

    public void selectTrip(Trip tr) {

    }

    private void createDataTables() {
        // <trip adr="3100" route="2300" sens1="924" sens2="902" loco="29,1,126" stopdelay="1500" />
        TableColumn<Trip, Integer> adrCol = new TableColumn<>("Adr(ID)");
        TableColumn<Trip, Integer> routeCol = new TableColumn<>("Fahrstr.");
        TableColumn<Trip, Integer> sens1Col = new TableColumn<>("Start");
        TableColumn<Trip, Integer> sens2Col = new TableColumn<>("Ende");
        TableColumn<Trip, String> locoCol = new TableColumn<>("Zug,Dir,Speed");
        TableColumn<Trip, Integer> startDelayCol = new TableColumn<>("StartDelay[ms]");
        TableColumn<Trip, Integer> stopDelayCol = new TableColumn<>("StopDelay[ms]");
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

        tableView.getColumns().add(adrCol);
        tableView.getColumns().add(routeCol);
        tableView.getColumns().add(sens1Col);
        tableView.getColumns().add(sens2Col);
        tableView.getColumns().add(startDelayCol);
        tableView.getColumns().add(stopDelayCol);
        tableView.getColumns().add(locoCol);

        tableView.setEditable(true);
        //idCol.setCellFactory(TextFieldTableCell.forTableColumn());
        /*adrCol.setCellFactory(TextFieldTableCell.forTableColumn(myStringIntConverter));
        adrCol.setOnEditCommit(new EventHandler<CellEditEvent<Route, Integer>>() {
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
        tableView.setRowFactory((TableView<Trip> tableView1) -> {
            final TableRow<Trip> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem setTrainMenuItem = new MenuItem("Zug setzen");
            setTrainMenuItem.setOnAction((ActionEvent event) -> {
                final Trip tr = row.getItem();
                int resLoco = SelectLocoDialog.open(primaryStage,tr.sens1);
                if ((resLoco != INVALID_INT) && (resLoco != 0)) {
                    PanelElement.setTrain(tr.sens1, resLoco);
                    debug("Sensor "+tr.sens1+" belegt mit Zug#"+resLoco);
                };
            });
            final MenuItem startMenuItem = new MenuItem("Starte diese Fahrt");
            startMenuItem.setOnAction((ActionEvent event) -> {
                final Trip tr = row.getItem();
                startTrip(tr);
            });
            final MenuItem stopMenuItem = new MenuItem("Stoppe diese Fahrt");
            stopMenuItem.setOnAction((ActionEvent event) -> {
                final Trip tripShown = row.getItem();
                tripShown.finish();
            });
            contextMenu.getItems().addAll(setTrainMenuItem, startMenuItem, stopMenuItem);
            // Set context menu on row, but use a binding to make it only show for non-empty rows:
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            return row;
        });

        adrCol.setCellValueFactory(new PropertyValueFactory<>("adr"));
        routeCol.setCellValueFactory(new PropertyValueFactory<>("route"));
        sens1Col.setCellValueFactory(new PropertyValueFactory<>("sens1"));
        sens2Col.setCellValueFactory(new PropertyValueFactory<>("sens2"));
        locoCol.setCellValueFactory(new PropertyValueFactory<>("locoString"));
        startDelayCol.setCellValueFactory(new PropertyValueFactory<>("startDelay"));
        stopDelayCol.setCellValueFactory(new PropertyValueFactory<>("stopDelay"));

        tableView.setItems(allTimetableTrips);

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

    // get timetable index from selected String, like "Fahrstraße 3300" -> 3300
    private int getTTIndex(String s) {
        int ttIndex = 0;
        for (Timetable tt : allTimetables) {
            if (s.contains("" + tt.adr)) {
                return ttIndex;
            }
            ttIndex++;
        }
        return 0;  // if not found, return 0
    }

    private VBox createButtonBar() {

        final HBox hb = new HBox(15);     // first line of button
        final HBox hb2 = new HBox(15);    // for second line of buttons
        final VBox vb = new VBox(5);

        // final ProgressIndicator pi = new ProgressIndicator();
        // pi.setVisible(false);
        btnRefresh.setGraphic(ivRefresh);
        btnStop.setDisable(true);

        btnStart.setOnAction(e -> {
            btnStop.setDisable(false);
            btnStart.setDisable(true);
            cbSelectTimetable.setDisable(true);
            if (ttSelected == null) {
                System.out.println("ERROR: no timetable -> cannot start any trip");
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error alert");
                alert.setHeaderText(null);
                alert.setContentText("no timetable -> cannot start any trip");
                alert.showAndWait();
            } else {
                ttSelected.start();
                if (ttSelected.isActive() == false) {
                    // reset button states if start was not successful
                    btnStop.setDisable(true);
                    btnStart.setDisable(false);
                    cbSelectTimetable.setDisable(false);
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error alert");
                    alert.setHeaderText(null);
                    alert.setContentText(ttSelected.getMessage());
                    alert.showAndWait();
                }
            }
        }
        );

        btnStop.setOnAction(e -> {
            btnStop.setDisable(true);
            btnStart.setDisable(false);
            cbSelectTimetable.setDisable(false);
            ttSelected.stop();
            //tableView.getSelectionModel().clearAndSelect(0);
        }
        );

        btnRefresh.setOnAction(e -> {
            // doublecheck that no trip is active
            int tripsActive = INVALID_INT;
            for (Trip tr : allTimetableTrips) {
                if ((tr.state == TripState.ACTIVE) || (tr.state == TripState.WAITING)) {
                    tripsActive = tr.adr;
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

                ReadConfigTrips.readTripsAndTimetables(configFilename);
                Collections.sort(allTrips, (a, b) -> b.compareTo(a));
                checkTimetables();
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
            cbSelectTimetable.setDisable(false);
            cbSelectTimetable.getValue();
            Timetable tt = allTimetables.get(0);
            tt.stop();
        }
        );

        btnChangeTrain.setOnAction(e -> {
            LocoSpeedPairs res = ChangeTrainDialog.open(primaryStage);
            if (res.loco1 != INVALID_INT) {
                System.out.println("changing " + res.loco1 + ",*," + res.speed1 + " to "
                        + res.loco2 + ",*," + res.speed2);
                for (Trip tr : allTrips) {
                    tr.locoString = tr.locoString.replace(res.loco1 + ",", res.loco2 + ",");
                    tr.locoString = tr.locoString.replace("," + res.speed1, "," + res.speed2);
                    tr.convertLocoData();
                }
            };
        }
        );
        btnSetTrain.setOnAction(e -> {
            SensorLocoPair res = SetTrainDialog.open(primaryStage);
            if (res.sensor != INVALID_INT) {
                PanelElement.setTrain(res.sensor, res.loco);
                debug("Sensor "+res.sensor+" belegt mit Zug#"+res.loco);
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
        hb.getChildren().addAll(ivPowerState, btnStart, btnStop, btnRefresh); // , pi);
        hb2.getChildren().addAll(cbSelectTimetable, btnChangeTrain, btnSetTrain, btnReset);
        vb.getChildren().addAll(hb, hb2);
        return vb;
    }

    public static void auto() {
        if (ttSelected != null) {
            ttSelected.timetableCheck();
        }
    }
}
