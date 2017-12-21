package com.mycompany.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class App extends Application {

    private MapView mapView;
    private ArcGISMap map;
    private static final double LATITUDE = 45.55;
    private static final double LONGITUDE = 25.25;
    private static final int LOD = 6;
    private ObservableList<ObservableList> data;
    private FeatureLayer layer;
    private static final String LAYER1 = "https://services6.arcgis.com/MLuUQwq7FiARivuF/arcgis/rest/services/Serviciu_UAT_Puncte/FeatureServer/0";
    private LocatorTask locatorTask;
    private TextField searchBox;


    @Override
    public void start(Stage stage) throws Exception {

        BorderPane mainpane = new BorderPane();
        StackPane left = new StackPane();
        StackPane center = new StackPane();
        TextField urlText = new TextField();
        Button addServiceBtn = new Button();
        TableView<ObservableList<String>> tabel = new TableView();
        tabel.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);


        left.setPrefWidth(700);
        center.setPrefWidth(800);
        urlText.setMaxWidth(270);
        urlText.setText(LAYER1);
        urlText.selectAll();
        addServiceBtn.setText("Add service");

        mainpane.setLeft(left);
        mainpane.setCenter(center);

        Scene scene = new Scene(mainpane);
        stage.setTitle("ArcGIS Runtime Java App");
        stage.setHeight(800);
        stage.setScene(scene);
//        stage.setMaximized(true);
//        stage.setFullScreen(true);
        stage.show();

        mapView = new MapView();

        // DropDown list with basemaps and event to change basemap when clicked
        ComboBox comboBox = new ComboBox<>();
        comboBox.getItems().addAll(FXCollections.observableArrayList(Basemap.Type.values()));

        // Change basemap event
        comboBox.getSelectionModel().selectedItemProperty().addListener(o -> {
            String basemapString = comboBox.getSelectionModel().getSelectedItem().toString().replace(" ","_");
            map = new ArcGISMap(Basemap.Type.valueOf(basemapString), LATITUDE, LONGITUDE, LOD);
            mapView.setMap(map);
            addLayer(urlText.getText());
            addDataToTable(tabel,urlText.getText());
        });

        // Set first basemap
        comboBox.getSelectionModel().selectFirst();

        // Create Search Box
        searchBox = new TextField();
        searchBox.setPromptText("Search location");
        searchBox.setEditable(true);
        searchBox.setMaxWidth(260.0);

        // Create a locatorTask
        locatorTask = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");

        // Create geocode task parameters
        GeocodeParameters geocodeParameters = new GeocodeParameters();
        // Return all attributes
        geocodeParameters.getResultAttributeNames().add("*");
        geocodeParameters.setMaxResults(1); // get closest match
        geocodeParameters.setOutputSpatialReference(mapView.getSpatialReference());

        // Event to get geocode when query is submitted
        searchBox.setOnAction((ActionEvent evt) -> {
            // Get the user's query
            String query = "";
            if (searchBox.getLength() == -1) {
                // User supplied their own query
                query = searchBox.getText();
            } else {
                // User chose a suggested query
                query = searchBox.getText();
            }

            if (!query.equals("")) {
                // Run the locatorTask geocode task
                ListenableFuture<List<GeocodeResult>> results = locatorTask.geocodeAsync(query, geocodeParameters);

                // Add a listener to display the result when loaded
                results.addDoneListener(new ResultsLoadedListener(results));
            }
        });

        // Map onClick event
        mapView.setOnMouseClicked((evt) -> {
            searchBox.setText(null);
            urlText.requestFocus();
            urlText.selectEnd();
        });

        // AddServiceBtn event
        addServiceBtn.setOnMouseClicked((event)->{
            System.out.println(urlText.getText());
            map.getOperationalLayers().remove(0);
            addLayer(urlText.getText());
            tabel.getColumns().clear();
            tabel.getItems().clear();
            addDataToTable(tabel,urlText.getText());
        });

        // Add other elements in LeftPane
        // Add the map view to stack pane
        Label urlLabel = new Label("Add service REST url to add the service in the map");
        Label basemapText = new Label("Select a basemap from the list to change the map");

        Hyperlink  author = new Hyperlink ();
        author.setText("Made by Ionut Alixandroae");
        author.setOnMouseClicked((evt) -> {
            openUrl("https://twitter.com/ialixandroae");
            author.setVisited(false);
        });
        Hyperlink  javaRuntime = new Hyperlink ();
        javaRuntime.setText("ArcGIS Runtime SDK for Java");
        javaRuntime.setOnMouseClicked((evt) -> {
            openUrl("https://developers.arcgis.com/java/latest/");
            javaRuntime.setVisited(false);
        });

        center.getChildren().addAll(mapView, searchBox);
        left.getChildren().addAll(urlLabel, urlText, addServiceBtn, basemapText, comboBox, tabel, author, javaRuntime);

        left.setAlignment(urlLabel,Pos.TOP_LEFT);
        left.setMargin(urlLabel, new Insets(10, 0, 0, 10));
        left.setAlignment(urlText,Pos.TOP_LEFT);
        left.setMargin(urlText, new Insets(40, 0, 0, 10));
        left.setAlignment(addServiceBtn,Pos.TOP_LEFT);
        left.setMargin(addServiceBtn, new Insets(90, 0, 0, 10));
        left.setAlignment(basemapText,Pos.TOP_LEFT);
        left.setMargin(basemapText, new Insets(150, 0, 0, 10));
        left.setAlignment(comboBox, Pos.TOP_LEFT);
        left.setMargin(comboBox, new Insets(180, 0, 0, 10));
        left.setAlignment(tabel,Pos.TOP_LEFT);
        left.setMargin(tabel,new Insets(220,0,0,0));
        left.setAlignment(author,Pos.TOP_RIGHT);
        left.setMargin(author,new Insets(10,10,0,0));
        left.setAlignment(javaRuntime,Pos.TOP_RIGHT);
        left.setMargin(javaRuntime,new Insets(30,10,0,0));
        center.setAlignment(searchBox, Pos.TOP_LEFT);
        center.setMargin(searchBox, new Insets(10, 0, 0, 10));


    }

    private class ResultsLoadedListener implements Runnable {

        private final ListenableFuture<List<GeocodeResult>> results;

        /**
         * Constructs a runnable listener for the geocode results.
         *
         * @param results results from a {@link LocatorTask#geocodeAsync} task
         */
        ResultsLoadedListener(ListenableFuture<List<GeocodeResult>> results) {
            this.results = results;
        }

        @Override
        public void run() {

            try {
                List<GeocodeResult> geocodes = results.get();
                if (geocodes.size() > 0) {
                    // get the top result
                    GeocodeResult geocode = geocodes.get(0);

                    // get attributes from the result for the callout
                    String addrType = geocode.getAttributes().get("Addr_type").toString();
                    String placeName = geocode.getAttributes().get("PlaceName").toString();
                    String placeAddr = geocode.getAttributes().get("Place_addr").toString();
                    String matchAddr = geocode.getAttributes().get("Match_addr").toString();
                    String locType = geocode.getAttributes().get("Type").toString();

                    // set the viewpoint to the marker
                    Point location = geocodes.get(0).getDisplayLocation();
                    mapView.setViewpointCenterAsync(location, 10000);
                }

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() throws Exception {

        if (mapView != null) {
            mapView.dispose();
        }
    }

    /**
     * Opens and runs application.
     *
     * @param args arguments passed to this application
     */
    public static void main(String[] args) {

        Application.launch(args);
    }


    /**
     * Adds the service into the application as
     * a layer in the map
     */
    private void addLayer(String serviceUrl){
        final ServiceFeatureTable featureTable = new ServiceFeatureTable(serviceUrl);
        layer = new FeatureLayer(featureTable);
        map.getOperationalLayers().add(layer);

    }

    /**
     * Parse the attributes and loads them into the TableView
     * @param tableView
     * @param serviceUrl
     */
    private void addDataToTable(TableView tableView, String serviceUrl){

        ServiceFeatureTable featureTable = new ServiceFeatureTable(serviceUrl);
        QueryParameters query = new QueryParameters();
        query.setWhereClause("1=1");
        data = FXCollections.observableArrayList();
        ListenableFuture<FeatureQueryResult> tableQueryResult = featureTable.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
        tableQueryResult.addDoneListener(() -> {

            try{

                ArrayList<String> flds = new ArrayList<>();
                FeatureQueryResult result = tableQueryResult.get();

                int numerOfFields = result.getFields().size();

                for(int i=0; i<numerOfFields; i++){
                    final int j = i;
                    String fldName = result.getFields().get(i).getName();
                    flds.add(fldName);
                    TableColumn tableColumn = new TableColumn(fldName);
                    tableColumn.setCellValueFactory(new Callback<CellDataFeatures<ObservableList,String>,ObservableValue<String>>(){
                        public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
                            return new SimpleStringProperty(param.getValue().get(j).toString());
                        }
                    });

                    tableView.getColumns().addAll(tableColumn);
//                    System.out.println("Nume camp: " + fldName.toString());
                }

                Iterator<Feature> iterator = result.iterator();
                while(iterator.hasNext()){
                    ObservableList<String> row = FXCollections.observableArrayList();
                    Feature feature = iterator.next();

                    for(int j=0; j<feature.getAttributes().size(); j++){
                        row.add(feature.getAttributes().get(flds.get(j)).toString());
                    }
//                    System.out.println(row);
                    data.add(row);
                }
//                System.out.println(data);
                tableView.setItems(data);

            }catch (Exception e){
//                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("Error loading the service!");
                alert.setContentText("Please verify the service or load another one!");
                alert.showAndWait();
            }
        });
    }

    private void openUrl(String url){
        Desktop desktop = Desktop.getDesktop();
        try{
            desktop.browse(new URI(url));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}



