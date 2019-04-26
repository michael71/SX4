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
import static de.blankedv.sx4.Constants.SXMAX_USED;
import de.blankedv.sx4.SXData;
import static de.blankedv.sx4.timetable.Vars.MAX_START_STOP_DELAY;
import static de.blankedv.sx4.timetable.VarsFX.allTrips;

import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
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
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import static de.blankedv.sx4.timetable.Vars.allTimetableUIs;
import java.util.ArrayList;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;

/**
 *
 * @author mblank
 *
 * TODO: when trip.start() does not work because of "route blocked", wait 10
 * secs, then try again TODO low prio: fahrplan options
 *
 */
public class TimetableUI {

    private final TableView<Trip> tableView = new TableView<>();

    private Scene timetableUIScene;
    private final Stage stage;

    private final Image green = new Image("/de/blankedv/sx4/res/greendot.png");
    private final Image red = new Image("/de/blankedv/sx4/res/reddot.png");
    private final Image imgStart = new Image("/de/blankedv/sx4/res/start2.png");
    private final Image imgStop = new Image("/de/blankedv/sx4/res/stop2.png");
    private final Image imgHelp = new Image("/de/blankedv/sx4/res/help2.png");
    private final Image imgRefresh = new Image("/de/blankedv/sx4/res/refresh.png");
    private final ImageView ivPowerState = new ImageView();
    private final ImageView ivStart = new ImageView(imgStart);
    private final ImageView ivStop = new ImageView(imgStop);
    private final ImageView ivHelp = new ImageView(imgHelp);
    private final ImageView ivRefresh = new ImageView(imgRefresh);
    //private final Button btnRefresh = new Button("Fahrten neu laden");

    private final Button btnChangeTrain = new Button("Zug ändern");
    private final Button btnSetTrain = new Button("Zug setzen");
    private final CheckBox cbRepeat = new CheckBox("Wiederh.");

    //private final Button btnHelp = new Button();
    private final Button btnStart = new Button();
    private final Button btnStop = new Button();
    //final Button btnPause = new Button();
    private final Pane spacer = new Pane();

    public Timetable ttSelected;
    private ArrayList<Trip> allMyTrips = new ArrayList<>();

    private final Label status = new Label();
    private final ObservableList<Trip> allTimetableTrips = FXCollections.observableArrayList();

    public TimetableUI(Stage primaryStage, Timetable tt) {
        stage = new Stage();
        stage.initOwner(primaryStage);
        ttSelected = tt;

    }

    public boolean check() {
        // check if we can open this timetable
        allTimetableTrips.clear();
        allMyTrips = ttSelected.getTripsList();
        ArrayList<Trip> tripsToLock = new ArrayList<>();
        String msg = "";
        for (Trip tr : allMyTrips) {
            if (tr.isLocked()) {
                msg += "Trip " + tr.adr + " is locked. ";
            } else {
                // save for later locking
                tripsToLock.add(tr);
            }
        }
        for (Trip tr : tripsToLock) {
            tr.lock();
        }
        if (!msg.isEmpty()) {
            return false;  // cannot activate this timetable, because one of the trips is locked
        }
        return true;
    }

    public void close() {
        stage.close();
    }

    public void start() {

        BorderPane bp = new BorderPane();
        VBox buttons = createButtonBar();

        bp.setTop(buttons);
        //hb.setAlignment(Pos.CENTER);
        BorderPane.setMargin(buttons, new Insets(8, 8, 8, 8));
        timetableUIScene = new Scene(bp, 700, 300);
        bp.setCenter(tableView);
        bp.setBottom(status);
        debug("starting TimetableUI " + ttSelected.getAdr());
        // already done in SX4 (if guiEnabled): ReadConfigTrips.readTripsAndTimetables(configFilename);

        allTimetableTrips.addAll(allMyTrips);

        status.setText(ttSelected.toString());

        stage.setTitle("Fahrplan " + ttSelected.getName() + " (" + ttSelected.getAdr() + ")");
        stage.setScene(timetableUIScene);

        createDataTables();
        allTimetableUIs.add(this);

        int n = allTimetableUIs.size();
        stage.setX((n + 1) * 100.0);
        stage.setY((n + 1) * 100.0);
        stage.show();

        stage.setOnCloseRequest((WindowEvent e) -> {
            allTimetableUIs.remove(this);
            System.out.println("TimetableUI closing");
        });

        // timeline for updating display
        final Timeline second = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            // update power control icon
            if (SXData.getActualPower() == true) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }

            if (ttSelected.isActive() == true) {
                status.setText(ttSelected.toString());
                status.setStyle("-fx-background-color: yellow;");
                btnStop.setDisable(false);
                btnStart.setDisable(true);
                //btnPause.setDisable(false);
                if (ttSelected.getCurrentTripIndex() != INVALID_INT) {
                    tableView.getSelectionModel().select(ttSelected.getCurrentTripIndex());
                }
            } else {
                status.setText(ttSelected.toString());
                status.setStyle("-fx-background-color: lightgrey;");
                btnStop.setDisable(true);
                //btnPause.setDisable(true);
                btnStart.setDisable(false);
            }
        }));

        second.setCycleCount(Timeline.INDEFINITE);
        second.play();

        stage.setOnCloseRequest((WindowEvent e) -> {
            if (ttSelected.isActive()) {
                if (!confirmClose()) {
                    e.consume();   // ==> do nothing
                    return;
                }
            }
            ttSelected.stop();
            unlockTrips();
        });

    }

    public void show() {
        stage.show();
    }

    private boolean confirmClose() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Fahrplan beenden?");
        alert.setHeaderText("Fahrplan beenden, obwohl er noch aktiv ist?");
        alert.setContentText("");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Ja, trotzdem beenden!");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Nein, zurück.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            return true;
        } else {
            return false;
        }
    }

    public int getAddress() {
        return ttSelected.getAdr();
    }

    private void createDataTables() {
        // <trip adr="3100" route="2300" sens1="924" sens2="902" loco="29,1,126" stopdelay="1500" />
        TableColumn<Trip, Integer> adrCol = new TableColumn<>("Adr(ID)");
        TableColumn<Trip, Integer> routeCol = new TableColumn<>("Fahrstr.");
        TableColumn<Trip, Integer> sens1Col = new TableColumn<>("Start");
        TableColumn<Trip, Integer> sens2Col = new TableColumn<>("Ende");
        TableColumn<Trip, Integer> locoCol = new TableColumn<>("Zug");
        TableColumn<Trip, Integer> dirCol = new TableColumn<>("Ri.");
        TableColumn<Trip, Integer> speedCol = new TableColumn<>("Geschw.");
        TableColumn<Trip, Integer> startDelayCol = new TableColumn<>("Start V.[ms]");
        TableColumn<Trip, Integer> stopDelayCol = new TableColumn<>("Stopp V.[ms]");

        tableView.getColumns().add(adrCol);
        tableView.getColumns().add(routeCol);
        tableView.getColumns().add(sens1Col);
        tableView.getColumns().add(sens2Col);
        tableView.getColumns().add(startDelayCol);
        tableView.getColumns().add(stopDelayCol);
        tableView.getColumns().add(locoCol);
        tableView.getColumns().add(dirCol);
        tableView.getColumns().add(speedCol);
        
        adrCol.setStyle( "-fx-alignment: CENTER;");
        routeCol.setStyle( "-fx-alignment: CENTER;");
        sens1Col.setStyle( "-fx-alignment: CENTER;");
        sens2Col.setStyle( "-fx-alignment: CENTER;");
        locoCol.setStyle( "-fx-alignment: CENTER;");
        dirCol.setStyle( "-fx-alignment: CENTER;");
        speedCol.setStyle( "-fx-alignment: CENTER;");
        startDelayCol.setStyle( "-fx-alignment: CENTER;");
        stopDelayCol.setStyle( "-fx-alignment: CENTER;");

        tableView.setEditable(true);

        tableView.setCenterShape(true);
        tableView.setRowFactory((TableView<Trip> tableView1) -> {
            final TableRow<Trip> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem setTrainMenuItem = new MenuItem("Zug setzen");
            setTrainMenuItem.setOnAction((ActionEvent event) -> {
                final Trip tr = row.getItem();
                int resLoco = SelectLocoDialog.open(stage, tr.sens1);
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
            final MenuItem continueMenuItem = new MenuItem("Fahrplan hier fortsetzen");
            continueMenuItem.setOnAction((ActionEvent event) -> {
                final int index = row.getIndex();
                contTimetable(index);
            });
            contextMenu.getItems().addAll(setTrainMenuItem, startMenuItem, stopMenuItem, continueMenuItem);
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

        locoCol.setCellFactory(TextFieldTableCell.forTableColumn(new MyIntegerStringConverter()));
        locoCol.setOnEditCommit((CellEditEvent<Trip, Integer> ev) -> {
            Trip to = ev.getTableView().getItems().get(ev.getTablePosition().getRow());
            if ((ev.getNewValue() > 0) && (ev.getNewValue() <= SXMAX_USED)) {
                String newLocoString = "" + ev.getNewValue() + "," + to.locoDir + "," + to.locoSpeed;
                UpdateXML.setLocoString(to.adr, newLocoString);
                to.locoString = newLocoString;
                to.locoAddr = ev.getNewValue();
            } else {
                invalidLoco(ev.getNewValue());
            }
            locoCol.setVisible(false);   // needed because of a JavaFX bug
            locoCol.setVisible(true);
        });

        dirCol.setCellFactory(TextFieldTableCell.forTableColumn(new MyIntegerStringConverter()));
        dirCol.setOnEditCommit((CellEditEvent<Trip, Integer> ev) -> {
            Trip to = ev.getTableView().getItems().get(ev.getTablePosition().getRow());
            if ((ev.getNewValue() >= 0) && (ev.getNewValue() <= 1)) {
                String newLocoString = "" + to.locoAddr + "," + ev.getNewValue() + "," + to.locoSpeed;
                UpdateXML.setLocoString(to.adr, newLocoString);
                to.locoString = newLocoString;
                to.locoDir = ev.getNewValue();
            } else {
                invalidDir();
            }
            dirCol.setVisible(false);   // needed because of a JavaFX bug
            dirCol.setVisible(true);
        });

        speedCol.setCellFactory(TextFieldTableCell.forTableColumn(new MyIntegerStringConverter()));
        speedCol.setOnEditCommit((CellEditEvent<Trip, Integer> ev) -> {
            Trip to = ev.getTableView().getItems().get(ev.getTablePosition().getRow());
            if ((ev.getNewValue() >= 1) && (ev.getNewValue() <= 31)) {
                String newLocoString = "" + to.locoAddr + "," + to.locoDir + "," + ev.getNewValue();
                UpdateXML.setLocoString(to.adr, newLocoString);
                to.locoString = newLocoString;
                to.locoSpeed = ev.getNewValue();
            } else {
                invalidSpeed(ev.getNewValue());
            }
            speedCol.setVisible(false);   // needed because of a JavaFX bug
            speedCol.setVisible(true);
        });

        adrCol.setCellValueFactory(new PropertyValueFactory<>("adr"));
        routeCol.setCellValueFactory(new PropertyValueFactory<>("route"));
        sens1Col.setCellValueFactory(new PropertyValueFactory<>("sens1"));
        sens2Col.setCellValueFactory(new PropertyValueFactory<>("sens2"));
        locoCol.setCellValueFactory(new PropertyValueFactory<>("locoAddr"));
        dirCol.setCellValueFactory(new PropertyValueFactory<>("locoDir"));
        speedCol.setCellValueFactory(new PropertyValueFactory<>("locoSpeed"));
        startDelayCol.setCellValueFactory(new PropertyValueFactory<>("startDelay"));
        stopDelayCol.setCellValueFactory(new PropertyValueFactory<>("stopDelay"));

        tableView.setItems(allTimetableTrips);

        // textField.setTextFormatter(formatter);
        Utils.customResize(tableView);

    }

    public void unlockTrips() {
        debug("unlocking trips");
        for (Trip tr : allMyTrips) {
            tr.unlock();
        }
    }

    private void invalidDelay(int value) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error alert");
        alert.setHeaderText(null);
        alert.setContentText("Start/Stop-Delay muss im Bereich 0 ... 100000 liegen!");
        alert.showAndWait();
    }

    private void invalidLoco(int value) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error alert");
        alert.setHeaderText(null);
        alert.setContentText("LocoAdresse muss im Bereich 0 .." + SXMAX_USED + " liegen!");
        alert.showAndWait();
    }

    private void invalidDir() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error alert");
        alert.setHeaderText(null);
        alert.setContentText("Dir muss 0 oder 1 sein");
        alert.showAndWait();
    }

    private void invalidSpeed(int value) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error alert");
        alert.setHeaderText(null);
        alert.setContentText("Loco Speed muss im Bereich 1 ..31 liegen!");
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

    private void contTimetable(int index) {

        if (globalPowerCheck()) {
            boolean result = ttSelected.cont(index);
            if (!result) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error alert");
                alert.setHeaderText(null);
                alert.setContentText("Fortsetzen des Fahrplans nicht möglich");
                alert.showAndWait();
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

    private VBox createButtonBar() {

        final HBox hb = new HBox(15);     // first line of button
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final VBox vb = new VBox(5);

        btnStart.setGraphic(ivStart);
        btnStop.setDisable(true);
        btnStop.setGraphic(ivStop);
        cbRepeat.setGraphic(ivRefresh);

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
            ttSelected.start();
            if (ttSelected.isActive() == false) {
                // reset button states if start was not successful
                btnStop.setDisable(true);
                //btnPause.setDisable(true);
                btnStart.setDisable(false);
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
            ttSelected.stop();
            tableView.getSelectionModel().clearSelection();
        }
        );

        ivHelp.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            MainUI.showManual();
            event.consume();
        });

        btnChangeTrain.setOnAction(e -> {
            LocoSpeedPairs res = ChangeTrainDialog.open(stage);
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
            SensorLocoPair res = SetTrainDialog.open(stage, ttSelected);
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
        hb.setFillHeight(true);          // Added this
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.getChildren().addAll(ivPowerState, btnStart, btnStop, cbRepeat, btnChangeTrain, btnSetTrain, spacer, ivHelp);
        vb.getChildren().addAll(hb);
        return vb;
    }

    private boolean checkTrainsPositions() {

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

    public void auto() {
        ttSelected.auto(cbRepeat.isSelected());
    }
}
