/*
 * @author Guido Daniel Wolff
 * Copyright 2021, 2022 CompuPhase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package packagemanager;
/* 1 */
import javafx.application.Application;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.event.*;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
//import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
//import javafx.util.converter.DoubleStringConverter;
//import javafx.util.converter.IntegerStringConverter;
import javafx.util.Callback;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.geometry.Pos;
import java.util.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.github.cliftonlabs.json_simple.Jsoner;
import com.github.cliftonlabs.json_simple.Jsonable;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import javafx.scene.text.TextAlignment;



/* 2 */
/**
 *
 * @author Guido Daniel Wolff
 *
 * General notes on PackageManager:
 * This is my first professional software project so please excuse my sloppiness, mistakes, inelegant solutions to
 * seemingly straightforward problems, forgotten little pieces of Dutch commentary and other bad things.
 *
 * Overview of program:
 * 1: imports
 *
 * 2: author notes
 *
 * 3: global variables (mainly UI nodes to access data fields in Package)
 *
 * 4: start(): this is basically the main method instead of main().
 *    start() runs all the methods that intitialize all the UI nodes contained in the main Scene.
 *
 * 5: methods for checking/creating config file and writing/reading .json data files
 *
 * 6: methods for loading UI controls with data from currently selected Package,
 *    and methods to modify data in said Package
 *
 * 7: methods that initialize the search Scene, and methods that implement search functions
 *
 * 8: support classes, which include classes that mirror the subclasses of Package, a searchResult class
 *    to be used as data type for a TableView, and an implementation of the jsonKey interface, which the
 *    JSON.SIMPLE library requires to get data from a json object. Also some other stuff.
 *
 */

/* 3 */
public class App extends Application{
    /* core constants */
    final int STARTING_WINDOW_WIDTH  = 900; // default "scene" width (excluding borders)
    final int STARTING_WINDOW_HEIGHT = 700; // default "scene" height (excluding caption and borders)
    double prevWidth;                       // current window width (including borders)
    double prevHeight;                      // current window height (including caption and borders)

    final String programVersion = "1.0";    //current version number

    final int firstid = 0;                  //for the first entry in an array for footprints (that for now will only have that one)

    final int fieldSpacing = 8;             //spacing between items on a row
    final double intInputPrefWidth = 40;    //width for TextFields that will hold values with up to 3 digits
    final double dimInputPrefWidth = 55;    //width for TextFields that will hold numbers (dimensions etc.)
    final double tolInputPrefWidth = 45;    //width for TextFields that will hold tolerances of dimensions

    /*functional fields; do not represent data to be stored*/
    static Config config;
    StageHolder stage;                                                          // A wrapper for the main (and only) Stage
    Popup delWarning;
    AboutPopup aboutPopup;
    BasicPopup exportPopup;
    BasicPopup selectionCanceledWarning;
    BasicPopup incompleteDataWarning;
    DuplicateWarning dupWarning;                                                // A inheritor of Popup that contains a SearchResult
    static File configFile;
    static boolean configExists;
    boolean fileIsSet = false;
    File loadedFile;
    boolean viewingSelection;
    Canvas canvas;
    GraphicsContext gc;
    double scaleFactor;
    
    Scene mainScene;
    Scene searchScene;
    Scene helpScene;
    Scene testScene;

    GridPane grid;

    /*field declarations, mirrors data contained in Package */
    ArrayList <Package> viewedPackages;
    ArrayList <Package> allPackages;
    ArrayList <Package> selectedPackages;

    Package crntPkg;
    int crntIndex;

    Label displayedIndex;

    Button newPack;
    Button backToFull;

    Label nameLbl;

    dtfManager nameBox;

    ArrayList<TextField> nameFields;

    TextField descField;

    ComboBox charatypeBox;
    ComboBox terminationBox;
    CheckBox polarCheck;
    TextField pinnumber;
    ComboBox tapeOrientation;

    TextField bodyXsize;
    TextField bodyXtol;
    TextField bodyYsize;
    TextField bodyYtol;
    TextField bodyOrgX;
    TextField bodyOrgY;

    TextField ltolXsize;
    TextField ltolXtol;
    TextField ltolYsize;
    TextField ltolYtol;
    TextField ltolOrgX;
    TextField ltolOrgY;

    final ObservableList<SpepaMirror> spePacks = FXCollections.observableArrayList();

    ComboBox footprinttypeBox;
    TextField pitchField;
    TextField spanXField;
    TextField spanYField;
    TextField fpolLength;
    TextField fpolWidth;
    TextField fpolOrgX;
    TextField fpolOrgY;

    final ObservableList<SearchResult> results = FXCollections.observableArrayList();
    final ObservableList<SearchResult> selection = FXCollections.observableArrayList();
    TextField searchField;

    final ObservableList<PadDimmirror> pdDimensions = FXCollections.observableArrayList();
    TextField padIdInput;

    final ObservableList<PadPosmirror> pdPositions = FXCollections.observableArrayList();
    TextField pinIdInput;
    TextField pinPadIdInput;


    /* 4 */
    @Override
    public void start(Stage primaryStage) throws IOException{
        initConfig();   //checks for the existence of a config folder, if it doesn't exist, create it and a config file in it

        /* code that runs on exit */
        primaryStage.setOnCloseRequest((WindowEvent e) -> {
            saveAndQuit();
        });

        /* Put primaryStage in a wrapper so that it's accessible outside of start() */
        stage = new StageHolder(primaryStage);

        /* Set application icon*/
        setIcon();

        /* Load an empty Package */
        viewedPackages = new ArrayList();
        crntPkg = new Package();
        viewedPackages.add(crntPkg);
        crntIndex = 0;

        selectedPackages = new ArrayList();

        /* Load the lists with mirror classes.*/
        loadSpePacks();
        loadDimensions();
        loadPositions();


        /* Here I begin instatiating UI elements for the main Scene.
         * The gridPane will be put in a ScrollPane, so it's not the outermost container.
         * It is the main container though, and all controls or subcontainers in the main scene
         * are ordered according to the row in which they are placed in the GridPane.
         */
        grid = new GridPane();
        grid.setPadding(new Insets(0, fieldSpacing, 0, fieldSpacing));
        grid.setHgap(5.0);
        grid.setVgap(4.0);
        //grid.setStyle("-fx-grid-lines-visible: true");  // to help visualize the layout
        

        initPopups();          //initializes delWarning and dupWarning and their components, also aboutPopup

        /* First row of components controls navigation */
        final int navRow = 0;
        final HBox menuBox = initMenuBox();
        GridPane.setConstraints(menuBox, 0, navRow); //thing, column, row
        final HBox navBox = initNavBox();       // initialize subcomponents and add them as children
        GridPane.setConstraints(navBox, 1, navRow); //thing, column, row

        final HBox searchBox = new HBox(3);
        searchBox.setAlignment(Pos.CENTER_RIGHT);
        searchScene = initSearchScene();
        GridPane.setConstraints(searchBox, 2, navRow); //thing, column, row
        final Button searchButton = new Button("search");
        searchButton.setTooltip(new Tooltip("Ctrl+F"));
        searchButton.setOnAction((ActionEvent arg0) -> {
            prevWidth = stage.stage.getWidth();
            prevHeight = stage.stage.getHeight();
            primaryStage.setScene(searchScene);
            stage.stage.setHeight(prevHeight);
            stage.stage.setWidth(prevWidth);
            searchField.requestFocus();
        });
        searchBox.getChildren().add(searchButton);
        /* Adding a shortcut, CTRL+F (mnemonic on a control only works with Alt,
         * so we use an accelerator); the accelerator is created here, but added
         * to the scene later (because the scene has not been initialized yet) */
        KeyCombination searchShort = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
        Runnable searchShortCut = () -> {
            prevWidth = stage.stage.getWidth();
            prevHeight = stage.stage.getHeight();
            primaryStage.setScene(searchScene);
            stage.stage.setHeight(prevHeight);
            stage.stage.setWidth(prevWidth);
            searchField.requestFocus();
        };

        /* Space reserved for testing scene and shortcut init method call */

        /* Second row of nodes. controls Package.names through a dynamic number of TextFields */
        final int nameRow = navRow + 1;
        nameLbl = new Label("Name/alias");
        GridPane.setConstraints(nameLbl, 0, nameRow); // ((node, column, row), columnspan, rowspan)
        GridPane.setValignment(nameLbl, VPos.TOP);
        nameBox = new dtfManager(5);      //no separate init method because it's a custom class with it's own load method
        GridPane.setConstraints(nameBox, 1, nameRow);
        GridPane.setColumnSpan(nameBox, 2);
        GridPane.setHgrow(nameBox, Priority.ALWAYS);
        nameBox.load(); //sets up rows with TextFields, the number of which depends on how many are in crntPkg.names


        /* Third row of nodes manages 'Description'. One TextField, nice and easy. */
        final int descRow = nameRow + 1;
        Label descLbl = new Label("Description");
        GridPane.setConstraints(descLbl, 0, descRow);
        initDescField();    //set actionlisteners
        GridPane.setConstraints(descField, 1, descRow);
        GridPane.setHgrow(descField, Priority.ALWAYS);
        loadDescription();

        /* Display an image of the Package footprint */
        final VBox imageBox = initImage();
        GridPane.setConstraints(imageBox, 2, descRow, 1, 4);
        gc = canvas.getGraphicsContext2D();
        //loadImage(); //If I run this here it makes an image of the empty Package

        /* Fourth row of nodes manages 'characteristics'*/
        final int charaRow = descRow + 1;
        Label characLbl = new Label("Characteristics");
        characLbl.setPadding(new Insets(4, 0, 0, 0));    // to align the label with the row
        GridPane.setConstraints(characLbl, 0, charaRow);
        GridPane.setValignment(characLbl, VPos.TOP);
        final VBox charaBranch = initCharaBranch();     // initialize subcomponents and add them as children
        GridPane.setConstraints(charaBranch, 1, charaRow);
        GridPane.setHgrow(charaBranch, Priority.ALWAYS);
        loadCharacteristics();


        /* Fifth row of nodes manages body size */
        final int bdszRow = charaRow + 1;
        Label bodysizeLbl = new Label("Body size");
        GridPane.setConstraints(bodysizeLbl, 0, bdszRow);
        final HBox bodySizeBranch = initBodySizeBranch();
        GridPane.setConstraints(bodySizeBranch, 1, bdszRow);
        GridPane.setHgrow(bodySizeBranch, Priority.ALWAYS);
        loadBodySize();


        /* Sixth row of nodes, for Lead to lead */
        final int ltolRow = bdszRow + 1;
        final Label leadtoleadLbl = new Label("Lead-to-lead");
        GridPane.setConstraints(leadtoleadLbl, 0, ltolRow);
        final HBox ltolBranch = initLtolBranch();
        ltolBranch.setAlignment(Pos.CENTER_LEFT);
        GridPane.setConstraints(ltolBranch, 1, ltolRow);
        GridPane.setHgrow(ltolBranch, Priority.ALWAYS);
        loadLeadToLead();


        /* Seventh row manages specific package  */
        int spepaRow = ltolRow + 1;
        Label packageLbl = new Label("Variants");
        GridPane.setConstraints(packageLbl, 0, spepaRow);
        GridPane.setValignment(packageLbl, VPos.TOP);

        final TitledPane spepaholder = initSpepaholder();
        spepaholder.setMaxHeight(250);
        GridPane.setConstraints(spepaholder, 1, spepaRow);
        GridPane.setColumnSpan(spepaholder, 2);
        GridPane.setHgrow(spepaholder, Priority.ALWAYS);


        /* Eigth row manages footprint */
        /* TODO: add support for multple footprints. For now everything that accesses
         * footprints does so with 'firstid', which is a constant integer set at 0 */
        final int ftprintRow = spepaRow + 1;
        final Label footprintLbl = new Label("Footprint");
        footprintLbl.setPadding(new Insets(4, 0, 0, 0));    // to align the label with the row
        GridPane.setConstraints(footprintLbl, 0, ftprintRow);
        GridPane.setValignment(footprintLbl, VPos.TOP);

        final HBox footprintBranch = initFootprintBranch();
        GridPane.setConstraints(footprintBranch, 1, ftprintRow);
        GridPane.setColumnSpan(footprintBranch, 2);
        GridPane.setHgrow(footprintBranch, Priority.ALWAYS);
        loadFootPrint();


        /* Ninth row manages PadShapes/PadDimensions */
        int padshapetableRow = ftprintRow + 1;
        TitledPane padshapeholder = initPadshapeholder();
        padshapeholder.setMaxHeight(100);
        GridPane.setConstraints(padshapeholder, 1, padshapetableRow);
        GridPane.setColumnSpan(padshapeholder, 2);
        GridPane.setHgrow(padshapeholder, Priority.ALWAYS);


        /* Tenth row manages padPositions */
        int padpostablerow = padshapetableRow + 1;
        TitledPane padposholder = initPadposholder();
        padshapeholder.setMaxHeight(200);
        GridPane.setConstraints(padposholder, 1, padpostablerow);
        GridPane.setColumnSpan(padposholder, 2);
        GridPane.setHgrow(padposholder, Priority.ALWAYS);


        /* Add components to GridPane. Sorted by row */
        grid.getChildren().addAll(menuBox, navBox, searchBox,
                                  nameLbl, nameBox,
                                  descLbl, descField, imageBox,
                                  characLbl, charaBranch,
                                  bodysizeLbl, bodySizeBranch,
                                  leadtoleadLbl, ltolBranch,
                                  packageLbl, spepaholder,
                                  footprintLbl, footprintBranch,
                                  padshapeholder,
                                  padposholder);
        /* Put GridPane in a VBox and that in a Scrollpane */
        final ScrollPane realRoot = new ScrollPane();
        realRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        final VBox fakeRoot = new VBox();   //using a VBox as a subcontainter allows for control over GridPane resizing
        fakeRoot.setFillWidth(true);
        fakeRoot.prefWidthProperty().bind(realRoot.widthProperty());    //this makes fakeRoot resize to the width of realRoot
        //fakeRoot.setStyle("-fx-background-color: yellow");    //this confirms that fakeRoot indeed resizes to realRoot
        fakeRoot.getChildren().add(grid);
        realRoot.setContent(fakeRoot);

        /* Put Scrollpane in a Scene, and put the scene in the Stage */
        mainScene = new Scene(realRoot, STARTING_WINDOW_WIDTH, STARTING_WINDOW_HEIGHT); //node, width, minHeight
        primaryStage.setTitle("PACKAGES: repository for component packages & footprints");
        primaryStage.setScene(mainScene);

        /* Add any shortcuts to the scene */
        mainScene.getAccelerators().put(searchShort, searchShortCut);

        KeyCombination prvPackShort = new KeyCodeCombination(KeyCode.PAGE_UP, KeyCombination.CONTROL_DOWN);
        Runnable prvPackShortCut = () -> {
            if(crntIndex > 0){
                /* remove focus from any textfield, to avoid a "duplicate name"
                   warning (due to "focus lost" event and navigation happening
                   at the same time) */
                mainScene.getRoot().requestFocus();
                navigate(crntIndex - 1);
            }
        };
        mainScene.getAccelerators().put(prvPackShort, prvPackShortCut);

        KeyCombination nxtPackShort = new KeyCodeCombination(KeyCode.PAGE_DOWN, KeyCombination.CONTROL_DOWN);
        Runnable nxtPackShortCut = () -> {
            if(crntIndex < viewedPackages.size() - 1){
                mainScene.getRoot().requestFocus(); /* remove focus from any textfield, see comment above */
                navigate(crntIndex + 1);
            }
        };
        mainScene.getAccelerators().put(nxtPackShort, nxtPackShortCut);

        /* If the configuration was loaded then attempt to open the last used json file. */
        if(Config.pathSet){
            try {
                File file = new File(Config.path);
                if(load(file)){
                    loadedFile = file;
                    fileIsSet = true;
                    stage.stage.setWidth(Config.width);
                    stage.stage.setHeight(Config.height);
                }
                else{
                    System.out.println("Failed to load" + Config.path);
                }
            } catch (IOException | JsonException ex) {
                System.out.println(ex);
            }
        }
        loadImage();    //in case auto-load doesn't work
        primaryStage.show();

        /* resize grid to window width; note that the width & height can only be
         * queried after initial display of the stage
         */
        prevWidth = stage.stage.getWidth();
        prevHeight = stage.stage.getHeight();
        /* scale main scene to window width */
        //double sceneWidth = mainScene.getWidth();
        //grid.setPrefWidth(sceneWidth+50);
    }
    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! End of start() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */


    /* Set the application icon */
    private void setIcon(){
        String path = getResourcePath();
        path = path + "/Packages64.png";
        File file = new File(path);
        if(file.exists()){
            Image icon;
            try{
                icon = new Image(file.toURI().toString());
                stage.stage.getIcons().add(icon);
            } catch(Exception e){
                //e.printStackTrace();
                System.out.println("Image failed to load!");
            }
        }
    }

    /* A support method to help create a dynamic layout */
    private Node createHSpacer(){
        final Region spacer = new Region();
        // Make it always grow or shrink according to the available space
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    
    /* A method that verifies correct input for those that require a number */
    private boolean verifyInput(TextField tf){
        return verifyInput(tf, true, true);
    }
    private boolean verifyInput(TextField tf, boolean permitNegative){
        return verifyInput(tf, permitNegative, true);
    }
    private boolean verifyInput(TextField tf, boolean permitNegative, boolean permitDecimal){
        String input = tf.getText();
        String output = "";
        boolean oneDot = false;
        for (int i = 0; i < input.length(); i++){
            char c = input.charAt(i);        
            if(Character.isDigit(c)){
                output += c;
            } else if((c == '.' || c == ',') && permitDecimal){
                if(!oneDot){
                    output += '.';
                    oneDot = true;
                }  //else just ignore it 
            } else if(c == '-' && i == 0 && permitNegative){
                output += '-';
            } else{
                tf.setStyle("-fx-control-inner-background: #fff0a0;");
                return false;
            }
        }
        tf.setText(output);
        tf.setStyle("-fx-control-inner-background: white;");
        return true;
    }


    /* Methods that intitialize UI in main scene */
    private HBox initMenuBox(){
        final HBox menuBox = new HBox(3);

        final MenuBar menubar = new MenuBar();
        final Menu fileMenu = new Menu("File");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
        final MenuItem loadItem = new MenuItem("Open...");
        MenuItem saveItem = new MenuItem("Save"); //initialized here instead of below for visibility
        if(Config.pathSet){
            loadedFile = new File(Config.path);
        } else{
            saveItem.setDisable(true);
        }

        final FileChooser loadChooser = new FileChooser();
        loadChooser.getExtensionFilters().add(extFilter);
        loadItem.setOnAction((ActionEvent t) -> {
            File selectedFile = loadChooser.showOpenDialog(stage.stage);
            System.out.println("Attempting to load");
            try {
                if(load(selectedFile)){
                    System.out.println("Successfully loaded");
                    loadedFile = selectedFile;
                    fileIsSet = true;
                    saveItem.setDisable(false);
                    Config.setPath(loadedFile.getPath());
                    saveConfig(config);
                }
                else{
                    System.out.println("failed to load");
                }
            } catch (IOException | JsonException ex) {
                System.out.println(ex);
            }
        });
        final MenuItem saveAsItem = new MenuItem("Save as...");
        final FileChooser saveChooser = new FileChooser();
        saveChooser.getExtensionFilters().add(extFilter);
        saveAsItem.setOnAction((ActionEvent t) -> {
            File selectedFile = saveChooser.showSaveDialog(stage.stage);
            System.out.println("Attempting to save");
            try {
                if(save(selectedFile)){
                    System.out.println("Successfully saved");
                    loadedFile = selectedFile;
                    fileIsSet = true;
                    saveItem.setDisable(false);
                    Config.setPath(loadedFile.getPath());
                    saveConfig(config);
                }
                else{
                     System.out.println("failed to save");
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        });
        final MenuItem newItem = new MenuItem("New...");
        newItem.setOnAction((ActionEvent t) -> {
            File selectedFile = saveChooser.showSaveDialog(stage.stage);
            System.out.println("Attempting to create new file");
            ArrayList<Package> newList = new ArrayList();
            newList.add(new Package());
            try {
                if(save(selectedFile, newList)){
                    System.out.println("Successfully saved new file");
                    loadedFile = selectedFile;
                    fileIsSet = true;
                    saveItem.setDisable(false);
                    Config.setPath(loadedFile.getPath());
                    saveConfig(config);
                    allPackages = newList;
                    setPackageSelection(false, 0);
                }
                else{
                     System.out.println("failed to save new file");
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        });
        //final MenuItem saveItem = new MenuItem("Save"); declared/initialized above other stuff for visibility
        saveItem.setOnAction((ActionEvent t) -> {
            System.out.println("Attempting to save");
            try {
                if(save(loadedFile)){
                    System.out.println("Successfully saved");
                }
                else{
                     System.out.println("failed to save");
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        });
        final MenuItem helpItem = new MenuItem("Help");
        helpScene = initHelpScene(stage.stage);
        helpItem.setOnAction((ActionEvent t) -> {
            prevWidth = stage.stage.getWidth();
            prevHeight = stage.stage.getHeight();
            stage.stage.setScene(helpScene);
            stage.stage.setHeight(prevHeight);
            stage.stage.setWidth(prevWidth);
        });
        final MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction((ActionEvent t) -> {
            aboutPopup.Update();    /* update current package/variant counts */
            aboutPopup.show(stage.stage);
        });
        final MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction((ActionEvent t) -> {
            saveAndQuit();
        });
        fileMenu.getItems().addAll(newItem, loadItem, saveItem, saveAsItem,
                                   new SeparatorMenuItem(), helpItem, aboutItem,
                                   new SeparatorMenuItem(), exitItem);
        menubar.getMenus().add(fileMenu);

        menuBox.getChildren().addAll(menubar);
        return menuBox;
    }

    private HBox initNavBox(){
        final HBox navButtons = new HBox(3); // spacing = 3

        navButtons.setAlignment(Pos.CENTER_LEFT);
        final Button prvPack = new Button("<<");
        prvPack.setTooltip(new Tooltip("Ctrl+PageUp"));
        prvPack.setOnAction((ActionEvent arg0) -> {
            if(crntIndex > 0){
                navigate(crntIndex - 1);
            }
        });

        displayedIndex = new Label(Integer.toString(crntIndex + 1) + " of " + Integer.toString(viewedPackages.size()));

        final Button nxtPack = new Button(">>");
        nxtPack.setTooltip(new Tooltip("Ctrl+PageDown"));
        nxtPack.setOnAction((ActionEvent arg0) -> {
            if(crntIndex < viewedPackages.size() - 1){
                navigate(crntIndex + 1);
            }
        });

        newPack = new Button("new");
        newPack.setOnAction((ActionEvent arg0) -> {
            viewedPackages.add(new Package());
            navigate(viewedPackages.size() - 1);
        });

        final Button delPack = new Button("delete");
        delPack.setOnAction((ActionEvent arg0) -> {
            delWarning.show(stage.stage);
        });

        backToFull = new Button("Back to full list");
        backToFull.setOnAction((ActionEvent arg0) -> {
            setPackageSelection(false, allPackages.indexOf(crntPkg));
        });
        backToFull.setVisible(false);   /* invisible by default */

        navButtons.getChildren().addAll(prvPack, displayedIndex, nxtPack, newPack, delPack, backToFull);
        return navButtons;
    }

    private void initDescField(){
        descField = new TextField(crntPkg.description);
        descField.setPromptText("short description or keywords");
        descField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                crntPkg.description = descField.getText();
            }
        });
        descField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                crntPkg.description = descField.getText();
            }
        });
    }

    private VBox initImage(){
        final VBox imageBranch = new VBox();

        final Separator topSep = new Separator(Orientation.HORIZONTAL);

        final HBox midBox = new HBox();
        final Separator leftSep = new Separator(Orientation.VERTICAL);
        final Separator rightSep = new Separator(Orientation.VERTICAL);
        canvas = new Canvas(200, 144);

        midBox.getChildren().addAll(leftSep, canvas, rightSep);

        final Separator botSep = new Separator(Orientation.HORIZONTAL);

        imageBranch.getChildren().addAll(topSep, midBox, botSep);
        return imageBranch;
    }

    private VBox initCharaBranch(){
        VBox charaBranch = new VBox(5); //holds two HBoxes to from two rows of components

        final HBox topChaRow = new HBox(3);     //contains first row of the characteristics

        HBox typeBoxBox = new HBox(3);
        typeBoxBox.setAlignment(Pos.CENTER_LEFT);
        final Label chaTypeLabel = new Label("type");
        charatypeBox = new ComboBox();
        charatypeBox.getItems().addAll(Package.charTypeValues());
        charatypeBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.type = Package.charTypefromString((String) charatypeBox.getValue());
        });
        typeBoxBox.getChildren().addAll(chaTypeLabel, charatypeBox);

        HBox pinsBox = new HBox(3);
        pinsBox.setAlignment(Pos.CENTER);
        pinnumber = new TextField();
        pinnumber.setMaxWidth(intInputPrefWidth);
        pinnumber.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(pinnumber, false, false)){
                    crntPkg.nrOfPins = Integer.parseInt(pinnumber.getText());
                }
            }
        });
        pinnumber.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pinnumber, false, false)){
                    crntPkg.nrOfPins = Integer.parseInt(pinnumber.getText());
                }
            }
        });
        final Label pincountLabel = new Label("pin count");
        pincountLabel.setPadding(new Insets(0, 0, 0, fieldSpacing));
        pinsBox.getChildren().addAll(pincountLabel, pinnumber);

        final HBox pitchBox = new HBox(3);
        pitchBox.setAlignment(Pos.CENTER);
        pitchField = new TextField();
        pitchField.setMaxWidth(dimInputPrefWidth);
        pitchField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(pitchField, false)){
                    crntPkg.pitch = Double.parseDouble(pitchField.getText());
                }
            }
        });
        pitchField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pitchField, false)){
                    crntPkg.pitch = Double.parseDouble(pitchField.getText());
                }
            }
        });
        final Label pitchLabel = new Label("pitch");
        pitchLabel.setPadding(new Insets(0, 0, 0, fieldSpacing));
        pitchBox.getChildren().addAll(pitchLabel, pitchField);

        polarCheck = new CheckBox("polarized");
        polarCheck.setIndeterminate(false);
        polarCheck.setPadding(new Insets(0, 0, 0, fieldSpacing));
        polarCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov,Boolean old_val, Boolean new_val) {
                crntPkg.polarized = polarCheck.isSelected();
            }
        });

        topChaRow.getChildren().addAll(typeBoxBox, pinsBox, pitchBox, polarCheck);

        final HBox botChaRow = new HBox(3);         //contains second row of the characteristics
        botChaRow.setAlignment(Pos.CENTER_LEFT);

        HBox termBoxBox = new HBox(3);
        termBoxBox.setAlignment(Pos.CENTER);
        terminationBox = new ComboBox();
        terminationBox.getItems().addAll(Package.termTypeValues());
        terminationBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.termination = Package.termTypefromString((String) terminationBox.getValue());
            checkLeadToLead();
        });
        Label termLabel = new Label("terminals");
        termBoxBox.getChildren().addAll(termLabel, terminationBox);

        HBox tapeOrientBox = new HBox(3);
        tapeOrientBox.setAlignment(Pos.CENTER);
        tapeOrientation = new ComboBox();
        tapeOrientation.getItems().addAll(Package.orientationValues());
        tapeOrientation.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.tapeOrient = Package.orientationFromInt(Integer.parseInt((String)tapeOrientation.getValue()));
        });
        Label tapeOrientationLabel = new Label("tape packaging orientation");
        tapeOrientationLabel.setPadding(new Insets(0, 0, 0, fieldSpacing));
        tapeOrientBox.getChildren().addAll(tapeOrientationLabel, tapeOrientation);

        botChaRow.getChildren().addAll(termBoxBox, tapeOrientBox);
        charaBranch.getChildren().addAll(topChaRow, botChaRow);

        return charaBranch;
    }

    private HBox initBodySizeBranch(){
        HBox bodySizeBranch = new HBox(3);
        bodySizeBranch.setAlignment(Pos.CENTER_LEFT);
        final HBox bodySizeBox = new HBox(3);
        bodySizeBox.setAlignment(Pos.CENTER_LEFT);

        bodyXsize = new TextField();
        bodyXsize.setMaxWidth(dimInputPrefWidth);
        bodyXsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyXsize, false)){
                    crntPkg.body.bodyX = Double.parseDouble(bodyXsize.getText());
                    checkBodySize();
                    loadImage();
                }
            }
        });
        bodyXsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyXsize, false)){
                    crntPkg.body.bodyX = Double.parseDouble(bodyXsize.getText());
                    checkBodySize();
                    loadImage();
                }
            }
        });
        bodyXtol = new TextField();
        bodyXtol.setMaxWidth(tolInputPrefWidth);
        bodyXtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyXtol, false)){
                    crntPkg.body.bodyXtol = Double.parseDouble(bodyXtol.getText());
                }
            }
        });
        bodyXtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyXtol, false)){
                    crntPkg.body.bodyXtol = Double.parseDouble(bodyXtol.getText());
                }
            }
        });
        bodyYsize = new TextField();
        bodyYsize.setMaxWidth(dimInputPrefWidth);
        bodyYsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyYsize, false)){
                    crntPkg.body.bodyY = Double.parseDouble(bodyYsize.getText());
                    checkBodySize();
                    loadImage();
                }
            }
        });
        bodyYsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyYsize, false)){
                    crntPkg.body.bodyY = Double.parseDouble(bodyYsize.getText());
                    checkBodySize();
                    loadImage();
                }
            }
        });
        bodyYtol = new TextField();
        bodyYtol.setMaxWidth(tolInputPrefWidth);
        bodyYtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyYtol, false)){
                    crntPkg.body.bodyYtol = Double.parseDouble(bodyYtol.getText());
                }
            }
        });
        bodyYtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyYtol, false)){
                    crntPkg.body.bodyYtol = Double.parseDouble(bodyYtol.getText());
                }
            }
        });

        bodySizeBox.getChildren().addAll(bodyXsize, new Label("±"), bodyXtol, new Label("x"), bodyYsize, new Label("±"), bodyYtol, new Label("mm"));

        final HBox bodySizeOrgBox = new HBox(3);
        bodySizeOrgBox.setAlignment(Pos.CENTER_LEFT);

        bodyOrgX = new TextField();
        bodyOrgX.setMaxWidth(dimInputPrefWidth);
        bodyOrgX.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyOrgX)){
                    crntPkg.body.bodyOrgX = Double.parseDouble(bodyOrgX.getText());
                }
            }
        });
        bodyOrgX.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyOrgX)){
                    crntPkg.body.bodyOrgX = Double.parseDouble(bodyOrgX.getText());
                }
            }
        });

        bodyOrgY = new TextField();
        bodyOrgY.setMaxWidth(dimInputPrefWidth);
        bodyOrgY.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyOrgY)){
                    crntPkg.body.bodyOrgY = Double.parseDouble(bodyOrgY.getText());
                }
            }
        });
        bodyOrgY.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyOrgY)){
                    crntPkg.body.bodyOrgY = Double.parseDouble(bodyOrgY.getText());
                }
            }
        });

        final Label bodyOrgLbl = new Label("origin X");
        bodyOrgLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        bodySizeOrgBox.getChildren().addAll(bodyOrgLbl, bodyOrgX, new Label("Y"), bodyOrgY);

        bodySizeBranch.getChildren().addAll(bodySizeBox, createHSpacer(), bodySizeOrgBox, createHSpacer());
        return bodySizeBranch;
    }

    private HBox initLtolBranch(){
        HBox ltolBranch = new HBox(3);
        final HBox ltolBox = new HBox(3);
        ltolBox.setAlignment(Pos.CENTER_LEFT);

        ltolXsize = new TextField();
        ltolXsize.setMaxWidth(dimInputPrefWidth);
        ltolXsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolXsize, false)){
                    crntPkg.lead2lead.x = Double.parseDouble(ltolXsize.getText());
                    checkLeadToLead();
                    loadImage();
                }
            }
        });
        ltolXsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolXsize, false)){
                    crntPkg.lead2lead.x = Double.parseDouble(ltolXsize.getText());
                    checkLeadToLead();
                    loadImage();
                }
            }
        });
        ltolXtol = new TextField();
        ltolXtol.setMaxWidth(tolInputPrefWidth);
        ltolXtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolXtol, false)){
                    crntPkg.lead2lead.xTol = Double.parseDouble(ltolXtol.getText());
                }
            }
        });
        ltolXtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolXtol, false)){
                    crntPkg.lead2lead.xTol = Double.parseDouble(ltolXtol.getText());
                }
            }
        });

        ltolYsize = new TextField();
        ltolYsize.setMaxWidth(dimInputPrefWidth);
        ltolYsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolYsize, false)){
                    crntPkg.lead2lead.y = Double.parseDouble(ltolYsize.getText());
                    checkLeadToLead();
                    loadImage();
                }
            }
        });
        ltolYsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolYsize, false)){
                    crntPkg.lead2lead.y = Double.parseDouble(ltolYsize.getText());
                    checkLeadToLead();
                    loadImage();
                }
            }
        });

        ltolYtol = new TextField();
        ltolYtol.setMaxWidth(tolInputPrefWidth);
        ltolYtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolYtol, false)){
                    crntPkg.lead2lead.yTol = Double.parseDouble(ltolYtol.getText());
                }
            }
        });
        ltolYtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolYtol, false)){
                    crntPkg.lead2lead.yTol = Double.parseDouble(ltolYtol.getText());
                }
            }
        });

        ltolBox.getChildren().addAll(ltolXsize, new Label("±"), ltolXtol, new Label("x"), ltolYsize, new Label("±"), ltolYtol, new Label("mm"));

        final HBox ltolOrgBox = new HBox(3);
        ltolOrgBox.setAlignment(Pos.CENTER_LEFT);

        ltolOrgX = new TextField();
        ltolOrgX.setMaxWidth(dimInputPrefWidth);
        ltolOrgX.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolOrgX)){
                    crntPkg.lead2lead.orgX = Double.parseDouble(ltolOrgX.getText());
                }
            }
        });
        ltolOrgX.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolOrgX)){
                    crntPkg.lead2lead.orgX = Double.parseDouble(ltolOrgX.getText());
                }
            }
        });
        ltolOrgY = new TextField();
        ltolOrgY.setMaxWidth(dimInputPrefWidth);
        ltolOrgY.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolOrgY)){
                    crntPkg.lead2lead.orgY = Double.parseDouble(ltolOrgY.getText());
                }
            }
        });
        ltolOrgY.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolOrgY)){
                    crntPkg.lead2lead.orgY = Double.parseDouble(ltolOrgY.getText());
                }
            }
        });

        final Label ltolOrgLbl = new Label("origin X");
        ltolOrgLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        ltolOrgBox.getChildren().addAll(ltolOrgLbl, ltolOrgX, new Label("Y"), ltolOrgY);
        ltolBranch.getChildren().addAll(ltolBox, createHSpacer(), ltolOrgBox, createHSpacer());
        return ltolBranch;
    }

    private TitledPane initSpepaholder(){
        TitledPane spepaholder = new TitledPane();
        spepaholder.setText("Specific package variants");
        VBox spepaBranch = new VBox(5);

        /*The table */
        TableView<SpepaMirror> spepaTable = new TableView<>();
        spepaTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        spepaTable.setEditable(true);
        spepaTable.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn, TableCell> cellFactory = (TableColumn p) -> new CustomCell();
        Callback<TableColumn, TableCell> cellDoubleFactory = (TableColumn p) -> new EditingDoubleCell();
        //Callback<TableColumn, TableCell> cellCheckFactory = (TableColumn p) -> new CustomCheckBoxCell();


        TableColumn ipcNameCol = new TableColumn("name");
        ipcNameCol.setMinWidth(100);
        ipcNameCol.setCellValueFactory(new PropertyValueFactory<>("spepaNameppt"));
        ipcNameCol.setCellFactory(cellFactory);
        ipcNameCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, String>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, String> t) {
                    ((SpepaMirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setIpcName(t.getNewValue());
                    updateVariants();
                }
            }
        );


        TableColumn minHeightCol = new TableColumn("min. height");
        minHeightCol.setMinWidth(60);
        minHeightCol.setCellValueFactory(new PropertyValueFactory<>("minHeightppt"));
        minHeightCol.setCellFactory(cellDoubleFactory);
        minHeightCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, Double> t) {
                    ((SpepaMirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setMinHeight(t.getNewValue());
                    updateVariants();
                }
            }
        );

        TableColumn maxHeightCol = new TableColumn("max. height");
        maxHeightCol.setMinWidth(100);
        maxHeightCol.setCellValueFactory(new PropertyValueFactory<>("maxHeightppt"));
        maxHeightCol.setCellFactory(cellDoubleFactory);
        maxHeightCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, Double> t) {
                    ((SpepaMirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setMaxHeight(t.getNewValue());
                    updateVariants();
                }
            }
        );

        TableColumn standardCol = new TableColumn("standard");
        standardCol.setMinWidth(100);
        standardCol.setCellValueFactory(new PropertyValueFactory<>("standardppt"));
        standardCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.nameStandardValues()));
        standardCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, String>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, String> t) {
                    ((SpepaMirror) t.getTableView().getItems().get(t.getTablePosition().getRow())).setStandard(t.getNewValue());
                    updateVariants();
                }
            }
        );

        TableColumn xposedCol = new TableColumn("exposed pad");
        xposedCol.setMinWidth(80);
        xposedCol.setCellValueFactory(new PropertyValueFactory<>("padExposedppt"));
        xposedCol.setCellFactory(CustomCheckBoxCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(Integer param) {
                spePacks.get(param).padExposed = spePacks.get(param).padExposedppt.get();
                updateVariants();
                return spePacks.get(param).padExposedppt;
            }
        }));


        TableColumn notesCol = new TableColumn("notes");
        notesCol.setMinWidth(120);
        notesCol.setCellValueFactory(new PropertyValueFactory<>("spepaNotesppt"));
        notesCol.setCellFactory(cellFactory);
        notesCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, String>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, String> t) {
                    ((SpepaMirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setNotes(t.getNewValue());
                    updateVariants();
                }
            }
        );

        spepaTable.setItems(spePacks);
        spepaTable.getColumns().addAll(ipcNameCol, minHeightCol, maxHeightCol, standardCol, xposedCol, notesCol);

        HBox spepaAdditionBox = new HBox(3);
        spepaAdditionBox.setAlignment(Pos.CENTER_LEFT);

        final TextField ipcNameInput = new TextField();
        ipcNameInput.setPromptText("name");

        final TextField minHeightInput = new TextField();
        minHeightInput.setMaxWidth(dimInputPrefWidth);
        minHeightInput.setPromptText("H min");
        minHeightInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(minHeightInput, false)){
                    //TODO: maybe disable 'add' button or something?
                }
            }
        });

        final TextField maxHeightInput = new TextField();
        maxHeightInput.setMaxWidth(dimInputPrefWidth);
        maxHeightInput.setPromptText("H max");
        maxHeightInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(maxHeightInput, false)){
                    //TODO: maybe disable 'add' button or something?
                }
            }
        });

        final ComboBox nameStandardInput =  new ComboBox();
        nameStandardInput.getItems().addAll(Package.nameStandardValues());

        final CheckBox exposedInput = new CheckBox("exposed center pad");
        exposedInput.setPadding(new Insets(0, fieldSpacing, 0, fieldSpacing));

        final TextField spepaNotesInput = new TextField();
        spepaNotesInput.setPromptText("notes");

        final Button addSpepaButton = new Button("Add");
        addSpepaButton.setOnAction((ActionEvent e) -> {
            if(ipcNameInput.getText().length() == 0){
                incompleteDataWarning.show(stage.stage);
            } else if(notDuplicate(ipcNameInput.getText())){
                spePacks.add(new SpepaMirror( //args: String ipcName, double minHeight, double maxHeight, boolean padExposed, String alias
                    ipcNameInput.getText(),
                    Package.nameStandardFromString((String)nameStandardInput.getValue()),
                    (minHeightInput.getText().length() > 0) ? Double.parseDouble(minHeightInput.getText()) : 0,
                    (maxHeightInput.getText().length() > 0) ? Double.parseDouble(maxHeightInput.getText()) : 0,
                    exposedInput.isSelected(),
                    spepaNotesInput.getText()
                ));
            } else{
                dupWarning.show(stage.stage);
            }

            ipcNameInput.clear();
            nameStandardInput.valueProperty().set(null);
            minHeightInput.clear();
            maxHeightInput.clear();
            exposedInput.setSelected(false);
            spepaNotesInput.clear();
            spepaTable.scrollTo(spepaTable.getItems().size() - 1);

            updateVariants(); //vernieuwt Package.specPacks
        });

        spepaAdditionBox.getChildren().addAll(ipcNameInput, minHeightInput, maxHeightInput, nameStandardInput, exposedInput, spepaNotesInput, addSpepaButton);

        HBox spepaDeletionBox = new HBox(3);
        final Button spepaDeleteButton = new Button("Delete selected");
        spepaDeleteButton.setOnAction(e -> {
            SpepaMirror selectedItem = spepaTable.getSelectionModel().getSelectedItem();
            spepaTable.getItems().remove(selectedItem);
            updateVariants();
        });
        spepaDeletionBox.getChildren().add(spepaDeleteButton);
        spepaBranch.getChildren().addAll(spepaTable, spepaAdditionBox ,spepaDeletionBox );
        spepaholder.setContent(spepaBranch);

        return spepaholder;
    }

    private HBox initFootprintBranch(){
        HBox footprintBranch = new HBox(3);

        footprinttypeBox = new ComboBox();
        footprinttypeBox.getItems().addAll(Package.footprintTypeValues());
        footprinttypeBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.footPrints[0].ftprntType = Package.footprintTypefromString((String)footprinttypeBox.getValue());
        });

        HBox spanBox = new HBox(3);
        spanBox.setAlignment(Pos.CENTER_LEFT);
        spanXField = new TextField();
        spanXField.setMaxWidth(dimInputPrefWidth);
        spanXField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(spanXField, false)){
                    crntPkg.footPrints[firstid].span.x = Double.parseDouble(spanXField.getText());
                checkSpan();
                }
            }
        });
        spanXField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(spanXField, false)){
                    crntPkg.footPrints[firstid].span.x = Double.parseDouble(spanXField.getText());
                checkSpan();
                }
            }
        });

        spanYField = new TextField();
        spanYField.setMaxWidth(dimInputPrefWidth);
        spanYField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(spanYField, false)){
                    crntPkg.footPrints[firstid].span.y = Double.parseDouble(spanYField.getText());
                    checkSpan();
                }
            }
        });
        spanYField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(spanYField, false)){
                    crntPkg.footPrints[firstid].span.y = Double.parseDouble(spanYField.getText());
                    checkSpan();
                }
            }
        });

        final Label spanLbl = new Label("span");
        spanLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        spanBox.getChildren().addAll(spanLbl, spanXField, new Label("x"), spanYField, new Label("mm"));

        final HBox fpoutlineBox = new HBox(3);
        fpoutlineBox.setAlignment(Pos.CENTER_LEFT);
        fpolLength = new TextField();   //TODO: possibly swap length and width. Currently length is X and width is Y here, but it's different elsewhere
        fpolLength.setMaxWidth(dimInputPrefWidth);
        fpolLength.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolLength, false)){
                    crntPkg.footPrints[firstid].outline.length = Double.parseDouble(fpolLength.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolLength.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolLength, false)){
                    crntPkg.footPrints[firstid].outline.length = Double.parseDouble(fpolLength.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolWidth = new TextField();
        fpolWidth.setMaxWidth(dimInputPrefWidth);
        fpolWidth.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolWidth, false)){
                    crntPkg.footPrints[firstid].outline.width = Double.parseDouble(fpolWidth.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolWidth.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolWidth, false)){
                    crntPkg.footPrints[firstid].outline.width = Double.parseDouble(fpolWidth.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolOrgX = new TextField();
        fpolOrgX.setMaxWidth(dimInputPrefWidth);
        fpolOrgX.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolOrgX)){
                    crntPkg.footPrints[firstid].outline.orgX = Double.parseDouble(fpolOrgX.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolOrgX.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolOrgX)){
                    crntPkg.footPrints[firstid].outline.orgX = Double.parseDouble(fpolOrgX.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolOrgY = new TextField();
        fpolOrgY.setMaxWidth(dimInputPrefWidth);
        fpolOrgY.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolOrgY)){
                    crntPkg.footPrints[firstid].outline.orgY = Double.parseDouble(fpolOrgY.getText());
                    checkContour();
                    loadImage();
                }
            }
        });
        fpolOrgY.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolOrgY)){
                    crntPkg.footPrints[firstid].outline.orgY = Double.parseDouble(fpolOrgY.getText());
                    checkContour();
                    loadImage();
                }
            }
        });

        final Label fpoutlineLbl = new Label("contour X");
        fpoutlineLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        final Label fporiginLbl = new Label("origin X");
        fporiginLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        fpoutlineBox.getChildren().addAll(fpoutlineLbl, fpolLength, new Label("Y"), fpolWidth, fporiginLbl, fpolOrgX, new Label("Y"), fpolOrgY);
        footprintBranch.getChildren().addAll(footprinttypeBox, spanBox, createHSpacer(), fpoutlineBox, createHSpacer());

        return footprintBranch;
    }

    private TitledPane initPadshapeholder(){
        TitledPane padshapeholder = new TitledPane();
        padshapeholder.setText("Pad shapes");
        padshapeholder.setExpanded(false);
        VBox shaBranch = new VBox(5);//parameter is spacing

        /* the table */
        TableView<PadDimmirror> padshapeTable = new TableView<>();
        padshapeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        padshapeTable.setEditable(true);
        padshapeTable.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn, TableCell> cellIntFactory = (TableColumn p) -> new EditingIntCell();
        Callback<TableColumn, TableCell> cellDoubleFactory = (TableColumn p) -> new EditingDoubleCell();

        TableColumn nrCol = new TableColumn("pad-id");
        nrCol.setMinWidth(60);
        nrCol.setCellValueFactory(new PropertyValueFactory<>("padIdppt"));
        nrCol.setCellFactory(cellIntFactory);
        nrCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, Integer>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, Integer> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setPadId(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn widCol = new TableColumn("cx");
        widCol.setMinWidth(50);
        widCol.setCellValueFactory(new PropertyValueFactory<>("widthppt"));
        widCol.setCellFactory(cellDoubleFactory);
        widCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, Double> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setWidth(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn lenCol = new TableColumn("cy");
        lenCol.setMinWidth(50);
        lenCol.setCellValueFactory(new PropertyValueFactory<>("lengthppt"));
        lenCol.setCellFactory(cellDoubleFactory);
        lenCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, Double> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setLength(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn holeCol = new TableColumn("Hole");
        holeCol.setMinWidth(70);
        holeCol.setCellValueFactory(new PropertyValueFactory<>("holeDiamppt"));
        holeCol.setCellFactory(cellDoubleFactory);
        holeCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, Double> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setHoleDiam(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn ognxCol = new TableColumn("origin X");
        ognxCol.setMinWidth(70);
        ognxCol.setCellValueFactory(new PropertyValueFactory<>("originXppt"));
        ognxCol.setCellFactory(cellDoubleFactory);
        ognxCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, Double> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setOriginX(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn ognyCol = new TableColumn("origin Y");
        ognyCol.setMinWidth(70);
        ognyCol.setCellValueFactory(new PropertyValueFactory<>("originYppt"));
        ognyCol.setCellFactory(cellDoubleFactory);
        ognyCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, Double> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setOriginY(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn shapeCol = new TableColumn("Shape");
        shapeCol.setMinWidth(100);
        shapeCol.setCellValueFactory(new PropertyValueFactory<>("shapeppt"));
        shapeCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.padShapeValues()));
        shapeCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimmirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadDimmirror, String> t) {
                    ((PadDimmirror) t.getTableView().getItems().get(t.getTablePosition().getRow())).setShape(t.getNewValue());
                    updateDimensions();
                }
            }
        );

        TableColumn xposedCol = new TableColumn("exposed pad");
        xposedCol.setMinWidth(80);
        xposedCol.setCellValueFactory(new PropertyValueFactory<>("padExposedppt"));
        xposedCol.setCellFactory(CheckBoxTableCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(Integer param) {
                pdDimensions.get(param).padExposed = pdDimensions.get(param).padExposedppt.get();
                updateDimensions();
                return pdDimensions.get(param).padExposedppt;
            }
        }));

        padshapeTable.setItems(pdDimensions);
        padshapeTable.getColumns().addAll(nrCol, widCol, lenCol, holeCol, ognxCol, ognyCol, shapeCol, xposedCol);

        /* shapeAdditionBox contains controls for adding items the list, and therefore the padShape table */
        HBox shapeAdditionBox = new HBox(3);
        shapeAdditionBox.setAlignment(Pos.CENTER_LEFT);

        padIdInput = new TextField(Integer.toString(highestPadId() + 1));
        padIdInput.setMaxWidth(dimInputPrefWidth);
        padIdInput.setPromptText("pad-id");
        padIdInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(padIdInput, false, false)){
                    //TODO: maybe disable 'add' button or something?
                }
            }
        });

        final TextField lengthInput = new TextField();
        lengthInput.setMaxWidth(dimInputPrefWidth);
        lengthInput.setPromptText("cx");
        lengthInput.setTooltip(new Tooltip("Horizontal dimension of the pad (mandatory)."));
        lengthInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(lengthInput, false)){
                    //see above
                }
            }
        });

        final TextField widthInput = new TextField();
        widthInput.setMaxWidth(dimInputPrefWidth);
        widthInput.setPromptText("cy");
        widthInput.setTooltip(new Tooltip("Vertical dimension of the pad (mandatory)."));
        widthInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(widthInput, false)){
                    //see above
                }
            }
        });

        final TextField holeInput = new TextField();
        holeInput.setMaxWidth(100); //Deze is dik
        holeInput.setPromptText("hole diameter");
        holeInput.setTooltip(new Tooltip("Hole size for through-hole pads.\nLeave empty (or set to zero) for SMD pads."));
        holeInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(holeInput, false)){
                    //see above
                }
            }
        });

        final TextField oriXinput = new TextField();
        oriXinput.setMaxWidth(dimInputPrefWidth);
        oriXinput.setPromptText("origin-x");
        oriXinput.setTooltip(new Tooltip("Offset of the pad's origin from the geometric centre (optional)."));
        oriXinput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(oriXinput)){
                    //see above
                }
            }
        });

        final TextField oriYinput = new TextField();
        oriYinput.setMaxWidth(dimInputPrefWidth);
        oriYinput.setPromptText("origin-y");
        oriYinput.setTooltip(new Tooltip("Offset of the pad's origin from the geometric centre (optional)."));
        oriYinput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(oriYinput)){
                    //see above
                }
            }
        });

        final ComboBox shapeBox = new ComboBox();
        shapeBox.getItems().addAll(Package.padShapeValues());
        shapeBox.setTooltip(new Tooltip("Pad shape (mandatory)."));

        final CheckBox exposedInput = new CheckBox("exposed center pad");
        exposedInput.setPadding(new Insets(0, fieldSpacing, 0, fieldSpacing));
        exposedInput.setTooltip(new Tooltip("Tick this field if the pad definition is for a thermal pad."));

        final Button addShapeButton = new Button("Add");
        addShapeButton.setOnAction((ActionEvent e) -> {
            /* check mandatory fields */
            if(padIdInput.getText().length() == 0 ||
               lengthInput.getText().length() == 0 ||
               widthInput.getText().length() == 0 ||
               lengthInput.getText().length() == 0 ||
               shapeBox.getValue() == null || shapeBox.getValue().toString().length() == 0){
                incompleteDataWarning.show(stage.stage);
            } else {
                pdDimensions.add(new PadDimmirror( //args: int padId, double length, double width, Package.PadShape shape, double holeDiam, double originX, double originY, boolean padExposed
                        Integer.parseInt(padIdInput.getText()),
                        Double.parseDouble(lengthInput.getText()),
                        Double.parseDouble(widthInput.getText()),
                        Package.padShapefromString((String)shapeBox.getValue()),
                        (holeInput.getText().length() > 0) ? Double.parseDouble(holeInput.getText()) : 0,
                        (oriXinput.getText().length() > 0) ? Double.parseDouble(oriXinput.getText()) : 0,
                        (oriYinput.getText().length() > 0) ? Double.parseDouble(oriYinput.getText()) : 0,
                        exposedInput.isSelected()
                ));
                padIdInput.setText(Integer.toString(highestPadId() + 1));
                lengthInput.clear();
                widthInput.clear();
                shapeBox.valueProperty().set(null);
                holeInput.clear();
                oriXinput.clear();
                oriYinput.clear();
                exposedInput.setSelected(false);
                padshapeTable.scrollTo(padshapeTable.getItems().size() - 1);
                updateDimensions();
            }
        });
        shapeAdditionBox.getChildren().addAll(padIdInput, lengthInput, widthInput, holeInput,
                                              oriXinput, oriYinput, shapeBox, exposedInput, addShapeButton);

        /* shapeDeletionBox contains controls for removing items from the list */
        HBox shapeDeletionBox = new HBox(3);
        final Button shapeDeleteButton = new Button("Delete selected");
        shapeDeleteButton.setOnAction(e -> {
            PadDimmirror selectedItem = padshapeTable.getSelectionModel().getSelectedItem();
            padshapeTable.getItems().remove(selectedItem);
            updateDimensions();
        });
        shapeDeletionBox.getChildren().addAll(shapeDeleteButton);

        shaBranch.getChildren().addAll(padshapeTable, shapeAdditionBox ,shapeDeletionBox );
        padshapeholder.setContent(shaBranch);

        return padshapeholder;
    }

    private TitledPane initPadposholder(){
        TitledPane padposholder = new TitledPane();
        padposholder.setText("Pad positions");
        padposholder.setExpanded(false);
        VBox posBranch = new VBox(5);//parameter is spacing

        TableView<PadPosmirror> padposTable = new TableView<>();
        padposTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        padposTable.setEditable(true);
        padposTable.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn, TableCell> cellFactory = (TableColumn p) -> new CustomCell();
        Callback<TableColumn, TableCell> cellIntFactory = (TableColumn p) -> new EditingIntCell();
        Callback<TableColumn, TableCell> cellDoubleFactory = (TableColumn p) -> new EditingDoubleCell();

        TableColumn idCol = new TableColumn("pin-id");
        idCol.setMinWidth(100);
        idCol.setCellValueFactory(new PropertyValueFactory<>("pinIdppt"));
        idCol.setCellFactory(cellFactory);
        idCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosmirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadPosmirror, String> t) {
                    ((PadPosmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setPinId(t.getNewValue());
                    updatePositions();
                }
            }
        );

        TableColumn padidCol = new TableColumn("pad-id");
        padidCol.setMinWidth(100);
        padidCol.setCellValueFactory(new PropertyValueFactory<>("padIdppt"));
        padidCol.setCellFactory(cellIntFactory);
        padidCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosmirror, Integer>>() {
                @Override
                public void handle(CellEditEvent<PadPosmirror, Integer> t) {
                    ((PadPosmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setPadId(t.getNewValue());
                    updatePositions();
                }
            }
        );

        TableColumn xposCol = new TableColumn("X");
        xposCol.setMinWidth(100);
        xposCol.setCellValueFactory(new PropertyValueFactory<>("xPosppt"));
        xposCol.setCellFactory(cellDoubleFactory);
        xposCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadPosmirror, Double> t) {
                    ((PadPosmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setXPos(t.getNewValue());
                    updatePositions();
                }
            }
        );

        TableColumn yposCol = new TableColumn("Y");
        yposCol.setMinWidth(100);
        yposCol.setCellValueFactory(new PropertyValueFactory<>("yPosppt"));
        yposCol.setCellFactory(cellDoubleFactory);
        yposCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosmirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadPosmirror, Double> t) {
                    ((PadPosmirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                            ).setYPos(t.getNewValue());
                    updatePositions();
                }
            }
        );

        TableColumn rotCol = new TableColumn("rotation");
        rotCol.setMinWidth(100);
        rotCol.setCellValueFactory(new PropertyValueFactory<>("rotationppt"));
        rotCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.orientationValues()));
        rotCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosmirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadPosmirror, String> t) {
                    ((PadPosmirror) t.getTableView().getItems().get(t.getTablePosition().getRow())).setRotation(t.getNewValue());
                    updatePositions();
                }
            }
        );

        padposTable.setItems(pdPositions);
        padposTable.getColumns().addAll(idCol, padidCol, xposCol, yposCol, rotCol);

        HBox positionAdditionBox = new HBox(3);

        pinIdInput = new TextField(Integer.toString(highestPinId() + 1));
        pinIdInput.setMaxWidth(dimInputPrefWidth);
        pinIdInput.setPromptText("pin-id");
        pinIdInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pinIdInput, false, false)){
                    //TODO: maybe dissable 'add'button
                }
            }
        });

        pinPadIdInput = new TextField();
        pinPadIdInput.setMaxWidth(dimInputPrefWidth);
        pinPadIdInput.setPromptText("pad-id");
        pinPadIdInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pinPadIdInput, false, false)){
                    //see above
                }
            }
        });

        final TextField posXinput = new TextField();
        posXinput.setMaxWidth(dimInputPrefWidth);
        posXinput.setPromptText("X");
        posXinput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(posXinput)){
                    //see above
                }
            }
        });

        final TextField posYinput = new TextField();
        posYinput.setMaxWidth(dimInputPrefWidth);
        posYinput.setPromptText("Y");
        posYinput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(posYinput)){
                    //see above
                }
            }
        });

        final ComboBox rotationBox = new ComboBox();
        rotationBox.getItems().addAll(Package.orientationValues());

        final Button addPosButton = new Button("Add");
        addPosButton.setOnAction((ActionEvent e) -> {
            pdPositions.add(new PadPosmirror( //args: String pinId, double xPos, double yPos, int padId, Package.Orientation rotation
                    pinIdInput.getText(),
                    Double.parseDouble(posXinput.getText()),
                    Double.parseDouble(posYinput.getText()),
                    Integer.parseInt(pinPadIdInput.getText()),
                    Package.orientationFromInt(Integer.parseInt((String)rotationBox.getValue()))
            ));
            pinIdInput.setText(Integer.toString(highestPinId() + 1));
            pinPadIdInput.setText(Integer.toString(mostRecentPadId()));
            double[] predict = {0.0, 0.0};
            if(predictPadPosition(predict)){
                posXinput.setText(Double.toString(predict[0]));
                posYinput.setText(Double.toString(predict[1]));
            } else {
                posXinput.clear();
                posYinput.clear();
            }
            rotationBox.valueProperty().set(Integer.toString(mostRecentPadRotation()));
            padposTable.scrollTo(padposTable.getItems().size() - 1);
            updatePositions();
        });
        positionAdditionBox.getChildren().addAll(pinIdInput, pinPadIdInput, posXinput, posYinput, rotationBox, addPosButton);

        HBox positionDeletionBox = new HBox(3);
        final Button posDeleteButton = new Button("Delete selected");
        positionDeletionBox.getChildren().addAll(posDeleteButton);
        posDeleteButton.setOnAction(e -> {
            PadPosmirror selectedItem = padposTable.getSelectionModel().getSelectedItem();
            padposTable.getItems().remove(selectedItem);
            updatePositions();
        });

        posBranch.getChildren().addAll(padposTable, positionAdditionBox ,positionDeletionBox );
        padposholder.setContent(posBranch);

        return padposholder;
    }
    private int highestPadId(){
        int highest = 0;
        for(PadDimmirror p: pdDimensions){
            if(p.padId > highest){
                highest = p.padId;
            }
        }
        return highest;
    }
    private int highestPinId(){
        int highest = 0;
        for(PadPosmirror p: pdPositions){
            try{
                if(Integer.parseInt(p.pinId) > highest){
                    highest = Integer.parseInt(p.pinId);
                }
            } catch(NumberFormatException e){
                //do some complicated prediction of what highest should be...
                //just kidding, that's never going to happen.
            }
        }
        return highest;
    }

    private int mostRecentPadId(){
        int pad = 1;
        int count = pdPositions.size();
        if(count > 0){
            pad = pdPositions.get(count - 1).getPadIdppt();
        }
        return pad;
    }

    private int mostRecentPadRotation(){
        int rotation = 0;
        int count = pdPositions.size();
        if(count > 0){
            rotation = pdPositions.get(count - 1).getRotationppt();
        }
        return rotation;
    }

    private boolean predictPadPosition(double[] pos){
        int count = pdPositions.size();
        if(count < 2){
            return false;
        }
        double xdelta = pdPositions.get(count - 1).getXPosppt() - pdPositions.get(count - 2).getXPosppt();
        double ydelta = pdPositions.get(count - 1).getYPosppt() - pdPositions.get(count - 2).getYPosppt();
        if(roughCompare(xdelta, 0) && (roughCompare(ydelta, crntPkg.pitch) || roughCompare(-ydelta, crntPkg.pitch))){
            pos[0] = pdPositions.get(count - 1).getXPosppt();
            pos[1] = pdPositions.get(count - 1).getYPosppt() + ydelta;
            /* the calculation may give a lot of decimals, due to the limited
             * accuracy of floating point; round to 4 decimals max.
             */
            pos[1] = (double)Math.round(pos[1] * 10000) / 10000;
            return true;
        } else if((roughCompare(xdelta, crntPkg.pitch) || roughCompare(-xdelta, crntPkg.pitch)) && roughCompare(ydelta, 0)){
            pos[0] = pdPositions.get(count - 1).getXPosppt() + xdelta;
            pos[0] = (double)Math.round(pos[0] * 1000) / 1000;  /* see comment above */
            pos[1] = pdPositions.get(count - 1).getYPosppt();
            return true;
        }
        return false;
    }

    private void initPopups(){
        /* the style String will be used for all Popups. TODO: learn more about the formatting */
        String style = "-fx-border-color:#a9a9a9; -fx-border-radius:4px; -fx-border-width:3px; -fx-background-color:#f0f0ea; -fx-padding:4px 8px 4px 8px;";

        /* dupWarning is shown when attempting to add a name/alias/ipcname that already exists */
        dupWarning = new DuplicateWarning();
        final VBox dupWaBranch = new VBox(5);
        dupWaBranch.setAlignment(Pos.CENTER);
        dupWaBranch.setStyle(style);
        final Label warn = new Label("The name already exists as a\npackage name, alias or variant.");
        warn.setPadding(new Insets(0, 0, fieldSpacing, 0)); // add spacing below text (above buttons)

        final HBox btnBox = new HBox(3);
        final Button goLook = new Button("View");
        goLook.setOnAction((ActionEvent arg0) -> {
            navigate(dupWarning.index());
            dupWarning.hide();
        });
        final Button okButt = new Button("Close");
        okButt.setOnAction((ActionEvent arg0) -> {
            dupWarning.hide();
        });
        btnBox.getChildren().addAll(goLook, okButt);
        dupWaBranch.getChildren().addAll(warn, btnBox);
        dupWarning.getContent().add(dupWaBranch);

        /* delWarning is shown when clicking the delete button. It's a confirmation window */
        delWarning = new Popup();
        final VBox delWaBranch = new VBox(5);
        delWaBranch.setAlignment(Pos.CENTER);
        delWaBranch.setStyle(style);
        final Label notice = new Label("Delete package:\nthis operation cannot be undone.\n\nAre you sure?");

        final HBox btnBox2 = new HBox(3);
        final Button confirm = new Button(" Yes ");
        confirm.setOnAction((ActionEvent arg0) -> {
            if(viewedPackages.size() < 2){             //never delete final Package, just reset it //TODO: fix for final Package if viewing selection
                crntPkg.reset();
                navigate(crntIndex);
                delWarning.hide();
            } else if(crntIndex == (viewedPackages.size() - 1)){
                viewedPackages.remove(crntPkg);
                allPackages.remove(crntPkg);
                if(!results.isEmpty()){
                    rectifySearchResults(crntIndex);
                }
                navigate(crntIndex - 1);
                delWarning.hide();
            } else{
                viewedPackages.remove(crntPkg);
                allPackages.remove(crntPkg);
                if(!results.isEmpty()){
                    rectifySearchResults(crntIndex);
                }
                navigate(crntIndex);
                delWarning.hide();
            }

        });
        final Button cancel = new Button(" No ");
        cancel.setOnAction((ActionEvent arg0) -> {
            delWarning.hide();
        });
        btnBox2.getChildren().addAll(confirm, cancel);
        delWaBranch.getChildren().addAll(notice, btnBox2);
        delWarning.getContent().add(delWaBranch);

        /* aboutPopup shows some typical 'about' info*/
        aboutPopup = new AboutPopup(style);

        /* exportPopup provides info on how to export - though that feature has not yet been implemented*/
        String exportText = "Make a selection through the search function in order to export packages.";
        exportPopup = new BasicPopup(style, exportText, "Close");

        /* warning popup that the selection has been canceled */
        String selectionCanceledText = "The package that you double-clicked on did not appear in the active selection.\n" +
                                       "Therefore, the selection was canceled (you are now viewing the full package list again).";
        selectionCanceledWarning = new BasicPopup(style, selectionCanceledText, "Close");

        /* warning that an item could not be added because some mandatory data was missing */
        String incompleteDataText = "The information could not be added, because one or more mandatory fields were left empty or undefined.\n" +
                                    "Please complete the data.";
        incompleteDataWarning = new BasicPopup(style, incompleteDataText, "Close");
    }


    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! IO methods & main() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    /**
     **************** MAIN() *******************
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Start the actual program*/
        launch(args);
        //System.exit(0);       //for debugging
    }

    /* This is run whenever the program exits normally. */
    private void saveAndQuit(){
        if(fileIsSet){
            try{
                if(save(loadedFile)){
                    //System.out.println("file saved");
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }

        try {
            /* get current size of the scene, and save this */
            double sceneWidth = mainScene.getWidth();
            double sceneHeight = mainScene.getHeight();
            Config.setWindowSize((int)sceneWidth, (int)sceneHeight);
            saveConfig(config);
        } catch (IOException e){
            /* ignore exception */
        }

        System.exit(0);
    }

    /* 5 */
    /* Methods for checking and creating a config file*/
    private void initConfig(){
        /* Set what is supposed to be the config directory, based on OS*/
        String configPath = setConfigPath();

        /* Check to see if a config file exists */
        configExists = checkConfig(configPath);
        if(!configExists){
            makeConfigDir(configPath);
        }
        loadConfig();
    }
    private static String setConfigPath(){
        String userHome = System.getProperty("user.home");
        String path = "";
        if(usingWindows()){
            // add /AppData/Local/PackageManager
            path = userHome + "\\AppData\\Local\\PackageManager";
        } else if(usingLinux()){
            // add /PackageManager
            path = userHome + "/.config/PackageManager";
        } else{
            System.out.println("unknown OS");
        }

        return path;
    }
    private static boolean usingWindows(){
        String osName = System.getProperty("os.name");
        return osName.contains("Windows");
    }
    private static String windowsConfigPath(){
        String path = System.getProperty("user.home");
        path += "\\AppData\\Local\\PackageManager\\config.json";
        return path;
    }
    private static boolean usingLinux(){
        String osName = System.getProperty("os.name");
        return osName.contains("Linux");
    }
    private static String linuxConfigPath(){
        String path = System.getProperty("user.home");
        path += "/.config/PackageManager/config.json";
        return path;
    }
    private static boolean checkConfig(String path){
        File file = new File(path);

        return file.exists();
    }
    private static void makeConfigDir(String path){
        /* create a directory*/
        File directory = new File(path);

        // true if the directory was created, false otherwise
        if (directory.mkdirs()) {

            /* create a config.json*/
            try{
                saveConfig(new Config(false, ""));
            } catch(IOException e){}
        } else {
            //Do nothing.
        }
    }
    private static boolean saveConfig(Config con) throws IOException{
        String configpath = usingWindows() ? windowsConfigPath() : linuxConfigPath();
        String json = Jsoner.serialize(con);
        try{
            if(write(json, configpath)){
                return true;
            }
        } catch(IOException e){
            System.out.println("failed to create config.json!");
            return false;
        }
        return false;
    }
    private void loadConfig(){
        String configpath = usingWindows() ? windowsConfigPath() : linuxConfigPath();
        boolean pathisset = false;
        String tmpPath = "";
        try (FileReader fileReader = new FileReader(configpath, StandardCharsets.UTF_8)) {
            JsonObject obj = (JsonObject)Jsoner.deserialize(fileReader);
            pathisset = obj.getBoolean(new BooleanKey("pathSet"));
            tmpPath = obj.getStringOrDefault(new StringKey("path"));
            int w = obj.getIntegerOrDefault(new IntegerKey("window-width"));
            int h = obj.getIntegerOrDefault(new IntegerKey("window-height"));
            if(w > 100){    /* apply some minimum size */
                prevWidth = w;
            }
            if(h > 100){
                prevHeight = h;
            }
        } catch(Exception e){
            System.out.println("failed to load config.json!");
        }
        config = new Config(pathisset, tmpPath);
        Config.setWindowSize((int)prevWidth, (int)prevHeight);
    }

    /* Methods for creating and loading .json files */
    private boolean save(File file) throws IOException{ //if no list of Packages is supplied as an argument then save allPackages
        if(file == null){//prevents errors when user hits cancel on 'save as' window
            return false;
        }
        return save(file, allPackages);
    }
    private boolean save(File file, ArrayList<Package> list) throws IOException{
        if(file == null){//prevents errors when user hits cancel on 'save as' window
            return false;
        }
        /* check path */
        String path = file.getPath();

        /* Java objects to JSON String */
        String json = Jsoner.serialize(list); //maybe serialize separately for more control?

        /* pretty print */
        json = Jsoner.prettyPrint(json);

        /* JSON String to JSON file */
        return write(json, path);
    }


    private static boolean write(String content, String path) throws IOException{
        try {
            FileWriter fileWriter = new FileWriter(path, StandardCharsets.UTF_8);
            fileWriter.write(content);
            fileWriter.close();
        }
        catch(IOException e){
            System.out.println("Failed to write file");
            return false;
        }
        return true;
    }

    private boolean load(File file) throws IOException, JsonException {
        if(file == null){          //prevents errors when user hits cancel on 'open file' window
            return false;
        }
        String path = file.getPath();
        try (FileReader fileReader = new FileReader(path, StandardCharsets.UTF_8)) {

            JsonArray deserialize = (JsonArray) Jsoner.deserialize(fileReader);
            ArrayList<JsonObject> jsonList = new ArrayList<>();
            ArrayList<Package> loadedPackages = new ArrayList<>();
            deserialize.asCollection(jsonList);

            for(JsonObject obj : jsonList){
                Package newpac = new Package();

                /* names */
                ArrayList<String> jsnNameList = obj.getCollection(new SimpleKey("names"));
                newpac.names = jsnNameList.toArray(new String[jsnNameList.size()]);

                /* description */
                newpac.description = obj.getStringOrDefault(new StringKey("description"));

                /* chartype */
                newpac.type = Package.charTypefromString(obj.getStringOrDefault(new StringKey("type")));

                /* termtype */
                newpac.termination = Package.termTypefromString(obj.getStringOrDefault(new StringKey("terminal")));

                /* boolean polarized */
                newpac.polarized = obj.getBooleanOrDefault(new BooleanKey("polarized"));

                /* Body body */
                JsonObject jsnBody = (JsonObject)obj.getMapOrDefault(new SimpleKey("body"));
                if(jsnBody != null){
                    newpac.body.bodyX = jsnBody.getDoubleOrDefault(new DoubleKey("cx"));
                    newpac.body.bodyXtol = jsnBody.getDoubleOrDefault(new DoubleKey("tol-x"));
                    newpac.body.bodyY = jsnBody.getDoubleOrDefault(new DoubleKey("cy"));
                    newpac.body.bodyYtol = jsnBody.getDoubleOrDefault(new DoubleKey("tol-y"));
                    newpac.body.bodyOrgX = jsnBody.getDoubleOrDefault(new DoubleKey("x"));
                    newpac.body.bodyOrgY = jsnBody.getDoubleOrDefault(new DoubleKey("y"));
                }

                /* Lead2Lead lead2lead */
                JsonObject jsnl2l = (JsonObject)obj.getMapOrDefault(new SimpleKey("lead-to-lead"));
                if(jsnl2l != null){
                    newpac.lead2lead.x = jsnl2l.getDoubleOrDefault(new DoubleKey("cx"));
                    newpac.lead2lead.y = jsnl2l.getDoubleOrDefault(new DoubleKey("cy"));
                    newpac.lead2lead.xTol = jsnl2l.getDoubleOrDefault(new DoubleKey("tol-x"));
                    newpac.lead2lead.yTol = jsnl2l.getDoubleOrDefault(new DoubleKey("tol-y"));
                    newpac.lead2lead.orgX = jsnl2l.getDoubleOrDefault(new DoubleKey("x"));
                    newpac.lead2lead.orgY = jsnl2l.getDoubleOrDefault(new DoubleKey("y"));
                }

                /* int nrOfPins; */
                newpac.nrOfPins = obj.getIntegerOrDefault(new IntegerKey("pin-count"));

                /* double pitch */
                newpac.pitch = obj.getDoubleOrDefault(new DoubleKey("pitch"));

                /* Orientation tapeOrient; */
                newpac.tapeOrient = Package.orientationFromInt(obj.getIntegerOrDefault(new IntegerKey("tape-orientation")));

                /* SpecificPackage[] ...renamed to "variants" in Json. Keeps old name in source code TODO: turn to variants everywhere */
                ArrayList<JsonObject> jsnSpepas = obj.getCollectionOrDefault(new SimpleKey("variants"));
                ArrayList<SpepaMirror> loadedSpepas = new ArrayList<>();
                if(jsnSpepas != null){
                    for(JsonObject jsnSpep : jsnSpepas){
                        double minHeight = 0, maxHeight = 0;
                        JsonObject spepHeight = (JsonObject)jsnSpep.getMapOrDefault(new SimpleKey("height"));
                        if(spepHeight != null){
                            minHeight = spepHeight.getDoubleOrDefault(new DoubleKey("low"));
                            maxHeight = spepHeight.getDoubleOrDefault(new DoubleKey("high"));
                        }
                        SpepaMirror spep = new SpepaMirror(
                            jsnSpep.getStringOrDefault(new StringKey("name")),
                            Package.nameStandardFromString(jsnSpep.getStringOrDefault(new StringKey("standard"))),
                            minHeight, maxHeight,
                            jsnSpep.getBooleanOrDefault(new BooleanKey("exposed-pad")),
                            jsnSpep.getStringOrDefault(new StringKey("notes"))
                        );
                        loadedSpepas.add(spep);
                    }
                    newpac.specPacks = new Package.SpecificPackage[loadedSpepas.size()];
                    for(SpepaMirror spep : loadedSpepas){
                        newpac.specPacks[loadedSpepas.indexOf(spep)] = newpac.new SpecificPackage(
                            spep.spepaName,
                            spep.standard,
                            spep.minHeight,
                            spep.maxHeight,
                            spep.padExposed,
                            spep.spepaNotes
                        );
                    }
                }

                /* footprints[]        currently only 1 footprint supported */
                ArrayList<JsonObject> jsnFootprints = obj.getCollection(new SimpleKey("footprints"));
                //ArrayList<FootprintMirror> loadedFootprints = new ArrayList<>();    //eventual support for multiple footprints
                for(JsonObject jsnFootprint : jsnFootprints){
                    newpac.footPrints[firstid].ftprntType = Package.footprintTypefromString(jsnFootprint.getStringOrDefault(new StringKey("type")));

                    /* Span span */
                    JsonObject jsnspan = (JsonObject)jsnFootprint.getMapOrDefault(new SimpleKey("span"));
                    if(jsnspan != null){
                        newpac.footPrints[firstid].span.x = jsnspan.getDoubleOrDefault(new DoubleKey("cx"));
                        newpac.footPrints[firstid].span.y = jsnspan.getDoubleOrDefault(new DoubleKey("cy"));
                    }

                    /* Outline / contour */
                    JsonObject jsnotl = (JsonObject)jsnFootprint.getMapOrDefault(new SimpleKey("contour"));
                    if(jsnotl != null){
                        newpac.footPrints[firstid].outline.length = jsnotl.getDoubleOrDefault(new DoubleKey("cx"));
                        newpac.footPrints[firstid].outline.width = jsnotl.getDoubleOrDefault(new DoubleKey("cy"));
                        newpac.footPrints[firstid].outline.orgX = jsnotl.getDoubleOrDefault(new DoubleKey("x"));
                        newpac.footPrints[firstid].outline.orgY = jsnotl.getDoubleOrDefault(new DoubleKey("y"));
                    }


                    /* padDimensions[] */
                    ArrayList<JsonObject> jsnDimensions = jsnFootprint.getCollectionOrDefault(new SimpleKey("pad-shapes"));
                    ArrayList<PadDimmirror> loadedDimensions = new ArrayList<>();
                    if(jsnDimensions != null){
                        for(JsonObject jsnDim : jsnDimensions){
                            PadDimmirror dim = new PadDimmirror(
                                jsnDim.getIntegerOrDefault(new IntegerKey("pad-id")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("cx")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("cy")),
                                Package.padShapefromString(jsnDim.getStringOrDefault(new DoubleKey("shape"))),
                                jsnDim.getDoubleOrDefault(new DoubleKey("hole-diameter")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("x")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("y")),
                                jsnDim.getBooleanOrDefault(new BooleanKey("exposed-pad"))
                            );
                            loadedDimensions.add(dim);
                        }
                        newpac.footPrints[firstid].dimensions = new Package.Footprint.PadDimension[loadedDimensions.size()];
                        for(PadDimmirror dim : loadedDimensions){
                            newpac.footPrints[firstid].dimensions[loadedDimensions.indexOf(dim)] = newpac.footPrints[firstid].new PadDimension(
                                dim.padId,
                                dim.length,
                                dim.width,
                                dim.shape,
                                dim.holeDiam,
                                dim.originX,
                                dim.originY,
                                dim.padExposed
                            );
                        }
                    }

                    /* padPositions[] */
                    ArrayList<JsonObject> jsnPositions = jsnFootprint.getCollectionOrDefault(new SimpleKey("pad-positions"));
                    ArrayList<PadPosmirror> loadedPositions = new ArrayList<>();
                    if(jsnPositions != null){
                        for(JsonObject jsnDim : jsnPositions){
                            PadPosmirror pos = new PadPosmirror(
                                jsnDim.getStringOrDefault(new StringKey("pin-id")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("x")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("y")),
                                jsnDim.getIntegerOrDefault(new IntegerKey("pad-id")),
                                Package.orientationFromInt(jsnDim.getIntegerOrDefault(new IntegerKey("rotation")))
                            );
                            loadedPositions.add(pos);
                        }
                        newpac.footPrints[firstid].padPositions = new Package.Footprint.PadPosition[loadedPositions.size()];
                        for(PadPosmirror pos : loadedPositions){
                            newpac.footPrints[firstid].padPositions[loadedPositions.indexOf(pos)] = newpac.footPrints[firstid].new PadPosition(
                                pos.pinId,
                                pos.xPos,
                                pos.yPos,
                                pos.padId,
                                pos.rotation
                            );
                        }
                    }
                }
                loadedPackages.add(newpac);
            }
            Collections.sort(loadedPackages, Package.FirstName);
            allPackages = loadedPackages;
            setPackageSelection(false, 0);
        } catch(Exception e){
            return false;
        }
        return true;
    }

    /* 6 */
    /* This method swaps crntPackage with another Package from viewdPackages*/
    private void navigate(int newIndex){
        if(fileIsSet){
            try{
                if(save(loadedFile)){
                    //do nothing
                } else{
                    System.out.println("auto-save failed");
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        crntPkg = viewedPackages.get(newIndex);
        crntIndex = newIndex;
        loadAll();
    }

    private void updateIndex(){
        String s = "";
        if(viewingSelection){
            s = "Selection: ";
        }
        String t = Integer.toString(crntIndex + 1) + " of " + Integer.toString(viewedPackages.size());
        s += t;
        displayedIndex.setText(s);
    }

    /* The load[something] methods fill the UI controls with data from crntPkg */

    private void loadImage(){
        setScale();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); //wipe canvas
        gc.setGlobalAlpha(1.0);     //set brush opacity (back) to full

        /* make background white*/
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        /* draw Y axis */
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.2);
        gc.strokeLine(imgXcenter(), 0, imgXcenter(), canvas.getHeight()); // startX, startY, endX, endY
        /* draw X axis */
        gc.strokeLine(0, imgYcenter(), canvas.getWidth(), imgYcenter());
        /* draw scale indicator */
        gc.setLineWidth(1);
        double indicWidth = scale(1);
        double indicXstart = 8;
        double indicXend = indicXstart + indicWidth;
        double indicY = canvas.getHeight() - (canvas.getHeight()/40) - 2; //set at XX% of window height
        gc.strokeLine(indicXstart, indicY, indicXend, indicY); //main line
        gc.strokeLine(indicXstart, indicY - 4, indicXstart, indicY + 4); //start clarity line
        gc.strokeLine(indicXend, indicY - 4, indicXend, indicY + 4); //end clarity line
        gc.setFill(Color.BLACK);
        gc.fillText("1mm", indicXend + 4.5, indicY + 3.5, 20); //draw "1mm" text

        /* draw body */
        double scaledWidth = scale(crntPkg.body.bodyX);
        double scaledHeight = scale(crntPkg.body.bodyY);
        double drawOrgX = imgXcenter() + offset(scaledWidth) + scale(crntPkg.body.bodyOrgX);
        double drawOrgY = imgYcenter() + offset(scaledHeight) - scale(crntPkg.body.bodyOrgY);

        gc.setStroke(Color.color(0, 0.2, 0.4));
        gc.setLineWidth(1.5);

        gc.strokeRect(drawOrgX, drawOrgY, scaledWidth, scaledHeight);  //X, Y, W, H

        /* draw pads */
        gc.setGlobalAlpha(0.7);
        for(PadDimmirror dim : pdDimensions){

            ArrayList<PadPosmirror> drawnPads = new ArrayList();    //fill a list with padpositions that match the pad id
            for(PadPosmirror pad : pdPositions){
                if(pad.padId == dim.padId){
                    drawnPads.add(pad);
                }
            }
            for(PadPosmirror pad : drawnPads){
                gc.setGlobalAlpha(0.5);
                if(dim.getPadExposed()){
                    gc.setFill(Color.color(0.9, 0.4, 0.1));
                } else{
                    gc.setFill(Color.color(0.9, 0.1, 0.2));
                }

                Polygon padPoly = Polygon.FromRect(dim.width, dim.length);
                padPoly.Move(-dim.originX, -dim.originY);               /* apply origin of the pad shape */
                padPoly.Rotate(Package.orientationAsInt(pad.rotation)); /* apply pad rotation */
                padPoly.Move(pad.xPos, pad.yPos);                       /* add pad offset from the body */
                padPoly.Scale(scaleFactor);                             /* scale pad dimensions & position for drawing */
                padPoly.Flip(Polygon.FlipType.FLIP_Y);                  /* toggle Y-axis */
                padPoly.Move(imgXcenter(), imgYcenter());               /* reposition relative to centre of drawing */

                double scaledPadWidth = padPoly.Right() - padPoly.Left();
                double scaledPadHeight = padPoly.Top() - padPoly.Bottom();
                double arcWidth;
                double arcHeight;
                switch(dim.shape){
                    case RECTANGLE:
                        gc.fillRect(padPoly.Left(), padPoly.Bottom(), scaledPadWidth, scaledPadHeight);
                        break;
                    case ROUND:
                        gc.fillOval(padPoly.Left(), padPoly.Bottom(), scaledPadWidth, scaledPadHeight);
                        break;
                    case ROUNDEDRECT:
                        arcWidth = scale(smallestDim(dim))*0.67;
                        arcHeight = scale(smallestDim(dim))*0.67;
                        gc.fillRoundRect(padPoly.Left(), padPoly.Bottom(), scaledPadWidth, scaledPadHeight, arcWidth, arcHeight);
                        break;
                    case OBROUND:
                        arcWidth = scale(smallestDim(dim));
                        arcHeight = scale(smallestDim(dim));
                        gc.fillRoundRect(padPoly.Left(), padPoly.Bottom(), scaledPadWidth, scaledPadHeight, arcWidth, arcHeight);
                }
                /* Draw pad holes */
                if(!roughCompare(dim.holeDiam, 0)){
                    gc.setStroke(Color.color(0.9, 0.1, 0.2));
                    gc.setFill(Color.WHITE);
                    gc.setGlobalAlpha(1.0);
                    double holeX = imgXcenter() + offset(scale(dim.holeDiam)) + scale(pad.xPos);
                    double holeY = imgYcenter() + offset(scale(dim.holeDiam)) - scale(pad.yPos);
                    gc.fillOval(holeX, holeY, scale(dim.holeDiam), scale(dim.holeDiam));
                    gc.strokeOval(holeX, holeY, scale(dim.holeDiam), scale(dim.holeDiam));
                }
            }
        }
    }
    /* image support methods */
    private double imgXcenter(){
        return canvas.getWidth()/2;
    }
    private double imgYcenter(){
        return canvas.getHeight()/2;
    }
    private void setScale(){
        /* calculate the courtyard, the bounding around body, lead-to-lead and
         * footprint (use the nominal sizes for body & lead-to-lead, so ignore
         * tolerances)
         */
        Courtyard bbox = new Courtyard();
        bbox.AddBoundingBox(crntPkg.body.bodyX,
                            crntPkg.body.bodyY,
                            crntPkg.body.bodyOrgX,
                            crntPkg.body.bodyOrgY);
        bbox.AddBoundingBox(crntPkg.lead2lead.x,
                            crntPkg.lead2lead.y,
                            crntPkg.lead2lead.orgX,
                            crntPkg.lead2lead.orgY);
        bbox.AddBoundingBox(crntPkg.footPrints[firstid].outline.length,
                            crntPkg.footPrints[firstid].outline.width,
                            crntPkg.footPrints[firstid].outline.orgX,
                            crntPkg.footPrints[firstid].outline.orgY);

        /* cx & cy are half of the required courtyard span, in mm */
        double cx = Math.max(-bbox.Left(), bbox.Right());
        double cy = Math.max(-bbox.Bottom(), bbox.Top());

        /* calculate the scale factor for width & height, and pick the smallest one */
        final int margin = 8;   /* margin on all sides of the footprint */
        double scale_x = (canvas.getWidth() - 2 * margin) / (2 * cx);
        double scale_y = (canvas.getHeight() - 2 * margin) / (2 * cy);
        scaleFactor = Math.min(scale_x, scale_y);

        /* tweak the scale factor: clamp it to a maximum, and make it smaller
         * in discrete steps (of roughly 33%), but give up when the scale factor
         * becomes small
         */
        if(scaleFactor > 60){
            scaleFactor = 60;   /* factor limited to 60 max. */
        } else if(scaleFactor > 40){
            scaleFactor = 40;   /* factors in range 40..60 rounded down to 40 */
        } else if(scaleFactor > 27){
            scaleFactor = 27;   /* factors in range 27..40 rounded down to 27 */
        } else if(scaleFactor > 18){
            scaleFactor = 18;   /* factors in range 18..27 rounded down to 18 */
        } else if(scaleFactor > 12){
            scaleFactor = 12;   /* factors in range 12..18 rounded down to 12 */
        } else if(scaleFactor > 8){
            scaleFactor = 8;    /* factors in range 8..12 rounded down to 8 */
        } else if(scaleFactor > 5){
            scaleFactor = 5;    /* factors in range 5..8 rounded down to 5 */
        }
    }
    private double smallestDim(PadDimmirror dim){
        return dim.length <= dim.width ? dim.length : dim.width;
    }
    private double scale(double input){ //TODO set variable scaling
        return input * scaleFactor;
    }
    private double offset(double widthOrHeight){
        return 0.0 - (widthOrHeight/2);
    }


    /* Other loading methods */
    private void loadDescription(){
        descField.setText(crntPkg.description);
    }

    private void loadCharacteristics(){
        charatypeBox.setValue(Package.charTypeasString(crntPkg.type));
        terminationBox.setValue(Package.termTypeasString(crntPkg.termination));
        polarCheck.setSelected(crntPkg.polarized);
        pinnumber.setText(Integer.toString(crntPkg.nrOfPins));
        pitchField.setText(Double.toString(crntPkg.pitch));
        tapeOrientation.setValue(Integer.toString(Package.orientationAsInt(crntPkg.tapeOrient)));
    }

    private void loadBodySize(){
        bodyXsize.setText(Double.toString(crntPkg.body.bodyX));
        bodyXtol.setText(Double.toString(crntPkg.body.bodyXtol));
        bodyYsize.setText(Double.toString(crntPkg.body.bodyY));
        bodyYtol.setText(Double.toString(crntPkg.body.bodyYtol));
        bodyOrgX.setText(Double.toString(crntPkg.body.bodyOrgX));
        bodyOrgY.setText(Double.toString(crntPkg.body.bodyOrgY));
    }

    private void loadLeadToLead(){
        ltolXsize.setText(Double.toString(crntPkg.lead2lead.x));
        ltolXtol.setText(Double.toString(crntPkg.lead2lead.xTol));
        ltolYsize.setText(Double.toString(crntPkg.lead2lead.y));
        ltolYtol.setText(Double.toString(crntPkg.lead2lead.yTol));
        ltolOrgX.setText(Double.toString(crntPkg.lead2lead.orgX));
        ltolOrgY.setText(Double.toString(crntPkg.lead2lead.orgY));
    }

    private void loadFootPrint(){
        footprinttypeBox.setValue(Package.footprintTypeasString(crntPkg.getfpType(0)));
        spanXField.setText(Double.toString(crntPkg.footPrints[firstid].span.x));
        spanYField.setText(Double.toString(crntPkg.footPrints[firstid].span.y));
        fpolLength.setText(Double.toString(crntPkg.footPrints[firstid].outline.length));
        fpolWidth.setText(Double.toString(crntPkg.footPrints[firstid].outline.width));
        fpolOrgX.setText(Double.toString(crntPkg.footPrints[firstid].outline.orgX));
        fpolOrgY.setText(Double.toString(crntPkg.footPrints[firstid].outline.orgY));
    }

    private void loadSpePacks(){
        if(!spePacks.isEmpty()){        //if this list is not empty, empty it
            spePacks.clear();
        }
        if(crntPkg.specPacks != null){
            for(Package.SpecificPackage spepa : crntPkg.specPacks){
                spePacks.add(new SpepaMirror(spepa));
            }
        }
    }

    private void loadDimensions(){
        if(!pdDimensions.isEmpty()){
            pdDimensions.clear();
        }
        if(crntPkg.footPrints[0].dimensions != null){
            for(Package.Footprint.PadDimension dimension : crntPkg.footPrints[0].dimensions){ //only 1 footprint in current version
                pdDimensions.add(new PadDimmirror(dimension));
            }
        }
        if(padIdInput != null){ //the first time this method is called the UI elements will not have been initialized
            padIdInput.setText(Integer.toString(highestPadId() + 1));
        }
    }

    private void loadPositions(){
        if(!pdPositions.isEmpty()){
            pdPositions.clear();
        }
        if(crntPkg.footPrints[0].padPositions != null){
            for(Package.Footprint.PadPosition position : crntPkg.footPrints[0].padPositions){ //only 1 footprint in current version
                pdPositions.add(new PadPosmirror(position));
            }
        }
        if(pinIdInput != null){ //the first time this method is called the UI elements will not have been initialized
            pinIdInput.setText(Integer.toString(highestPinId() + 1));
        }
        if(pinPadIdInput != null){
            pinPadIdInput.setText("1"); //padId '2' is usually for exposed pads for which there will only be one
        }
    }


    private void loadAll(){
        updateIndex();
        nameBox.load();
        loadDescription();
        loadCharacteristics();
        loadBodySize();
        loadLeadToLead();
        loadFootPrint();
        loadSpePacks();
        loadDimensions();
        loadPositions();
        loadImage();

        /* check dimensions are loading everything */
        checkBodySize();
        checkLeadToLead();
        checkSpan();
        checkContour();
    }

    private void updateVariants(){
        crntPkg.specPacks = new Package.SpecificPackage[spePacks.size()];
        for(SpepaMirror spep: spePacks){
            crntPkg.specPacks[spePacks.indexOf(spep)] = crntPkg.new SpecificPackage(spep.spepaName, spep.standard, spep.minHeight, spep.maxHeight, spep.padExposed, spep.spepaNotes);
        }
    }
    private void updateDimensions(){
        crntPkg.footPrints[firstid].dimensions = new Package.Footprint.PadDimension[pdDimensions.size()];
        for(PadDimmirror dim: pdDimensions){
            crntPkg.footPrints[firstid].dimensions[pdDimensions.indexOf(dim)] = crntPkg.footPrints[firstid].new PadDimension(
                    dim.padId, dim.length, dim.width, dim.shape, dim.holeDiam, dim.originX, dim.originY, dim.padExposed);
        }
        loadImage();
        checkContour();
    }
    private void updatePositions(){
        crntPkg.footPrints[firstid].padPositions = new Package.Footprint.PadPosition[pdPositions.size()];
        for(PadPosmirror pos: pdPositions){
            crntPkg.footPrints[firstid].padPositions[pdPositions.indexOf(pos)] = crntPkg.footPrints[firstid].new PadPosition(
                    pos.pinId, pos.xPos, pos.yPos, pos.padId, pos.rotation);
        }
        loadImage();
        checkSpan();
        checkContour();
    }

    private void checkBodySize(){
        try {
            String warnText = "This value should not be zero.";

            boolean warn = roughCompare(crntPkg.body.bodyX, 0);
            bodyXsize.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
            String ttipText = "Horizontal size of the body (excluding pins).";
            if(warn){
                ttipText += "\n" + warnText;
            }
            final Tooltip bodyXtip = new Tooltip(ttipText);
            bodyXsize.setTooltip(bodyXtip);

            warn = roughCompare(crntPkg.body.bodyY, 0);
            bodyYsize.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
            ttipText = "Vertical size of the body (excluding pins).";
            if(warn){
                ttipText += "\n" + warnText;
            }
            final Tooltip bodyYtip = new Tooltip(ttipText);
            bodyYsize.setTooltip(bodyYtip);
        } catch (Exception e) {
            /* ignore any reason why checking or colouring the input fields failed */
        }
    }
    private void checkLeadToLead(){
        try {
            /* make a difference between terminals that protrude from the package
             * and terminals that are below the package or at the edges of the
             * package
             */
            boolean no_protruding_pins = (crntPkg.termination == Package.TermType.ENDCAP ||
                                          crntPkg.termination == Package.TermType.NOLEAD ||
                                          crntPkg.termination == Package.TermType.BALLGRID ||
                                          crntPkg.termination == Package.TermType.LANDGRID ||
                                          crntPkg.termination == Package.TermType.CASTELLATED);

            String warnText = (no_protruding_pins
                                ? "Considering the terminals, this size must be the same as the body size, or left at zero."
                                : "Considering the terminals, this size must be bigger than (or equal to) the body size.");

            boolean warn;
            if(no_protruding_pins){
                /* for these terminals, lead-to-lead must be the same as body size,
                 *  or be left at zero.
                 */
                warn = !roughCompare(crntPkg.lead2lead.x, crntPkg.body.bodyX) && !roughCompare(crntPkg.lead2lead.x, 0);
            } else {
                /* for these terminals, leads are protruding from the package, so
                 * the lead-to-lead size must be larger than the body size in at
                 * least one direction
                 */
                warn = (crntPkg.lead2lead.x < crntPkg.body.bodyX - 0.0005) || roughCompare(crntPkg.lead2lead.x, 0);
            }
            ltolXsize.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
            String ttipText = "Horizontal lead-to-lead size of the package (including pins).";
            if(warn){
                ttipText += "\n" + warnText;
            }
            final Tooltip ltolXtip = new Tooltip(ttipText);
            ltolXsize.setTooltip(ltolXtip);

            if(no_protruding_pins){
                warn = !roughCompare(crntPkg.lead2lead.y, crntPkg.body.bodyY) && !roughCompare(crntPkg.lead2lead.y, 0);
            } else {
                warn = (crntPkg.lead2lead.y < crntPkg.body.bodyY - 0.0005) || roughCompare(crntPkg.lead2lead.y, 0);
            }
            ltolYsize.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
            ttipText = "Vertical lead-to-lead size of the package (including pins).";
            if(warn){
                ttipText += "\n" + warnText;
            }
            final Tooltip ltolYtip = new Tooltip(ttipText);
            ltolYsize.setTooltip(ltolYtip);
        } catch (Exception e) {
            /* ignore any reason why checking or colouring the input fields failed */
        }
    }
    private void checkSpan(){
        /* collect information about the pins */
        final int mismatchSpanX = 0x01;
        final int mismatchSpanY = 0x02;
        final int matchSpanX    = 0x04;
        final int matchSpanY    = 0x08;
        final int matchPitchX   = 0x10;
        final int matchPitchY   = 0x20;

        int[] padflag = new int[pdPositions.size()];
        for(int idx1 = 0; idx1 < pdPositions.size(); idx1++){
            PadPosmirror pin1 = pdPositions.get(idx1);
            for(int idx2 = idx1 + 1; idx2 < pdPositions.size(); idx2++){
                PadPosmirror pin2 = pdPositions.get(idx2);
                if(pin1.padId != pin2.padId)
                    continue;   /* only consider pads with the same pad-id */
                if (roughCompare(pin1.yPos, pin2.yPos)){
                    /* pins are horizontally aligned, check span between them */
                    double span = Math.abs(pin1.xPos - pin2.xPos);
                    if(roughCompare(span, crntPkg.footPrints[firstid].span.x)){
                        padflag[idx1] |= matchSpanX;
                        padflag[idx2] |= matchSpanX;
                    } else {
                        padflag[idx1] |= mismatchSpanX;
                        padflag[idx2] |= mismatchSpanX;
                    }
                    if(roughCompare(span, crntPkg.pitch)){
                        padflag[idx1] |= matchPitchX;
                        padflag[idx2] |= matchPitchX;
                    }
                }
                if (roughCompare(pin1.xPos, pin2.xPos)){
                    /* pins are vertically aligned, check span between them */
                    double span = Math.abs(pin1.yPos - pin2.yPos);
                    if(roughCompare(span, crntPkg.footPrints[firstid].span.y)){
                        padflag[idx1] |= matchSpanY;
                        padflag[idx2] |= matchSpanY;
                    } else {
                        padflag[idx1] |= mismatchSpanY;
                        padflag[idx2] |= mismatchSpanY;
                    }
                    if(roughCompare(span, crntPkg.pitch)){
                        padflag[idx1] |= matchPitchY;
                        padflag[idx2] |= matchPitchY;
                    }
                }

            }
        }

        /* when a pin has a match for a span (x or y), erase any mismatch
         * flags
         */
        for(int idx = 0; idx < padflag.length; idx++){
            if((padflag[idx] & (matchSpanX | matchSpanY | matchPitchX | matchPitchY)) != 0){
                padflag[idx] &= ~(mismatchSpanX | mismatchSpanY);
            }
        }

        /* TODO: SOT23 span cannot be detected with this algorithm */

        boolean warn_span_x = false;
        if(!roughCompare(crntPkg.footPrints[firstid].span.x, 0)){
            for(int idx = 0; idx < padflag.length; idx++){
                if((padflag[idx] & mismatchSpanX) != 0){
                    warn_span_x = true;
                }
            }
        }

        boolean warn_span_y = false;
        if(!roughCompare(crntPkg.footPrints[firstid].span.y, 0)){
            for(int idx = 0; idx < padflag.length; idx++){
                if((padflag[idx] & mismatchSpanY) != 0){
                    warn_span_y = true;
                }
            }
        }

        spanXField.setStyle("-fx-control-inner-background:" + (warn_span_x ? "#fff0a0;" : "white;"));
        String ttipText = "Distance between the left & right rows (pad centres)";
        if(warn_span_x){
            ttipText += "\nMismatch between this value and the pad definitions";
        }
        final Tooltip spanXFieldTip = new Tooltip(ttipText);
        spanXField.setTooltip(spanXFieldTip);

        spanYField.setStyle("-fx-control-inner-background:" + (warn_span_y ? "#fff0a0;" : "white;"));
        ttipText = "Distance between the top & bottom rows (pad centres)";
        if(warn_span_y){
            ttipText += "\nMismatch between this value and the pad definitions";
        }
        final Tooltip spanYFieldTip = new Tooltip(ttipText);
        spanYField.setTooltip(spanYFieldTip);
    }
    private void checkContour(){
        boolean firstpad = true;
        double contourLeft = 0.0, contourRight = 0.0, contourTop = 0.0, contourBottom = 0.0;

        /* run over all pads */
        for(PadPosmirror pin: pdPositions){
            /* find pad definition (ignore any pin for which no pad definition cannot be found) */
            for(PadDimmirror pad: pdDimensions){
                if(pad.padId == pin.getPadIdppt()){
                    Polygon padPoly = Polygon.FromRect(pad.width, pad.length);
                    padPoly.Move(-pad.originX, -pad.originY);               /* apply origin */
                    padPoly.Rotate(Package.orientationAsInt(pin.rotation)); /* apply pad rotation */
                    padPoly.Move(pin.xPos, pin.yPos);                       /* add pin offset */
                    /* update calculated contour */
                    double padLeft = padPoly.Left();
                    double padRight = padPoly.Right();
                    double padBottom = padPoly.Bottom();
                    double padTop = padPoly.Top();
                    /* for the very first pad, the contour is the pad itself */
                    if(padLeft < contourLeft || firstpad) contourLeft = padLeft;
                    if(padRight > contourRight || firstpad) contourRight = padRight;
                    if(padBottom < contourBottom || firstpad) contourBottom = padBottom;
                    if(padTop > contourTop || firstpad) contourTop = padTop;
                    firstpad = false;
                    break;      /* pin is handled, no need to search further */
                }
            }
        }

        /* now compare the calculated contour and origin with the stored values */
        boolean warn;
        String ttipText;
        final double roundFactor = 1000.0;

        assert contourLeft <= contourRight;
        double cx = contourRight - contourLeft;
        assert contourBottom <= contourTop;
        double cy = contourTop - contourBottom;

        warn = !roughCompare(crntPkg.footPrints[firstid].outline.length, cx);
        fpolLength.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Horizontal dimension of the contour of the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the bounding box around the pads (" + Double.toString(Math.round(cx*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolLengthTip = new Tooltip(ttipText);
        fpolLength.setTooltip(fpolLengthTip);

        warn = !roughCompare(crntPkg.footPrints[firstid].outline.width, cy);
        fpolWidth.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Vertical dimension of the contour of the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the bounding box around the pads (" + Double.toString(Math.round(cy*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolWidthTip = new Tooltip(ttipText);
        fpolWidth.setTooltip(fpolWidthTip);

        double ox = (contourLeft + contourRight) / 2;
        double oy = (contourBottom + contourTop) / 2;

        warn = !roughCompare(crntPkg.footPrints[firstid].outline.orgX, ox);
        fpolOrgX.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Horizontal offset of the origin from the geometric centre the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the origin calculated from the pads (" + Double.toString(Math.round(ox*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolOrgXTip = new Tooltip(ttipText);
        fpolOrgX.setTooltip(fpolOrgXTip);

        warn = !roughCompare(crntPkg.footPrints[firstid].outline.orgY, oy);
        fpolOrgY.setStyle("-fx-control-inner-background:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Vertical offset of the origin from the geometric centre the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the origin calculated from the pads (" + Double.toString(Math.round(oy*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolOrgYTip = new Tooltip(ttipText);
        fpolOrgY.setTooltip(fpolOrgYTip);
    }

    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Help functionality methods !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    private Scene initHelpScene(Stage primaryStage) {
        final FlowPane root = new FlowPane();
        final VBox helpBranch = new VBox(5);
        helpBranch.setFillWidth(true);
        helpBranch.prefWidthProperty().bind(root.widthProperty());

        /* go back to main Scene */
        final Button backButton = new Button("back");
        backButton.setCancelButton(true);
        backButton.setOnAction((ActionEvent arg0) -> {
            prevWidth = stage.stage.getWidth();
            prevHeight = stage.stage.getHeight();
            primaryStage.setScene(mainScene);
            stage.stage.setHeight(prevHeight);
            stage.stage.setWidth(prevWidth);
        });

        String url = getResourcePath();
        url = "file://" + url + "/help.html";

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.getEngine().load(url);
        
        //the css says to disable the horizontal scrollbar. It doesn't work for some reason. The weird part is that if you go to the css file and swap 'x' for 'y' then disabling the vertical scrollbar works just fine.
        String wvStylePath = getResourcePath() + "/webviewstyle.css";   
        File file = new File(wvStylePath);
        if(!file.exists()){
            System.out.println("Can't find webview stylesheet");
        } else{
            webView.getEngine().setUserStyleSheetLocation("file://" + wvStylePath);
        }

        helpBranch.setPadding(new Insets(fieldSpacing, fieldSpacing, 0, fieldSpacing));
        helpBranch.getChildren().addAll(backButton, webView);
        root.getChildren().add(helpBranch);
        Scene scene = new Scene(root, STARTING_WINDOW_WIDTH, STARTING_WINDOW_HEIGHT); //node, width, minHeight
        return scene;
    }
    private String getResourcePath(){
        String url;
        String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            url = URLDecoder.decode(path, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            url = path; /* cannot occur, in practice */
        }
        int last_slash = url.lastIndexOf('/');
        if(last_slash > 0){
            boolean trailing_slash = (last_slash == url.length() - 1);
            url = url.substring(0, last_slash);
            if(trailing_slash){
                /* the last slash was at the very end, get the last slash before
                 * that one (because we want to strip off the "classes" sub-path)
                 */
                last_slash = url.lastIndexOf('/');
                if(last_slash > 0){
                    url = url.substring(0, last_slash);
                }
            }
            /* also check whether trailing sub-path is "target" or "bin"; if
             * so, strip it off and replace it with "doc"
             */
            last_slash = url.lastIndexOf('/');
            String subpath = url.substring(last_slash + 1);
            if(subpath.equalsIgnoreCase("target") || subpath.equalsIgnoreCase("bin")){
                url = url.substring(0, last_slash) + "/doc";
            }
        }
        return url;
    }


    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Search functionality methods !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */

    /* 7 */
    /* Initializes GUI for search functions*/
    private Scene initSearchScene(){
        final ScrollPane root = new ScrollPane();
        final VBox searchBranch = new VBox(5);
        searchBranch.setFillWidth(true);
        searchBranch.prefWidthProperty().bind(root.widthProperty());
        final HBox topBox = new HBox(3);
        final HBox midBox = new HBox(3);
        midBox.setPadding(new Insets(fieldSpacing, fieldSpacing, 0, fieldSpacing));
        final GridPane searchGrid = new GridPane(); //is in midbox
        final HBox botBox = new HBox(3);

        final SearchConstraint sc = new SearchConstraint();

        /* go back to main Scene */
        final Button backButton = new Button("back");
        backButton.setCancelButton(true);
        backButton.setOnAction((ActionEvent arg0) -> {
            prevWidth = stage.stage.getWidth();
            prevHeight = stage.stage.getHeight();
            stage.stage.setScene(mainScene);
            stage.stage.setHeight(prevHeight);
            stage.stage.setWidth(prevWidth);
        });

        /* input for searching */
        searchField = new TextField();
        /* input action for searchField is set later, because it references a
         * control that is declared below
         */

        topBox.getChildren().addAll(backButton, searchField);


        /* TableView with SearchResults */
        final TableView<SearchResult> searchTable = new TableView<>();
        searchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        searchTable.setMinWidth(300);
        searchTable.setEditable(true);
        searchTable.setRowFactory(tv -> {
            TableRow<SearchResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    SearchResult res = row.getItem();
                    int newIndex = res.getOrgIndex();
                    /* newIndex is relative to allPackages, but if we are currently
                     * viewing a selection, this index should be translated
                     */
                    if(viewingSelection){
                        int selectionIndex = -1;
                        for(int idx = 0; idx < viewedPackages.size() && selectionIndex < 0; idx++){
                            if(viewedPackages.get(idx).equals(allPackages.get(newIndex))){
                                selectionIndex = idx;
                            }
                        }
                        if(selectionIndex < 0){
                            setPackageSelection(false, newIndex);
                            selectionCanceledWarning.show(stage.stage);
                        } else {
                            newIndex = selectionIndex;
                        }
                    }
                    navigate(newIndex);
                    prevWidth = stage.stage.getWidth();
                    prevHeight = stage.stage.getHeight();
                    stage.stage.setScene(mainScene);
                    stage.stage.setHeight(prevHeight);
                    stage.stage.setWidth(prevWidth);
                }
            });
            return row ;
        });

        TableColumn matchCol = new TableColumn("matches");
        matchCol.setEditable(false);
        matchCol.setMinWidth(200);
        matchCol.setCellValueFactory(new PropertyValueFactory<>("match"));

        TableColumn selectCol = new TableColumn();
        selectCol.setMinWidth(25);
        selectCol.setEditable(true);
        selectCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectCol.setCellFactory(new Callback<TableColumn<SearchResult,Boolean>,TableCell<SearchResult,Boolean>>(){
            @Override public TableCell<SearchResult,Boolean> call( TableColumn<SearchResult,Boolean> p ){
                CheckBoxTableCell<SearchResult, Boolean> cbt = new CheckBoxTableCell<>();
                cbt.setSelectedStateCallback(new Callback<Integer, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(Integer index) {
                        return searchTable.getItems().get(index).selectedProperty();
                    }
                });
                return cbt;
            }
        });
        CheckBox selectAll = new CheckBox("all");
        selectCol.setGraphic(selectAll);
        selectAll.setOnAction(e -> {
            for (SearchResult sr : results) {
                sr.setSelected(((CheckBox) e.getSource()).isSelected());
            }
        });

        searchTable.setItems(results);
        searchTable.getColumns().addAll(matchCol, selectCol);

        searchField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });


        /* defining content voor searchGrid */
        final int labelRow = 0;
        final int pinRow = labelRow + 1;
        final int pitchRow = pinRow + 1;
        final int spanRow = pitchRow + 1;
        final int heightRow = spanRow + 1;
        final int termRow = heightRow + 1;
        final int selTableRow = termRow + 1;
        final int selContRow = selTableRow + 1;

        final Label advancedSearch = new Label("additional constraints");
        GridPane.setConstraints(advancedSearch, 0, labelRow);

        final CheckBox pinCountCheck = new CheckBox("pin count");
        GridPane.setConstraints(pinCountCheck, 0, pinRow);
        pinCountCheck.setIndeterminate(false);
        final TextField pinConstraint = new TextField();
        GridPane.setConstraints(pinConstraint, 1, pinRow);
        pinConstraint.setMaxWidth(dimInputPrefWidth);
        pinCountCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov,Boolean old_val, Boolean new_val) {
                if(new_val){        // if checked
                    sc.isActive = true;
                    sc.hasPinConstraint = true;
                    try{
                        sc.pinConstraint = Integer.parseInt(pinConstraint.getText());
                    } catch(Exception e){
                        pinConstraint.setText("0");
                        sc.pinConstraint = 0;
                    }
                } else{             // if unchecked
                    sc.hasPinConstraint = false;
                    pinConstraint.clear();
                    sc.verifyIfActive();
                }
            }
        });
        pinConstraint.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(pinConstraint.getText().equals("")){
                    sc.hasPinConstraint = false;
                    pinCountCheck.setSelected(false);
                } else{
                    pinCountCheck.setSelected(true);
                    sc.isActive = true;
                    sc.pinConstraint = Integer.parseInt(pinConstraint.getText());
                    sc.hasPinConstraint = true;
                    fullSearch(allPackages, searchField.getText(), results, sc);
                    selectAll.setSelected(false);
                }
            }
        });

        final CheckBox pitchCheck = new CheckBox("pitch");
        GridPane.setConstraints(pitchCheck, 0, pitchRow);
        final TextField pitchConstraint = new TextField();
        GridPane.setConstraints(pitchConstraint, 1, pitchRow);
        pitchConstraint.setMaxWidth(dimInputPrefWidth);
        pitchCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov,Boolean old_val, Boolean new_val) {
                if(new_val){        // if checked
                    sc.isActive = true;
                    sc.hasPitchConstraint = true;
                    try{
                        sc.pitchConstraint = Double.parseDouble(pitchConstraint.getText());
                    } catch(Exception e){
                        pitchConstraint.setText("0.0");
                        sc.pitchConstraint = 0.0;
                    }
                } else{             // if unchecked
                    sc.hasPitchConstraint = false;
                    pitchConstraint.clear();
                    sc.verifyIfActive();
                }
            }
        });
        pitchConstraint.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(pitchConstraint.getText().equals("")){
                    sc.hasPitchConstraint = false;
                    pitchCheck.setSelected(false);
                } else{
                    pitchCheck.setSelected(true);
                    sc.isActive = true;
                    sc.pitchConstraint = Double.parseDouble(pitchConstraint.getText());
                    sc.hasPitchConstraint = true;
                    fullSearch(allPackages, searchField.getText(), results, sc);
                    selectAll.setSelected(false);
                }
            }
        });

        final CheckBox spanCheck = new CheckBox("span");
        GridPane.setConstraints(spanCheck, 0, spanRow);
        final TextField spanConstraint = new TextField();
        GridPane.setConstraints(spanConstraint, 1, spanRow);
        spanConstraint.setMaxWidth(dimInputPrefWidth);
        spanCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov,Boolean old_val, Boolean new_val) {
                if(new_val){        // if checked
                    sc.isActive = true;
                    sc.hasSpanConstraint = true;
                    try{
                        sc.spanConstraint = Double.parseDouble(spanConstraint.getText());
                    } catch(NumberFormatException e){
                        spanConstraint.setText("0.0");
                        sc.spanConstraint = 0.0;
                    }
                } else{             // if unchecked
                    sc.hasSpanConstraint = false;
                    spanConstraint.clear();
                    sc.verifyIfActive();
                }
            }
        });
        spanConstraint.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(spanConstraint.getText().equals("")){
                    sc.hasSpanConstraint = false;
                    spanCheck.setSelected(false);
                } else{
                    spanCheck.setSelected(true);
                    sc.isActive = true;
                    sc.spanConstraint = Double.parseDouble(spanConstraint.getText());
                    sc.hasSpanConstraint = true;
                    fullSearch(allPackages, searchField.getText(), results, sc);
                    selectAll.setSelected(false);
                }
            }
        });

        final CheckBox heightCheck = new CheckBox("height");
        GridPane.setConstraints(heightCheck, 0, heightRow);
        final TextField heightConstraint = new TextField();
        GridPane.setConstraints(heightConstraint, 1, heightRow);
        heightConstraint.setMaxWidth(dimInputPrefWidth);
        heightCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov,Boolean old_val, Boolean new_val) {
                if(new_val){        // if checked
                    sc.isActive = true;
                    sc.hasHeightConstraint = true;
                    try{
                        sc.heightConstraint = Double.parseDouble(heightConstraint.getText());
                    } catch(NumberFormatException e){
                        heightConstraint.setText("0.0");
                        sc.heightConstraint = 0.0;
                    }
                } else{             // if unchecked
                    sc.hasHeightConstraint = false;
                    heightConstraint.clear();
                    sc.verifyIfActive();
                }
            }
        });
        heightConstraint.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(heightConstraint.getText().equals("")){
                    sc.hasSpanConstraint = false;
                    heightCheck.setSelected(false);
                } else{
                    heightCheck.setSelected(true);
                    sc.isActive = true;
                    sc.heightConstraint = Double.parseDouble(heightConstraint.getText());
                    sc.hasHeightConstraint = true;
                    fullSearch(allPackages, searchField.getText(), results, sc);
                    selectAll.setSelected(false);
                }
            }
        });

        final CheckBox termCheck = new CheckBox("terminals");
        GridPane.setConstraints(termCheck, 0, termRow);
        final ComboBox termConstraint = new ComboBox();
        termConstraint.getItems().addAll(Package.termTypeValues());
        GridPane.setConstraints(termConstraint, 1, termRow);
        termCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov,Boolean old_val, Boolean new_val) {
                if(new_val){        // if checked
                    sc.isActive = true;
                    sc.hasTermConstraint = true;
                } else{             // if unchecked
                    sc.hasTermConstraint = false;
                    termConstraint.valueProperty().set(null);
                    sc.verifyIfActive();
                }
            }
        });
        termConstraint.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if(termConstraint.getValue() != null){
                termCheck.setSelected(true);
                sc.isActive = true;
                sc.hasTermConstraint = true;
                sc.termConstraint = Package.termTypefromString((String)termConstraint.getValue());
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });


        /* selectionTable displays selected SearchResults, which can be viewed separately from other Packages. TODO: make selection exportable */
        final TableView<SearchResult> selectionTable = new TableView<>();
        selectionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        GridPane.setConstraints(selectionTable, 0, selTableRow);
        GridPane.setColumnSpan(selectionTable, 2);
        selectionTable.setRowFactory(tv -> {
            TableRow<SearchResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    selectedPackages.clear();
                    for(SearchResult sr : selection){
                        selectedPackages.add(allPackages.get(sr.getOrgIndex()));
                    }
                    SearchResult res = row.getItem();
                    int newIndex = selection.indexOf(res);
                    setPackageSelection(true, newIndex);
                    prevWidth = stage.stage.getWidth();
                    prevHeight = stage.stage.getHeight();
                    stage.stage.setScene(mainScene);
                    stage.stage.setHeight(prevHeight);
                    stage.stage.setWidth(prevWidth);
                }
            });
            return row ;
        });

        TableColumn selectedCol = new TableColumn("selection");
        selectedCol.setMinWidth(200);
        selectedCol.setCellValueFactory(new PropertyValueFactory<>("match"));

        selectionTable.setItems(selection);
        selectionTable.getColumns().addAll(selectedCol);

        final HBox selControlBox = new HBox(3);
        GridPane.setConstraints(selControlBox, 0, selContRow);
        GridPane.setColumnSpan(selControlBox, 2);
        final Label selectInstructions = new Label("Double-click to view selection");

        Button selWipe = new Button("clear selection");
        selWipe.setOnAction((ActionEvent arg0) -> {
            selection.clear();
            for (SearchResult sr : results) {
                sr.setSelected(false);
            }
            setPackageSelection(false, allPackages.indexOf(crntPkg));   /* preset, may be overruled when a new selection is made */
        });
        selWipe.setAlignment(Pos.CENTER_RIGHT);

        selControlBox.getChildren().addAll(selectInstructions, selWipe);


        searchGrid.getChildren().addAll(
                advancedSearch,
                pinCountCheck, pinConstraint,
                pitchCheck, pitchConstraint,
                spanCheck, spanConstraint,
                heightCheck, heightConstraint,
                termCheck, termConstraint,
                selectionTable,
                selControlBox
        );

        midBox.getChildren().addAll(searchTable, createHSpacer(), searchGrid, createHSpacer());


        final Label instruction = new Label("Double-click to view package");
        botBox.getChildren().addAll(instruction);

        searchBranch.setPadding(new Insets(fieldSpacing, fieldSpacing, 0, fieldSpacing));
        searchBranch.getChildren().addAll(topBox, midBox, botBox);
        root.setContent(searchBranch);
        Scene scene = new Scene(root, STARTING_WINDOW_WIDTH, STARTING_WINDOW_HEIGHT); //node, width, minHeight
        return scene;
    }

    /* setPackageSelection() switches between the filtered list and the full
     * list of packages.
     */
    private void setPackageSelection(boolean filtered, int newIndex){
        if(filtered){
            assert (selectedPackages.size() > 0);
            viewedPackages = selectedPackages;
        } else {
            viewedPackages = allPackages;
            /* clear selection in searchScene */
            selection.clear();
        }
        newPack.setDisable(filtered);
        backToFull.setVisible(filtered);
        viewingSelection = filtered;

        navigate(newIndex);
    }

    /* fullSearch fills the supplied list with SearchResults while looping through AllPackages.  */
    private void fullSearch(ArrayList<Package> searchList, String s, ObservableList<SearchResult> resultList, SearchConstraint sc){
        resultList.clear();
        SearchResult res;
        boolean gotOne;         //flag to ensure a maximum of one SearchResult per Package.
        for(Package p : searchList){
            gotOne = false;
            int matchedIndex = sc.exactMatch ? arrayMatchesAt(p.names, s) : arrayContainsAt(p.names, s); //search names
            if(matchedIndex >= 0){
                res = new SearchResult(searchList.indexOf(p));
                res.match = new SimpleStringProperty(p.names[matchedIndex]);
                resultList.add(res);
                gotOne = true;
            }
            if(sc.searchDescription &&
               (sc.exactMatch ? p.description.toLowerCase().equals(s.toLowerCase()) : p.description.toLowerCase().contains(s.toLowerCase()))){ //search description
                res = new SearchResult(searchList.indexOf(p));
                res.match = new SimpleStringProperty(p.description);
                if(!gotOne){
                    resultList.add(res);
                    gotOne = true;
                }
            }
            if(p.specPacks != null){                            //search SpecificPackages/Variants
                for(Package.SpecificPackage sp: p.specPacks){
                    if(sc.exactMatch ? sp.variantName.equalsIgnoreCase(s) : sp.variantName.toLowerCase().contains(s.toLowerCase())){
                        res = new SearchResult(searchList.indexOf(p));
                        matchedIndex = searchList.indexOf(p);
                        res.match = new SimpleStringProperty(sp.variantName);
                        if(!gotOne){
                            resultList.add(res);
                        gotOne = true;
                        }
                    }
                    if(sc.searchDescription &&
                       (sc.exactMatch ? sp.variantNotes.equalsIgnoreCase(s) : sp.variantNotes.toLowerCase().contains(s.toLowerCase()))){
                        res = new SearchResult(searchList.indexOf(p));
                        matchedIndex = searchList.indexOf(p);
                        res.match = new SimpleStringProperty(sp.variantNotes);
                        if(!gotOne){
                            resultList.add(res);
                        }
                    }
                }
            }
            if(matchedIndex == -1){
                //TODO: display a "no match found" message on the gui\
                //just make another popup
            }
        }
        if(sc.isActive){        //if searchresults do not satisfy constraints, remove them from list
            ArrayList<SearchResult> toRemove = new ArrayList();

            for(SearchResult sr: resultList){
                gotOne = false;
                if(sc.hasPinConstraint){
                    if(!(sc.pinConstraint == searchList.get(sr.getOrgIndex()).nrOfPins)){
                        toRemove.add(sr);
                        gotOne = true;
                    }
                }
                if(sc.hasPitchConstraint && (!gotOne)){
                    if(!(roughCompare(sc.pitchConstraint, searchList.get(sr.getOrgIndex()).pitch))){
                        toRemove.add(sr);
                        gotOne = true;
                    }
                }
                if(sc.hasSpanConstraint && (!gotOne)){
                    if(!(roughCompare(sc.spanConstraint, searchList.get(sr.getOrgIndex()).footPrints[firstid].span.x, 0.1))){
                        toRemove.add(sr);
                        gotOne = true;
                        //TODO: Also compare to span-Y
                    }
                }
                if(sc.hasHeightConstraint && (!gotOne)){
                    if(!checkHeight(sr, sc.heightConstraint)){
                        toRemove.add(sr);
                        gotOne = true;
                    }
                }
                if(sc.hasTermConstraint && (!gotOne)){
                    if(!(sc.termConstraint == searchList.get(sr.getOrgIndex()).termination)){
                        toRemove.add(sr);
                        //gotOne = true;  //not currently needed but may as well leave it the line here in case another constraint is ever added
                    }
                }
            }
            resultList.removeAll(toRemove);
        }
    }

    /*returns false if the height constraint does not fall within the boundries of any of the variants being searched */
    private boolean checkHeight(SearchResult sr, double hConstraint){
        for(Package.SpecificPackage spep: viewedPackages.get(sr.getOrgIndex()).specPacks){
            if(hConstraint <= spep.heightRange.maxHeight && hConstraint >= spep.heightRange.minHeight){
                return true;
            }
        }
        return false;
    }

    private int arrayContainsAt(String[] array, String search){
        for(int i = 0; i < array.length; i++){
            if (array[i].toLowerCase().contains(search.toLowerCase())){
                return i; //'true for'
            }
        }
        return -1; //'false'
    }
    private int arrayMatchesAt(String[] array, String search){
        /* returns the array index of the first match, or -1 for no (further) matches */
        for(int i = 0; i < array.length; i++){
            if (array[i].toLowerCase().equals(search.toLowerCase())){
                return i; //'true for'
            }
        }
        return -1; //'false'
    }

    private boolean notDuplicate(String s){
        if(s.equals("")){
            return true;
        }
        ObservableList<SearchResult> otherSearchList = FXCollections.observableArrayList();
        otherSearchList.clear();
        SearchConstraint sc = new SearchConstraint();
        sc.exactMatch = true;
        sc.searchDescription = false;
        fullSearch(allPackages, s, otherSearchList, sc);

        if(!otherSearchList.isEmpty()){
            dupWarning.res = otherSearchList.get(0); //just get the first item from the list (if the file is valid, there can at most be a single conflict: the one just entered)
            return false;
        }
        return true;
    }

    /* "...close enough..." */
    private static boolean roughCompare(double d1, double d2){
        double margin = 0.0005;             //standard acceptable margin set at 0.05%
        return roughCompare(d1, d2, margin);
    }
    private static boolean roughCompare(double d1, double d2, double margin){
        double lowerbound;
        double upperbound;
        if(d1 > 0.0){
            lowerbound = d1 * (1.0 - margin);
            upperbound = d1 * (1.0 + margin);
        } else if(d1 < 0.0){
            lowerbound = d1 * (1.0 + margin);
            upperbound = d1 * (1.0 - margin);
        } else {
            /* special case for comparing 0 to 0 */
            assert (d1 == 0.0);
            lowerbound = 0 - margin;
            upperbound = margin;
        }
        assert (lowerbound < upperbound);
        boolean equal = d2 > lowerbound && d2 < upperbound;
        return equal;
    }

    /* returns true if  list 'selection' already has a SearchResult pointing at the same Package as the candidate does */
    private boolean alreadySelected(SearchResult candidate){
        for(SearchResult sr : selection){
            if(candidate.getOrgIndex() == sr.getOrgIndex()){
                return true;
            }
        }
        return false;
    }

    /* This method is called when a Package is deleted while the list with SearchResults is filled.
     * The SearchResult pointing at that Package is deleted, and SearchResults pointing at a package
     * with a higher index than the deleted one have their 'originIndex' reduced by 1 */
    private void rectifySearchResults(int deletedIndex){
        SearchResult toDelete = null;
        for(SearchResult res: results){
            if(res.getOrgIndex() == deletedIndex){
                toDelete = res;
            }
        }
        if(toDelete != null){
            results.remove(toDelete);
        }

        for(SearchResult res: results){
            res.rectifyOrgIndex(deletedIndex);
        }
    }

    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Test functionality methods !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    //Here I define the testing scene in the development branch


    /* 8 */
    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! All support classes below !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */

    /* Config stores data that is read when the program starts*/
    private static class Config implements Jsonable{
        static boolean pathSet;
        static String path;
        static int width, height;

        static void setPath(String path){
            Config.path = path;
            pathSet = true;
        }

        static void setWindowSize(int w, int h){
            width = w;
            height = h;
        }

        /*implementation for interface Jsonable of Package*/
        @Override
        public String toJson() {
            final StringWriter writable = new StringWriter();
            try {
                this.toJson(writable);
            } catch (final IOException e) {
            }
        return writable.toString();
    }

        @Override
        public void toJson(Writer writer) throws IOException {
            final JsonObject json = new JsonObject();
            json.put("pathSet", pathSet);
            json.put("path", path);
            json.put("window-width", width);
            json.put("window-height", height);

            json.toJson(writer);
        }

        Config(boolean pathSet, String path){
            Config.pathSet = pathSet;
            Config.path = path;
            Config.width = 0;   /* zero means to use a default width/height */
            Config.height = 0;
        }
    }

    /* A dynamic textfield manager is a VBox that containts a dynamic number of HBoxes that each contain textfields. */
    private class dtfManager extends VBox{
        final int FIELDS_PER_ROW = 5;
        Stack<Row> rowBin;
        Row supplyRow;
        Stack<Row.DTF> dtfSupply;

        /* add all names from current Package plus an empty field */
        public void load(){
            this.getChildren().clear();
            rowBin.clear();
            dtfSupply.clear();
            supplyRow.fillSupply(50); //Max number of TextFields that I think will be in use at any give time.
            for(String s: crntPkg.names){
                if(s.length() > 0){
                    baseAdd(s);
                }
            }
            baseAdd("");
        }

        private void baseAdd(String s){
            if(this.getChildren().isEmpty()){
                addRow(); //add a new row if there are none
            }
            Row lastRow = (Row) this.getChildren().get(this.getChildren().size()-1); //access last row
            if(lastRow.isFull()){
                addRow();
            }
            lastRow = (Row) this.getChildren().get(this.getChildren().size()-1); //update last row
            lastRow.add(s);
        }

        /* If there are Rows in the bin, take one from there, otherwise make a new one */
        private void addRow(){
            if(!rowBin.empty()){
                this.getChildren().add(rowBin.pop());
            } else{
                this.getChildren().add(new Row(3));
            }
        }

        /*Take the first entry from the next row and put it in the current one, then repeat for next row if there is one */
        private void takeFromNext(Row crntRow){
            int nextIndex = (this.getChildren().indexOf(crntRow) + 1);
            Row nextRow = (Row) this.getChildren().get(nextIndex);
            Row.DTF shiftIt = (Row.DTF) nextRow.getChildren().get(0);
            nextRow.getChildren().remove(shiftIt);
            crntRow.add(shiftIt.getText());
            if(nextRow.isEmpty()){
                rowBin.push(nextRow);
                this.getChildren().remove(nextRow);
            } else if(!nextRow.lastRow(nextRow)){
                takeFromNext(nextRow);
            }
        }

        private Row dtfBelongsTo(Row.DTF d){
            for(Node n: this.getChildren()){
                Row r = (Row) n;
                if(r.getChildren().contains(d)){
                    return r;
                }
            }
            return null;
        }

        public void store(){
            ArrayList<String> tmpNames = new ArrayList();
            for(Node n : this.getChildren()){
                Row r = (Row) n;
                for(Node n2 : r.getChildren()){
                    Row.DTF d = (Row.DTF) n2;
                    if(!d.getText().equalsIgnoreCase("")){
                        tmpNames.add(d.getText());
                    }
                }
            }
            String[] tmpNameArray = tmpNames.toArray(new String[tmpNames.size()]);
            crntPkg.names = tmpNameArray; //replace Package.names
        }

        private class Row extends HBox{
            private String identifier;
            private void add(String s){
                if(!this.isFull()){
                    DTF dtf = dtfSupply.pop();
                    dtf.setText(s);
                    dtf.check = s;
                    dtf.setPromptText("name");
                    this.getChildren().add(dtf);
                } else{
                    dtfManager.this.baseAdd(s);
                }

            }

            private void remove(DTF dtf){
                if(lastRow(this)){
                    this.getChildren().remove(dtf); //if it's in the last row just remove it
                } else{
                    this.getChildren().remove(dtf);
                    takeFromNext(this);             //if it's not the last row than textfields will need to be shifted back
                }
            }

            private boolean lastRow(Row r){
                dtfManager dtf = dtfManager.this;
                int index = dtf.getChildren().indexOf(r);
                int maxIndex = dtf.getChildren().size() - 1;
                return index == maxIndex;
            }

            private boolean isFull(){
                boolean full = this.getChildren().size() == FIELDS_PER_ROW;
                return(full);
            }
            private boolean isEmpty(){
                return(this.getChildren().isEmpty());
            }

            private void fillSupply(int supply){
                for(int i = 0; i < supply; i++){
                    dtfSupply.push(new DTF(""));
                }
            }

            private class DTF extends TextField{
                String check;

                private boolean lastInRow(){
                    Row r = dtfBelongsTo(this);
                    int index = r.getChildren().indexOf(this);
                    int maxIndex = r.getChildren().size() - 1;
                    return index == maxIndex;
                }

                private boolean finalOne(){
                    Row r = dtfBelongsTo(this);
                    return this.lastInRow() && r.lastRow(r);
                }

                private void handle(String s){
                    if(!s.equals(check)){
                        if(notDuplicate(s)){
                            if(s.equals("")){
                                if(this.finalOne()){
                                    //do nothing
                                } else{
                                    // delete entry & shift remaining entries back to fall into place
                                    dtfBelongsTo(this).remove(this);
                                    check = s;
                                    store();
                                }
                            } else{
                                if(this.finalOne()){
                                    baseAdd("");
                                    check = s;
                                    store();
                                } else{
                                    check = s;
                                    store();
                                }
                            }
                        } else{
                            this.clear();
                            dupWarning.show(stage.stage);
                        }
                    }
                }

                DTF(String s){
                    super(s);
                    check = s;

                    this.setOnKeyPressed( event -> {
                        if( event.getCode() == KeyCode.ENTER ) {
                            handle(this.getText());
                        }
                    });
                    this.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                        if (!newPropertyValue){
                            handle(this.getText());
                        }
                    });
                }

                @Override
                public boolean equals(Object o) {

                    // If the object is compared with itself then return true
                    if (o == this) {
                        return true;
                    }

                    /* Check if o is an instance of Complex or not "null instanceof [type]" also returns false */
                    if (!(o instanceof DTF)) {
                        return false;
                    }

                    // typecast o to Complex so that we can compare data members
                    DTF d = (DTF) o;

                    // Compare the data members and return accordingly
                    boolean eq = false;
                    if(this.getText().equals(d.getText())){
                        eq = true;
                    }
                    return eq;
                }

            }
            Row(double spacing){
                super(spacing);
                identifier = Objects.toString(this); //hax
            }

            @Override
            public boolean equals(Object o) {
                // If the object is compared with itself then return true
                if (o == this) {
                    return true;
                }

                /* Check if o is an instance of Row or not "null instanceof [type]" also returns false */
                if (!(o instanceof Row)) {
                    //dunno why this method gets called on stuff that isn't a row, it shouldnt but w/e
                    return false;
                }

                // typecast o to Complex so that we can compare data members
                boolean eq = false;
                Row r = (Row) o;
                if(this.identifier.equals(r.identifier)){
                    eq = true;
                }
                return eq;
            }
        }
        dtfManager(double spacing){
            super(spacing);
            rowBin = new Stack();
            supplyRow = new Row(3);
            dtfSupply = new Stack();
        }
    }


    /* Mirror class for SpecificPackage/Variant */
    public class SpepaMirror{
        String spepaName;
        SimpleStringProperty spepaNameppt;
        public SimpleStringProperty spepaNamepptProperty(){
            return spepaNameppt;
        }
        public void setIpcName(String name){
            spepaName = name;
            spepaNameppt.set(name);
        }

        Package.NameStandard standard;
        SimpleStringProperty standardppt;
        public SimpleStringProperty standardpptProperty(){
            return standardppt;
        }
        public void setStandard(String s){
            this.standard = Package.nameStandardFromString(s);
            setStandardppt(s);
        }
        public void setStandardppt(String s){
            standardppt.set(s);
        }
        public String getStandardppt(){
            return standardppt.get();
        }

        double minHeight;
        SimpleDoubleProperty minHeightppt;
        public SimpleDoubleProperty minHeightpptProperty(){
            return minHeightppt;
        }
        public void setMinHeight(double hmin){
            minHeight = hmin;
            minHeightppt.set(hmin);
        }

        double maxHeight;
        SimpleDoubleProperty maxHeightppt;
        public SimpleDoubleProperty maxHeightpptProperty(){
            return maxHeightppt;
        }
        public void setMaxHeight(double hmax){
            maxHeight = hmax;
            maxHeightppt.set(hmax);
        }

        boolean padExposed;
        SimpleBooleanProperty padExposedppt;
        public SimpleBooleanProperty padExposedpptProperty(){
            return padExposedppt;
        }
        public void setPadExposed(boolean e){
            padExposed = e;
            padExposedppt.set(e);
        }

        String spepaNotes;
        SimpleStringProperty spepaNotesppt;
        public SimpleStringProperty spepaNotespptProperty(){
            return spepaNotesppt;
        }
        public void setNotes(String note){
            spepaNotes = note;
            spepaNotesppt.set(note);
        }

        /* Constructors */
        SpepaMirror(String ipcName, Package.NameStandard standard, double height, double hTol, boolean padExposed, String notes){
            this.spepaName = ipcName;
            spepaNameppt = new SimpleStringProperty(ipcName);

            this.standard = standard;
            standardppt = new SimpleStringProperty(Package.nameStandardAsString(standard));

            this.spepaNotes = notes;
            spepaNotesppt = new SimpleStringProperty(notes);

            this.minHeight = height;
            minHeightppt = new SimpleDoubleProperty(height);

            this.maxHeight = hTol;
            maxHeightppt = new SimpleDoubleProperty(hTol);

            this.padExposed = padExposed;
            padExposedppt = new SimpleBooleanProperty(padExposed);
        }
        SpepaMirror(Package.SpecificPackage spepa){
            this(spepa.variantName,
                 spepa.standard,
                 spepa.heightRange.minHeight,
                 spepa.heightRange.maxHeight,
                 spepa.centerPadExposed,
                 spepa.variantNotes);
        }
    }


    public class PadDimmirror{
        int padId;
        SimpleIntegerProperty padIdppt;
        public SimpleIntegerProperty padIdpptProperty(){
            return padIdppt;
        }
        public void setPadId(int i){
            padId = i;
            setPadIdppt(i);
        }
        public void setPadIdppt(int padId){
            padIdppt.set(padId);
        }
        public int getPadIdppt(){
            return padIdppt.get();
        }

        double length;
        SimpleDoubleProperty lengthppt;
        public SimpleDoubleProperty lengthpptProperty(){
            return lengthppt;
        }
        public void setLength(double le){
            length = le;
            setLengthppt(le);
        }
        public void setLengthppt(double length){
            lengthppt.set(length);
        }
        public double getLengthppt(){
            return lengthppt.get();
        }

        double width;
        SimpleDoubleProperty widthppt;
        public SimpleDoubleProperty widthpptProperty(){
            return widthppt;
        }
        public void setWidth(double w){
            width = w;
            setWidthppt(w);
        }
        public void setWidthppt(double width){
            widthppt.set(width);
        }
        public double getWidthppt(){
            return widthppt.get();
        }

        Package.PadShape shape;
        SimpleStringProperty shapeppt;
        public SimpleStringProperty shapepptProperty(){
            return shapeppt;
        }
        public void setShape(String shapestr){
            this.shape = Package.padShapefromString(shapestr);
            setShapeppt(shapestr);
        }
        public void setShapeppt(String shapestr){
            shapeppt.set(shapestr);
        }
        public String getShapeppt(){
            return shapeppt.get();
        }

        double holeDiam;
        SimpleDoubleProperty holeDiamppt;
        public SimpleDoubleProperty holeDiampptProperty(){
            return holeDiamppt;
        }
        public void setHoleDiam(double h){
            holeDiam = h;
            setHoleDiamppt(h);
        }
        public void setHoleDiamppt(double holeDiam){
            holeDiamppt.set(holeDiam);
        }
        public double getHoleDiamppt(){
            return holeDiamppt.get();
        }

        double originX;
        SimpleDoubleProperty originXppt;
        public SimpleDoubleProperty originXpptProperty(){
            return originXppt;
        }
        public void setOriginX(double ox){
            originX = ox;
            setOriginXppt(ox);
        }
        public void setOriginXppt(double originX){
            originXppt.set(originX);
        }
        public double getOriginX(){
            return originXppt.get();
        }

        double originY;
        SimpleDoubleProperty originYppt;
        public SimpleDoubleProperty originYpptProperty(){
            return originYppt;
        }
        public void setOriginY(double oy){
            originY = oy;
            setOriginYppt(oy);
        }
        public void setOriginYppt(double originY){
            originYppt.set(originY);
        }
        public double getOriginY(){
            return originYppt.get();
        }

        boolean padExposed;
        SimpleBooleanProperty padExposedppt;
        public SimpleBooleanProperty padExposedpptProperty(){
            return padExposedppt;
        }
        public void setPadExposed(boolean b){
            this.padExposed = b;
            setPadExposedppt(b);
        }
        public void setPadExposedppt(boolean b){
            padExposedppt.set(b);
        }
        public boolean getPadExposed(){
            return padExposedppt.get();
        }

        PadDimmirror(int padId, double width, double length, Package.PadShape shape, double holeDiam, double originX, double originY, boolean padExposed){
            this.padId = padId;
            padIdppt = new SimpleIntegerProperty(padId);

            this.length = length;
            lengthppt = new SimpleDoubleProperty(length);

            this.width = width;
            widthppt = new SimpleDoubleProperty(width);

            this.shape = shape;
            shapeppt = new SimpleStringProperty(Package.padShapeasString(shape));

            this.holeDiam = holeDiam;
            holeDiamppt = new SimpleDoubleProperty(holeDiam);

            this.originX = originX;
            originXppt = new SimpleDoubleProperty(originX);

            this.originY = originY;
            originYppt = new SimpleDoubleProperty(originY);

            this.padExposed = padExposed;
            padExposedppt = new SimpleBooleanProperty(padExposed);
        }
        PadDimmirror(Package.Footprint.PadDimension dim){
            this(dim.padId,
                 dim.width,
                 dim.length,
                 dim.shape,
                 dim.holeDiam,
                 dim.originX,
                 dim.originY,
                 dim.padExposed);
        }
    }


    public class PadPosmirror{
        String pinId;
        SimpleStringProperty pinIdppt;
        public SimpleStringProperty pinIdpptProperty(){
            return pinIdppt;
        }
        public void setPinId(String pin){
            pinId = pin;
            setPinIdppt(pin);
        }
        public void setPinIdppt(String pinId){
            pinIdppt.set(pinId);
        }
        public String getPinIdppt(){
            return pinIdppt.get();
        }

        double xPos;
        SimpleDoubleProperty xPosppt;
        public SimpleDoubleProperty xPospptProperty(){
            return xPosppt;
        }
        public void setXPos(double x){
            xPos = x;
            setXPosppt(x);
        }
        public void setXPosppt(double xPos){
            xPosppt.set(xPos);
        }
        public double getXPosppt(){
            return xPosppt.get();
        }

        double yPos;
        SimpleDoubleProperty yPosppt;
        public SimpleDoubleProperty yPospptProperty(){
            return yPosppt;
        }
        public void setYPos(double y){
            yPos = y;
            setYPosppt(y);
        }
        public void setYPosppt(double yPosstr){
            yPosppt.set(yPosstr);
        }
        public double getYPosppt(){
            return yPosppt.get();
        }

        int padId;
        SimpleIntegerProperty padIdppt;
        public SimpleIntegerProperty padIdpptProperty(){
            return padIdppt;
        }
        public void setPadId(int pad){
            padId = pad;
            setPadIdppt(pad);
        }
        public void setPadIdppt(int padIdstr){
            padIdppt.set(padIdstr);
        }
        public int getPadIdppt(){
            return padIdppt.get();
        }

        Package.Orientation rotation;
        SimpleIntegerProperty rotationppt;
        public SimpleIntegerProperty rotationpptProperty(){
            return rotationppt;
        }
        public void setRotation(String rotStr){
            this.rotation = Package.orientationFromString(rotStr);
            setRotationppt(Package.orientationAsInt(rotation));
        }
        public void setRotationppt(int rotInt){
            rotationppt.set(rotInt);
        }
        public int getRotationppt(){
            return rotationppt.get();
        }

        PadPosmirror(String pinId, double xPos, double yPos, int padId, Package.Orientation rotation){
            this.pinId = pinId;
            pinIdppt = new SimpleStringProperty(pinId);

            this.xPos = xPos;
            xPosppt = new SimpleDoubleProperty(xPos);

            this.yPos = yPos;
            yPosppt = new SimpleDoubleProperty(yPos);

            this.padId = padId;
            padIdppt = new SimpleIntegerProperty(padId);

            this.rotation = rotation;
            rotationppt = new SimpleIntegerProperty(Package.orientationAsInt(rotation));
        }
        PadPosmirror(Package.Footprint.PadPosition pos){
            this(pos.pinId,
                 pos.xPos,
                 pos.yPos,
                 pos.padId,
                 pos.rotation);
        }
    }


    /* SearchResults are added to an observable list and displayed in a TableView while searching.
     * They contain an integer that points at the location of the Package in which a match was found,
     * and a StringProperty containing the string that was matched. Also a BooleanProperty for
     * controlling them being added to or removed from 'selection' */
    public class SearchResult{
        private int orgIndex;
        public int getOrgIndex(){
            return orgIndex;
        }

        public SimpleStringProperty match;
        public SimpleStringProperty matchProperty(){
            return match;
        }
        public void setMatch(String s){
            match.set(s);
        }
        public String getMatch(){
            return match.get();
        }

        public SimpleBooleanProperty selected;
        public SimpleBooleanProperty selectedProperty(){
            return selected;
        }
        public void setSelected(boolean s){
            selected.set(s);
        }
        public boolean getSelected(){
            return selected.get();
        }

        /* This is to ensure a SearchResult does not point at the wrong Package after one is deleted */
        public void rectifyOrgIndex(int deletedIndex){
            if(orgIndex > deletedIndex){
                orgIndex--;
            }
        }

        /*constructor*/
        SearchResult(int orgIndex){
            this.orgIndex = orgIndex;
            this.match = new SimpleStringProperty("");
            this.selected = new SimpleBooleanProperty(false);
            this.selected.addListener((ObservableValue<? extends Boolean> obs, Boolean wasSelected, Boolean isSelected) -> {
                /* Add it to the list if there isn't anything already on the list pointing at same Package. Otherwise, undo selection */
                if(isSelected){
                    if(!alreadySelected(this)){
                        selection.add(this);
                    } else{
                        //this.setSelected(false);
                    }
                } else{
                    if(selection.contains(this)){
                        selection.remove(this);
                    }
                }
            });
        }
    }

    /* SearchConstraints is a wrapper that contains and validates input to be used as arguments for a search*/
    private class SearchConstraint{
        boolean isActive;
        boolean hasPinConstraint;
        int pinConstraint;
        boolean hasPitchConstraint;
        double pitchConstraint;
        boolean hasSpanConstraint;
        double spanConstraint;
        boolean hasHeightConstraint;
        double heightConstraint;
        boolean hasTermConstraint;
        Package.TermType termConstraint;
        boolean exactMatch;
        boolean searchDescription;

        void clear(){
            isActive = false;
            hasPinConstraint = false;
            pinConstraint = 0;
            hasPitchConstraint = false;
            pitchConstraint = 0.0;
            hasSpanConstraint = false;
            spanConstraint = 0.0;
            hasHeightConstraint = false;
            hasTermConstraint = false;
            heightConstraint = 0.0;
        }

        void verifyIfActive(){
            if(!(hasPinConstraint || hasPitchConstraint || hasSpanConstraint || hasHeightConstraint || hasTermConstraint)){
                isActive = false;
            }
        }

        SearchConstraint(){
            isActive = false;
            hasPinConstraint = false;
            hasPitchConstraint = false;
            hasSpanConstraint = false;
            hasHeightConstraint = false;
            hasTermConstraint = false;
            exactMatch = false;
            searchDescription = true;
        }
    }

    /* A Key is required to get data from JsonObjects */
    private class SimpleKey implements JsonKey{
        protected Object value; //value gets used when no data matches the key
        protected String key; //key is compared to the name of a field in a JsonObject

        @Override
        public Object getValue(){
            return value;
        }
        @Override
        public String getKey(){
            return key;
        }

        SimpleKey(){

        }
        SimpleKey(String key){
            this.key = key;
        }
    }
    private class IntegerKey extends SimpleKey{
        IntegerKey(String key){
            super(key);
            this.value =  0;
        }
    }
    private class DoubleKey extends SimpleKey{
        DoubleKey(String key){
            super(key);
            this.value = 0.0;
        }
    }
    private class StringKey extends SimpleKey{
        StringKey(String key){
            super(key);
            this.value = "";
        }
    }
    private class BooleanKey extends SimpleKey{
        BooleanKey(String key){
            super(key);
            this.value = false;
        }
    }

    /* StageHolder is a wrapper for the Stage that Start() receives as an argument from somewhere(?)
     * Doing this makes it so I can swap Scenes from anywhere without passing along the stage */
    private class StageHolder{
        Stage stage;

        StageHolder(Stage stage){
            this.stage = stage;
        }
    }

    private class BasicPopup extends Popup{
        VBox branch;
        Label messageLbl;
        Button close;

        BasicPopup(String style, String message, String btnText){
            super();
            branch = new VBox(5);
            branch.setAlignment(Pos.CENTER);
            branch.setStyle(style);
            messageLbl = new Label(message);
            messageLbl.setPadding(new Insets(0, 0, fieldSpacing, 0)); // add spacing below text (above button)
            close = new Button(btnText);
            close.setOnAction((ActionEvent arg0) -> {
                this.hide();
            });
            branch.getChildren().addAll(messageLbl, close);
            this.getContent().add(branch);
            this.setAutoHide(true);
        }
    }

    /* This is a Popup that contains a little bit of extra data, in the form of a single SearchResult*/
    private class DuplicateWarning extends Popup{
        SearchResult res;

        public int index(){
            return res.getOrgIndex();
        }

        DuplicateWarning(){
            super();
        }
    }

    private class AboutPopup extends Popup{
        VBox branch;
        Label Caption;
        Label Copyright;
        Label SystemInfo;
        Label FileInfo;
        Button close;

        AboutPopup(String style){
            super();
            branch = new VBox(5);
            branch.setAlignment(Pos.CENTER_LEFT);
            branch.setStyle(style);

            Caption = new Label("PACKAGES " + programVersion);
            Caption.setStyle("-fx-font-size:14; -fx-font-weight:bold");
            Copyright = new Label("Copyright 2021, 2022 CompuPhase\n" +
                                  "Developed by Guido Wolff & Thiadmer Riemersma");

            String version = System.getProperty("java.version");
            String fxversion = System.getProperty("javafx.version");
            String jre_path = System.getProperty("java.home");
            String sysmsg = "JDK:\t\t" + version + " (" + jre_path + ")\n"
                            + "JavaFx:\t" + fxversion;
            SystemInfo = new Label(sysmsg);

            FileInfo = new Label("Data file:\t(none)");
            FileInfo.setPadding(new Insets(0, 0, fieldSpacing, 0)); // add spacing below text (above button)

            final HBox buttonBox = new HBox(3);
            buttonBox.setAlignment(Pos.CENTER);
            close = new Button("Close");
            close.setAlignment(Pos.CENTER);
            close.setOnAction((ActionEvent arg0) -> {
                this.hide();
            });
            buttonBox.getChildren().add(close);

            branch.getChildren().addAll(Caption, Copyright, SystemInfo, FileInfo, buttonBox);
            this.getContent().add(branch);
            this.setAutoHide(true);
        }

        void Update(){
            /* update statistics on the JSON file */
            String pkginfo;
            if(fileIsSet){
                pkginfo = "Data file:\t" + loadedFile.getPath() + "\n";
                int count_packages = allPackages.size();
                int count_names = 0, count_variants = 0;
                for(Package p : allPackages){
                    if(p.names != null){
                        count_names += p.names.length;
                    }
                    if(p.specPacks != null){
                        count_variants += p.specPacks.length;
                    }
                }
                pkginfo += String.format("\t\t%d packages, %d names, %d variants",
                        count_packages, count_names, count_variants);
            } else {
                pkginfo = "Data file:\t(none)";
            }
            FileInfo.setText(pkginfo);
        }
    }

    private class Courtyard{
        Polygon boundbox;

        Courtyard(){
            boundbox = new Polygon();
        }

        public void AddBoundingBox(double cx, double cy, double orgx, double orgy){
            /* don't do anything when the bounding box has zero width or length */
            if(!roughCompare(cx, 0.0) && !roughCompare(cy, 0.0)){
                Polygon box = Polygon.FromRect(cx, cy);
                box.Move(-orgx, -orgy);
                boundbox.AppendPolygon(box);
            }
        }

        public double Left(){
            return boundbox.Left();
        }
        public double Right(){
            return boundbox.Right();
        }
        public double Bottom(){
            return boundbox.Bottom();
        }
        public double Top(){
            return boundbox.Top();
        }
    }
}


/* digital scrap paper

java -jar AnotherGrep.jar      om een verpakt java preject te runnen via command line

voorbeeld
navigate to dist/packagemanager.jar . Expand the node for the JAR file, expand the META-INF folder, and double-click MANIFEST.MF to display the manifest in the Source Editor.

Main-Class: packagemanager.App

-------------

https://docs.oracle.com/javase/8/javafx/layout-tutorial/size_align.htm

https://stackoverflow.com/questions/33414194/fill-width-in-a-pane
----------------


Other info:
https://blog.idrsolutions.com/2012/11/adding-a-window-resize-listener-to-javafx-scene/
https://geektortoise.wordpress.com/2014/02/07/how-to-programmatically-resize-the-stage-in-a-javafx-app/




*/
