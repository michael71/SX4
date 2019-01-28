/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static de.blankedv.sx4.SX4.powerToBe;
import static de.blankedv.sx4.SX4.routingEnabled;
import static de.blankedv.sx4.SX4.running;
import static de.blankedv.sx4.SX4.sxi;
import de.blankedv.sx4.SXData;
import de.blankedv.sx4.timetable.Trip;
import static de.blankedv.sx4.timetable.Vars.allTrips;
import static de.blankedv.sx4.timetable.Vars.panelName;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 *
 * @author mblank
 */
public class TripsTable extends Application {

    private final TableView<Trip> tableView = new TableView<>();

    Scene tripTableScene;
    // New window (Stage)
    Stage tripWindow;

    private final Image green = new Image("/de/blankedv/sx4/res/greendot.png");
    private final Image red = new Image("/de/blankedv/sx4/res/reddot.png");
    private final Image refresh = new Image("/de/blankedv/sx4/res/refresh.png");
    private final ImageView ivPowerState = new ImageView();
    private final ImageView ivRefresh = new ImageView(refresh);

    @Override
    public void start(Stage primaryStage) {

        BorderPane bp = new BorderPane();

        HBox hb = createButtonBar();

        bp.setTop(hb);
        //hb.setAlignment(Pos.CENTER);
        BorderPane.setMargin(hb, new Insets(8, 8, 8, 8));
        tripTableScene = new Scene(bp, 700, 300);
        bp.setCenter(tableView);

        // New window (Stage)

        /* btnClose.setOnAction((e) -> {
            //sxAddress.addr = -1;
            tripWindow.close();
        }); */
        primaryStage.setTitle("Fahrplan " + panelName);
        primaryStage.setScene(tripTableScene);

        createDataTables();

        primaryStage.show();

        // load the image
        final Timeline second = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            if (SXData.getActualPower() == true) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }
        }));

        second.setCycleCount(Timeline.INDEFINITE);
        second.play();

    }

    public void show() {
        tripWindow.show();
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
                        final Trip tStart = row.getItem();
                        tStart.start();
                    }
                });
                final MenuItem stopMenuItem = new MenuItem("Stop diese Fahrt");
                stopMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        final Trip rtShown = row.getItem();
                        rtShown.stopLoco();
                        //rtShown.setMarked(true);
                        //Timeline timeline = new Timeline(new KeyFrame(
                        //       Duration.millis(3500),
                        //       ae -> rtShown.setMarked(false)));
                        //timeline.play();

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

    private HBox createButtonBar() {

        final HBox hb = new HBox(15);
        final Button btnStart = new Button("Start");
        final Button btnStop = new Button("Stop");
        final Button btnRefresh = new Button();
        btnRefresh.setGraphic(ivRefresh);
        btnStop.setDisable(true);

        btnStart.setOnAction(e -> {
            btnStop.setDisable(false);
            btnStart.setDisable(true);

            Trip startTrip = allTrips.get(0);
            if (startTrip != null) {
                startTrip.start();
            }

        }
        );

        ivPowerState.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // toggle global power
            if (SXData.getActualPower()) {
                powerToBe.set(false);
                ivPowerState.setImage(red);
            } else {
                powerToBe.set(true);
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
        hb.getChildren().addAll(ivPowerState, btnStart, btnStop, btnRefresh);
        return hb;
    }

}
