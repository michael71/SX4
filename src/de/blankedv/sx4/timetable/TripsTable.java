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
import static de.blankedv.sx4.Constants.NUM_VERSION;
import static de.blankedv.sx4.SX4.configFilename;
import de.blankedv.sx4.SXData;
import de.blankedv.sx4.timetable.Trip.TripState;
import static de.blankedv.sx4.timetable.Vars.MAX_START_STOP_DELAY;
import static de.blankedv.sx4.timetable.VarsFX.allTimetables;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;
import static de.blankedv.sx4.timetable.Vars.panelName;

import java.util.ArrayList;
import java.util.Collections;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 *
 * @author mblank
 */
public class TripsTable extends Application {

    private final TableView<Trip> tableView = new TableView<>();

    private Scene tripTableScene;
    // New window (Stage)
    private Stage tripWindow;

    private final Image green = new Image("/de/blankedv/sx4/res/greendot.png");
    private final Image red = new Image("/de/blankedv/sx4/res/reddot.png");
    private final Image refresh = new Image("/de/blankedv/sx4/res/refresh.png");
    private final Image imgPause = new Image("/de/blankedv/sx4/res/pause2.png");
    private final Image imgStart = new Image("/de/blankedv/sx4/res/start2.png");
    private final Image imgStop = new Image("/de/blankedv/sx4/res/stop2.png");
    private final Image imgHelp = new Image("/de/blankedv/sx4/res/help2.png");
    private final ImageView ivPowerState = new ImageView();
    private final ImageView ivRefresh = new ImageView(refresh);
    private final ImageView ivPause = new ImageView(imgPause);
    private final ImageView ivStart = new ImageView(imgStart);
    private final ImageView ivStop = new ImageView(imgStop);
    private final ImageView ivHelp = new ImageView(imgHelp);
    private final Button btnRefresh = new Button("Fahrten neu laden");
    private final ComboBox<String> cbSelectTimetable = new ComboBox<>();
    private final Button btnChangeTrain = new Button("Zug ändern");
    private final Button btnSetTrain = new Button("Zug setzen");
    private final Button btnReset = new Button("Reset");
    //private final Button btnHelp = new Button();
    private final Button btnStart = new Button();
    private final Button btnStop = new Button();
    //final Button btnPause = new Button();
    private final Pane spacer = new Pane();
    private final Pane spacer2 = new Pane();

    public Timetable ttSelected = null;

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

        cbSelectTimetable.valueProperty().addListener((ov, oldV, newV) -> {   // String values
            if (newV != null) {
                status.setText(newV);
                // extract number and load new timetable
                if (allTimetables.size() > 0) {
                    ttSelected = allTimetables.get(getTTIndex(newV));
                    orderTrips(ttSelected);
                }
            }
        });

        primaryStage.setTitle("Fahrplan " + panelName + " (" + NUM_VERSION + ")");
        primaryStage.setScene(tripTableScene);

        createDataTables();

        primaryStage.show();
        
        primaryStage.setOnCloseRequest((WindowEvent e) -> {
            System.out.println("TripsTable closing");
        });

        // timeline for updating display
        final Timeline second = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            // update power control icon
            if (SXData.getActualPower() == true) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }

            if (ttSelected.getCurrentTripIndex() != INVALID_INT) {
                tableView.getSelectionModel().select(ttSelected.getCurrentTripIndex());
            }

            if (ttSelected == null) {
                btnStop.setDisable(true);
                btnStart.setDisable(true);
                //btnPause.setDisable(true);
                status.setText("kein Fahrplan.");
            } else if (ttSelected.isActive() == true) {
                btnRefresh.setDisable(true);
                status.setText(ttSelected.toString());
                btnStop.setDisable(false);
                btnStart.setDisable(true);
                //btnPause.setDisable(false);
                cbSelectTimetable.setDisable(true);
                if (ttSelected.getCurrentTripIndex() != INVALID_INT) {
                    tableView.getSelectionModel().select(ttSelected.getCurrentTripIndex());
                }
            } else {
                btnRefresh.setDisable(false);
                status.setText(ttSelected.toString());
                btnStop.setDisable(true);
                //btnPause.setDisable(true);
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
                cbTimetables.add("Fahrplan " + tt.getAdr());
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
        allTimetableTrips.addAll(tt.getTripsList());
    }

    public void show() {
        tripWindow.show();
    }

    public boolean startNewTimetable(int timetableAdrToStart) {
        debug("next timetable=" + timetableAdrToStart);
        for (Timetable tt : allTimetables) {
            if (tt.getAdr() == timetableAdrToStart) {
                ttSelected = tt;
                cbSelectTimetable.getSelectionModel().select("Fahrplan " + ttSelected.getAdr());
                ttSelected.start(this);
                return true; // timetable found
            }
        }
        return false;  // did not find a timetable with this address
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

        tableView.getColumns().add(adrCol);
        tableView.getColumns().add(routeCol);
        tableView.getColumns().add(sens1Col);
        tableView.getColumns().add(sens2Col);
        tableView.getColumns().add(startDelayCol);
        tableView.getColumns().add(stopDelayCol);
        tableView.getColumns().add(locoCol);

        tableView.setEditable(true);

        tableView.setCenterShape(true);
        tableView.setRowFactory((TableView<Trip> tableView1) -> {
            final TableRow<Trip> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem setTrainMenuItem = new MenuItem("Zug setzen");
            setTrainMenuItem.setOnAction((ActionEvent event) -> {
                final Trip tr = row.getItem();
                int resLoco = SelectLocoDialog.open(primaryStage, tr.sens1);
                if ((resLoco != INVALID_INT) && (resLoco != 0)) {
                    PanelElement.setTrain(tr.sens1, resLoco);
                    debug("Sensor " + tr.sens1 + " belegt mit Zug#" + resLoco);
                }
            });
            final MenuItem startMenuItem = new MenuItem("Starte diese Fahrt");
            startMenuItem.setOnAction((ActionEvent event) -> {
                final Trip tr = row.getItem();
                startTripManually(tr);
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

        startDelayCol.setCellFactory(TextFieldTableCell.forTableColumn(new MyIntegerStringConverter()));

        startDelayCol.setOnEditCommit((CellEditEvent<Trip, Integer> ev) -> {
            Trip to = ev.getTableView().getItems().get(ev.getTablePosition().getRow());
            if ((ev.getNewValue() >= 0) && (ev.getNewValue() < MAX_START_STOP_DELAY)) {
                UpdateXML.setTripDelay(ev.getRowValue().adr, ev.getNewValue(), true);  
                to.startDelay = ev.getNewValue();
            } else {
                invalidDelay(ev.getNewValue());
                //to.startDelay = ev.getOldValue();
            }
            startDelayCol.setVisible(false);  // needed because of a JavaFX bug
            startDelayCol.setVisible(true);
        });

        stopDelayCol.setCellFactory(TextFieldTableCell.forTableColumn(new MyIntegerStringConverter()));
        stopDelayCol.setOnEditCommit((CellEditEvent<Trip, Integer> ev) -> {
            Trip to = ev.getTableView().getItems().get(ev.getTablePosition().getRow());
            if ((ev.getNewValue() >= 0) && (ev.getNewValue() < MAX_START_STOP_DELAY)) {
                UpdateXML.setTripDelay(ev.getRowValue().adr, ev.getNewValue(), false);
                to.stopDelay = ev.getNewValue();
            } else {
                invalidDelay(ev.getNewValue());
            }
            stopDelayCol.setVisible(false);   // needed because of a JavaFX bug
            stopDelayCol.setVisible(true);
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

    private void invalidDelay(int value) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error alert");
        alert.setHeaderText(null);
        alert.setContentText("Start/Stop-Delay muss im Bereich 0 ... 100000 liegen!");
        alert.showAndWait();
    }

    private void startTripManually(Trip trip) {
        if (trip != null) {
            if (globalPowerCheck()) {
                boolean result = trip.start();
                if (!result) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error alert");
                    alert.setHeaderText(null);
                    alert.setContentText("Start der Fahrt nicht möglich: " + trip.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    private boolean globalPowerCheck() {
        if (SXData.getActualPower() == true) {
            return true;
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error alert");
            alert.setHeaderText(null);
            alert.setContentText("Start der Fahrt nicht möglich, da keine Gleisspannung!");
            alert.showAndWait();
            return false;
        }
    }

    // get timetable index from selected String, like "Fahrstraße 3300" -> 3300
    private int getTTIndex(String s) {
        int ttIndex = 0;
        for (Timetable tt : allTimetables) {
            if (s.contains("" + tt.getAdr())) {
                return ttIndex;
            }
            ttIndex++;
        }
        return 0;  // if not found, return 0
    }

    private VBox createButtonBar() {

        final HBox hb = new HBox(15);     // first line of button
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox hb2 = new HBox(15);    // for second line of buttons
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        final VBox vb = new VBox(5);

        // final ProgressIndicator pi = new ProgressIndicator();
        // pi.setVisible(false);
        btnRefresh.setGraphic(ivRefresh);
        btnStart.setGraphic(ivStart);
        btnStop.setDisable(true);
        btnStop.setGraphic(ivStop);
        //btnHelp.setGraphic(ivHelp);
        //btnPause.setDisable(true);
        //btnPause.setGraphic(ivPause);

        btnStart.setOnAction(e -> {
            if (!globalPowerCheck()) {
                return;
            }
            if (!checkTrainsPositions()) {
                return;
            }
            btnStop.setDisable(false);
            btnStop.requestFocus();
            //btnPause.setDisable(false);
            btnStart.setDisable(true);
            cbSelectTimetable.setDisable(true);
            ttSelected.start(this);
            if (ttSelected.isActive() == false) {
                // reset button states if start was not successful
                btnStop.setDisable(true);
                //btnPause.setDisable(true);
                btnStart.setDisable(false);
                cbSelectTimetable.setDisable(false);
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error alert");
                alert.setHeaderText(null);
                alert.setContentText(ttSelected.getMessage());
                alert.showAndWait();
            }
        }
        );

        btnStop.setOnAction(e -> {
            btnStop.setDisable(true);
            //btnPause.setDisable(true);
            btnStart.setDisable(false);
            btnStart.requestFocus();
            cbSelectTimetable.setDisable(false);
            ttSelected.stop();
            tableView.getSelectionModel().clearSelection();
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

        ivHelp.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            showManual(primaryStage);
            event.consume();
        });

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
            }
        }
        );
        btnSetTrain.setOnAction((ActionEvent e) -> {
            SensorLocoPair res = SetTrainDialog.open(primaryStage);
            if (res.sensor != INVALID_INT) {
                PanelElement.setTrain(res.sensor, res.loco);
                debug("Sensor " + res.sensor + " belegt mit Zug#" + res.loco);
            }
        });

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

        Label lblNothing = new Label("  ");
        HBox.setHgrow(lblNothing, Priority.ALWAYS);
        lblNothing.setMaxWidth(Double.MAX_VALUE);
        hb.getChildren().addAll(ivPowerState, btnStart, /* btnPause, */ btnStop, spacer, btnRefresh); // , pi);
        hb2.getChildren().addAll(cbSelectTimetable, btnChangeTrain, btnSetTrain, spacer2, ivHelp); // btnReset);
        vb.getChildren().addAll(hb, hb2);
        return vb;
    }

    private boolean checkTrainsPositions() {
        if (ttSelected == null) {
            System.out.println("ERROR: no timetable -> cannot start any trip");
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error alert");
            alert.setHeaderText(null);
            alert.setContentText("no timetable -> cannot start any trip");
            alert.showAndWait();
            return false;
        }

        String msg = ttSelected.checkPositions();
        if (msg.isEmpty()) {
            return true;
        } else {
            System.out.println("ERROR: start positions for trains not valid.");
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error alert");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
            return false;
        }
    }

    private void showManual(Stage primStage) {
        Stage wvStage = new Stage();
        wvStage.initOwner(primStage);

        WebView mWebView = new WebView();
        WebEngine webEngine = mWebView.getEngine();
        webEngine.load(this.getClass().getResource("/de/blankedv/sx4/res/docs/index.html").toExternalForm());
        Scene scene = new Scene(mWebView, 900.0, 500.0);
        wvStage.setTitle("Handbuch SX4 Fahrplan");

        // Add  the Scene to the Stage
        wvStage.setScene(scene);
        // Display the Stage
        wvStage.show();
    }

    public void auto() {
        if (ttSelected != null) {
            ttSelected.timetableCheck();
        }
    }

}
