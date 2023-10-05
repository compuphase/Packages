/* Packages
 *
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
import javafx.application.Platform;

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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
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
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import com.github.cliftonlabs.json_simple.Jsoner;
import com.github.cliftonlabs.json_simple.Jsonable;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;



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
    final boolean RELOAD_CSS_BUTTON = false;// for testing CSS changes
    final int STARTING_WINDOW_WIDTH  = 900; // default "scene" width (excluding borders)
    final int STARTING_WINDOW_HEIGHT = 700; // default "scene" height (excluding caption and borders)
    double prevWidth;                       // current window width (including borders)
    double prevHeight;                      // current window height (including caption and borders)

    final String programVersion = "1.3.1";  //current version number

    final int IMAGE_WIDTH = 200;
    final int IMAGE_HEIGHT = 170;

    final int fieldSpacing = 8;             //spacing between items on a row
    final int SPACING_VBOX = 5;
    final int SPACING_HBOX = 3;
    final double intInputPrefWidth = 40;    //width for TextFields that will hold values with up to 3 digits
    final double dimInputPrefWidth = 55;    //width for TextFields that will hold numbers (dimensions etc.)
    final double tolInputPrefWidth = 45;    //width for TextFields that will hold tolerances of dimensions

    final double THINCOLUMN_MINWIDTH = 72;
    final double THINCOLUMN_PREFWIDTH = 90;
    final double THINCOLUMN_MAXWIDTH = 130;

    final static int IO_RETRIES = 10;   //the number of times that an IO process will try again after failing the first time
    static int ioTryCounter;            //not a constant but it's strongly connected to IO_RETRIES
    static Random ioRandomDelay = new Random(SystemInfo.machineId());   //for randomizing delays between read/write attempts

    /*functional fields; do not represent data to be stored*/
    static Config config;
    Stage stage;                                                          // Stage is declared globally for convenience
    Popup delWarning;
    Popup fpDelWarning;
    AboutPopup aboutPopup;
    Popup impSuccess;
    BasicPopup impFailed;
    ImportConflictPopup impConflicted;
    BasicPopup selectionCanceledWarning;
    BasicPopup incompleteDataWarning;
    BasicPopup packageNotFound;
    DuplicateWarning dupWarning;                                                // A inheritor of Popup that contains a SearchResult
    BasicPopup dupSoftWarning;
    PolygonBuilder polyBuilder;
    BasicPopup vertexIdWarning;
    ImagePopup previewPopup;
    static File configFile;
    static boolean configExists;
    boolean fileIsSet = false;
    File loadedFile;
    boolean viewingSelection;
    Canvas mainCanvas;
    GraphicsContext mainGc;
    long lastModified;
    boolean changesWereMade;

    String currentSceneName;    /* for context-sensitive help */
    WebView webView;

    Scene mainScene;
    Scene searchScene;
    Scene importScene;
    Scene helpScene;
    Scene currentScene;
    Scene previousScene;    /* to jump back to the previous scene when closing help */

    GridPane grid;

    CustomMenuItem exportItem;

    Button newPack;
    Button backToFull;

    /*field declarations, mirrors data contained in Package */
    ArrayList<Package> loadedPackages;
    ArrayList<Package> viewedPackages;
    ArrayList<Package> allPackages;
    ArrayList<Package> selectedPackages;

    Package crntPkg;
    Package backupPkg;
    int crntIndex;

    Label displayedIndex;

    Label nameLbl;

    dtfManager nameBox;

    ArrayList<TextField> nameFields;

    TextField descField;

    ComboBox charatypeBox;
    ComboBox terminationBox;
    CheckBox polarCheck;
    TextField pinNumber;
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

    int fpIndex;
    Label fpIndexLabel;
    ComboBox footprinttypeBox;
    TextField pitchField;
    TextField spanXField;
    TextField spanYField;
    TextField fpolLength;
    TextField fpolWidth;
    TextField fpolOrgX;
    TextField fpolOrgY;

    final ObservableList<PadDimMirror> pdDimensions = FXCollections.observableArrayList();
    TextField padIdInput;

    final ObservableList<PadPosMirror> pdPositions = FXCollections.observableArrayList();
    TextField pinIdInput;
    TextField pinPadIdInput;

    final ObservableList<ReferenceMirror> referenceList = FXCollections.observableArrayList();
    final ObservableList<RelatedPack> relatedList = FXCollections.observableArrayList();

    final ObservableList<ImportedPackage> cleanImportPacks = FXCollections.observableArrayList();
    final ObservableList<ImportedPackage> conflictedImportPacks = FXCollections.observableArrayList();
    ImportedPackage inspectedImport;
    Package inspectedPackage;

    final ObservableList<SearchResult> results = FXCollections.observableArrayList();
    final ObservableList<SearchResult> selection = FXCollections.observableArrayList();
    TextField searchField;

    /* 4 */
    @Override
    public void start(Stage primaryStage){
        initConfig();   //checks for the existence of a config folder, if it doesn't exist, create it and a config file in it

        /* code that runs on exit */
        primaryStage.setOnCloseRequest((WindowEvent e) -> {
            saveAndQuit();
        });

        /* Set global Stage to reference primaryStage */
        stage = primaryStage;

        /* Set application icon*/
        setIcon();

        /* init main list */
        allPackages = new ArrayList();
        allPackages.add(new Package());

        /* Load an empty Package */
        viewedPackages = allPackages;
        crntIndex = 0;
        crntPkg = viewedPackages.get(crntIndex);

        /* init backup package */
        backupPkg = new Package();

        loadedPackages = new ArrayList();
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
        //grid.setStyle("-fx-grid-lines-visible: true");  // to help visualize the layout when debugging

        initPopups();          //initializes all Popups and children of Popups

        /* First row of components controls navigation */
        final int navRow = 0;
        final HBox menuBox = initMenuBox();
        GridPane.setConstraints(menuBox, 0, navRow); //thing, column, row
        final HBox navBox = initNavBox();       // initialize subcomponents and add them as children
        GridPane.setConstraints(navBox, 1, navRow); //thing, column, row

        final HBox searchBox = new HBox(SPACING_HBOX);
        searchBox.setAlignment(Pos.CENTER_RIGHT);
        searchBox.setPadding(new Insets(3, 10, 0, 0));
        searchScene = initSearchScene();
        GridPane.setConstraints(searchBox, 2, navRow); //thing, column, row
        final Button searchButton = new Button("search");
        searchButton.setTooltip(new Tooltip("Ctrl+F"));
        searchButton.setOnAction((ActionEvent arg0) -> {
            changeScene(searchScene);
            //searchField.requestFocus(); //weird bug: this doesn't work the first time but does work any subsequent time
            Platform.runLater(()->searchField.requestFocus());
        });
        searchBox.getChildren().add(searchButton);
        if (RELOAD_CSS_BUTTON){
            final Button reloadCssButton = new Button("CSS");
            reloadCssButton.setOnAction((ActionEvent arg0) -> {
                mainScene.getStylesheets().clear();
                mainScene.getStylesheets().add("file://" + getResourcePath() + "/packages.css");
            });
            searchBox.getChildren().add(reloadCssButton);
        }
        /* Adding a shortcut, CTRL+F (mnemonic on a control only works with Alt,
         * so we use an accelerator); the accelerator is created here, but added
         * to the scene later (because the scene has not been initialized yet) */
        KeyCombination searchShort = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
        Runnable searchShortCut = () -> {
            changeScene(searchScene);
            //searchField.requestFocus(); //weird bug: this doesn't work the first time but does work any subsequent time
            Platform.runLater(()->searchField.requestFocus());
        };

        /* style for labels in the left column */
        final String styleCatLabel = "-fx-font-weight:bold; -fx-text-fill:#446677;";

        /* Second row of nodes. controls Package.names through a dynamic number of TextFields */
        final int nameRow = navRow + 1;
        nameLbl = new Label("Name/alias");
        nameLbl.setStyle(styleCatLabel);
        nameLbl.setPadding(new Insets(4, 10, 0, 0));    // to align the label with the row
        GridPane.setConstraints(nameLbl, 0, nameRow); // ((node, column, row), columnspan, rowspan)
        GridPane.setValignment(nameLbl, VPos.TOP);
        nameBox = new dtfManager();      //A modified VBox that holds a dynamic number of TextFields, organized in rows of up to 5
        GridPane.setConstraints(nameBox, 1, nameRow);
        GridPane.setColumnSpan(nameBox, 2);
        GridPane.setHgrow(nameBox, Priority.ALWAYS);
        nameBox.load(crntPkg);  //fill the dtfManager with data from the current Package


        /* Third row of nodes manages 'Description'. One TextField, nice and easy. */
        final int descRow = nameRow + 1;
        Label descLbl = new Label("Description");
        descLbl.setStyle(styleCatLabel);
        GridPane.setConstraints(descLbl, 0, descRow);
        initDescField();    //set actionlisteners
        GridPane.setConstraints(descField, 1, descRow);
        GridPane.setHgrow(descField, Priority.ALWAYS);
        loadDescription();

        /* Display an image of the Package footprint */
        final VBox imageBox = initImage();
        GridPane.setConstraints(imageBox, 2, descRow, 1, 5);
        mainGc = mainCanvas.getGraphicsContext2D();
        //loadImage(); //If I run this here it makes an image of the empty Package

        /* Fourth row of nodes manages 'characteristics'*/
        final int charaRow = descRow + 1;
        Label characLbl = new Label("Characteristics");
        characLbl.setStyle(styleCatLabel);
        characLbl.setPadding(new Insets(4, 10, 0, 0));    // to align the label with the row
        GridPane.setConstraints(characLbl, 0, charaRow);
        GridPane.setValignment(characLbl, VPos.TOP);
        final VBox charaBranch = initCharaBranch();     // initialize subcomponents and add them as children
        GridPane.setConstraints(charaBranch, 1, charaRow);
        GridPane.setHgrow(charaBranch, Priority.ALWAYS);
        loadCharacteristics();


        /* Fifth row of nodes manages body size */
        final int bdszRow = charaRow + 1;
        Label bodysizeLbl = new Label("Body size");
        bodysizeLbl.setStyle(styleCatLabel);
        GridPane.setConstraints(bodysizeLbl, 0, bdszRow);
        final HBox bodySizeBranch = initBodySizeBranch();
        GridPane.setConstraints(bodySizeBranch, 1, bdszRow);
        GridPane.setHgrow(bodySizeBranch, Priority.ALWAYS);
        loadBodySize();


        /* Sixth row of nodes, for Lead to lead */
        final int ltolRow = bdszRow + 1;
        final Label leadtoleadLbl = new Label("Lead-to-lead");
        leadtoleadLbl.setStyle(styleCatLabel);
        GridPane.setConstraints(leadtoleadLbl, 0, ltolRow);
        final HBox ltolBranch = initLtolBranch();
        ltolBranch.setAlignment(Pos.CENTER_LEFT);
        GridPane.setConstraints(ltolBranch, 1, ltolRow);
        GridPane.setHgrow(ltolBranch, Priority.ALWAYS);
        loadLeadToLead();

        /* Seventh row manages references  */
        final int refRow = ltolRow + 1;
        final Label refLbl = new Label("References");
        refLbl.setStyle(styleCatLabel);
        GridPane.setConstraints(refLbl, 0, refRow);
        GridPane.setValignment(refLbl, VPos.TOP);
        final TitledPane refHolder = initRefHolder();
        refHolder.setMaxHeight(200);
        GridPane.setConstraints(refHolder, 1, refRow);
        GridPane.setHgrow(refHolder, Priority.ALWAYS);

        /* [number] row manages 'Related packages' */
        final int relPackRow = refRow + 1;
        final TitledPane relPackHolder = initRelPackHolder();
        relPackHolder.setMaxHeight(250);
        GridPane.setConstraints(relPackHolder, 1, relPackRow);
        GridPane.setColumnSpan(relPackHolder, 2);
        GridPane.setHgrow(relPackHolder, Priority.ALWAYS);

        /* Eigth row manages variants (standardized specific packages) */
        int spepaRow = relPackRow + 1;
        Label packageLbl = new Label("Variants");
        packageLbl.setStyle(styleCatLabel);
        GridPane.setConstraints(packageLbl, 0, spepaRow);
        GridPane.setValignment(packageLbl, VPos.TOP);
        final TitledPane spepaHolder = initSpepaHolder();
        spepaHolder.setMaxHeight(250);
        GridPane.setConstraints(spepaHolder, 1, spepaRow);
        GridPane.setColumnSpan(spepaHolder, 2);
        GridPane.setHgrow(spepaHolder, Priority.ALWAYS);


        /* Ninth row manages footprint */
        final int ftprintRow = spepaRow + 1;
        final Label footprintLbl = new Label("Footprint");
        footprintLbl.setStyle(styleCatLabel);
        footprintLbl.setPadding(new Insets(4, 0, 0, 0));    // to align the label with the row
        GridPane.setConstraints(footprintLbl, 0, ftprintRow);
        GridPane.setValignment(footprintLbl, VPos.TOP);
        final HBox footprintBranch = initFootprintBranch();
        GridPane.setConstraints(footprintBranch, 1, ftprintRow);
        GridPane.setColumnSpan(footprintBranch, 2);
        GridPane.setHgrow(footprintBranch, Priority.ALWAYS);
        loadFootPrint();


        /* Tenth row manages PadShapes/PadDimensions */
        final int padshapetableRow = ftprintRow + 1;
        final TitledPane padshapeHolder = initPadshapeHolder();
        padshapeHolder.setMaxHeight(205);
        GridPane.setConstraints(padshapeHolder, 1, padshapetableRow);
        GridPane.setColumnSpan(padshapeHolder, 2);
        GridPane.setHgrow(padshapeHolder, Priority.ALWAYS);


        /* Eleventh row manages padPositions */
        final int padpostablerow = padshapetableRow + 1;
        final TitledPane padposHolder = initPadposHolder();
        padposHolder.setMaxHeight(250);
        GridPane.setConstraints(padposHolder, 1, padpostablerow);
        GridPane.setColumnSpan(padposHolder, 2);
        GridPane.setHgrow(padposHolder, Priority.ALWAYS);


        /* Add components to GridPane. Sorted by row */
        grid.getChildren().addAll(menuBox, navBox, searchBox,
                                  nameLbl, nameBox,
                                  descLbl, descField, imageBox,
                                  characLbl, charaBranch,
                                  bodysizeLbl, bodySizeBranch,
                                  leadtoleadLbl, ltolBranch,
                                  refLbl, refHolder,
                                  relPackHolder,
                                  packageLbl, spepaHolder,
                                  footprintLbl, footprintBranch,
                                  padshapeHolder,
                                  padposHolder);
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
        mainScene.setUserData(new String("main"));
        mainScene.getStylesheets().add("file://" + getResourcePath() + "/packages.css");
        primaryStage.setTitle("PACKAGES: repository for component packages & footprints");
        primaryStage.setScene(mainScene);
        currentScene = mainScene;
        currentSceneName = "main";

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

        /* F1 must work in all scenes */
        KeyCombination helpShort = new KeyCodeCombination(KeyCode.F1);
        Runnable helpShortCut = () -> {
            updateHelpTopic();
            changeScene(helpScene);
        };
        mainScene.getAccelerators().put(helpShort, helpShortCut);
        searchScene.getAccelerators().put(helpShort, helpShortCut);
        importScene.getAccelerators().put(helpShort, helpShortCut);

        /* If the configuration was loaded then attempt to open the last used json file. */
        if(Config.pathSet){
            File file = new File(Config.path);
            if(loadMainList(file)){
                loadedFile = file;
                fileIsSet = true;
                lastModified = file.lastModified();
                stage.setWidth(Config.width);
                stage.setHeight(Config.height);
            }
            else{
                System.out.println("Failed to load " + Config.path);
            }
        }
        loadImage();    //in case auto-load doesn't work
        primaryStage.show();

        /* resize grid to window width; note that the width & height can only be
         * queried after initial display of the stage
         */
        prevWidth = stage.getWidth();
        prevHeight = stage.getHeight();


        /* Set periodic code for saving */
        PeriodicPulse pulse = new PeriodicPulse(10) {   //argument is seconds between pulses
            @Override
            public void run() {
                if(fileIsSet){
                    if(loadedFileChanged()){
                        update();
                    }
                    if(changesWereMade){
                        try{
                            if(!save(loadedFile)){
                                System.out.println("Auto-save failed");
                            }
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }
                    }
                }
            }
        };
        pulse.start();

    }
    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! End of start() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */


    /* Change the displayed Scene while keeping window dimensions */
    private void changeScene(Scene nextScene){
        assert(nextScene != null);
        if (nextScene == null)
            nextScene = mainScene;

        previousScene = currentScene;   /* save, so help scene switches back to active scene */
        currentScene = nextScene;

        prevWidth = stage.getWidth();
        prevHeight = stage.getHeight();
        stage.setScene(nextScene);
        stage.setHeight(prevHeight);
        stage.setWidth(prevWidth);
        Object obj = nextScene.getUserData();
        currentSceneName = obj.toString();
    }

    /* The global boolean changesWereMade will determine whether auto-save triggers */
    private void change(String s){
        changesWereMade = true;
        crntPkg.dateUpdate();
    }

    /* Set the application icon */
    private void setIcon(){
        String path = getResourcePath();
        path = path + "/Packages64.png";
        File file = new File(path);
        if(file.exists()){
            Image icon;
            try{
                icon = new Image(file.toURI().toString());
                stage.getIcons().add(icon);
            } catch(Exception e){
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
        return verifyInput(tf, true, true, false, false);
    }
    private boolean verifyInput(TextField tf, boolean permitNegative){
        return verifyInput(tf, permitNegative, true, false, false);
    }
    private boolean verifyInput(TextField tf, boolean permitNegative, boolean permitDecimal){
        return verifyInput(tf, permitNegative, permitDecimal, false, false);
    }
    private boolean verifyInput(TextField tf, boolean permitNegative, boolean permitDecimal, boolean permitLetterPrefix, boolean permitStar){
        String input = tf.getText();
        String output = "";
        int start = 0;
        if(permitStar && input.compareTo("*") == 0){
            output = input;
            start = input.length();
        } else if(permitLetterPrefix){
            char c = input.charAt(start);
            if(Character.isAlphabetic(c)){
                output += Character.toUpperCase(c);
                start += 1;
            }
        }
        boolean oneDot = false;
        for (int i = start; i < input.length(); i++){
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
                tf.setStyle("TextFieldBkgnd: #fff0a0;");
                return false;
            }
        }
        tf.setText(output);
        tf.setStyle("TextFieldBkgnd: white;");
        return true;
    }


    /* Methods that intitialize UI in main scene */
    private HBox initMenuBox(){
        final HBox menuBox = new HBox(SPACING_HBOX);

        final MenuBar menubar = new MenuBar();
        final Menu fileMenu = new Menu("File");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
        final MenuItem loadItem = new MenuItem("_Open...");
        MenuItem saveItem = new MenuItem("_Save"); //initialized here instead of below for visibility
        if(Config.pathSet){
            loadedFile = new File(Config.path);
            lastModified = loadedFile.lastModified();
        } else{
            saveItem.setDisable(true);
        }

        final FileChooser loadChooser = new FileChooser();
        loadChooser.getExtensionFilters().add(extFilter);
        loadItem.setOnAction((ActionEvent t) -> {
            File selectedFile = loadChooser.showOpenDialog(stage);
            if(selectedFile != null){
                try {
                    if(loadMainList(selectedFile)){
                        //System.out.println("Successfully loaded");
                        loadedFile = selectedFile;
                        lastModified = loadedFile.lastModified();
                        fileIsSet = true;
                        saveItem.setDisable(false);
                        Config.setPath(loadedFile.getPath());
                        saveConfig(config);
                    } else{
                        System.out.println("Failed to load " + selectedFile.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        });

        final MenuItem saveAsItem = new MenuItem("Save _as...");
        final FileChooser saveChooser = new FileChooser();
        saveChooser.getExtensionFilters().add(extFilter);
        saveAsItem.setOnAction((ActionEvent t) -> {
            File selectedFile = saveChooser.showSaveDialog(stage);
            if(selectedFile != null){
                try {
                    if(save(selectedFile)){
                        // System.out.println("Successfully saved");
                        loadedFile = selectedFile;
                        lastModified = loadedFile.lastModified();
                        fileIsSet = true;
                        saveItem.setDisable(false);
                        Config.setPath(loadedFile.getPath());
                        saveConfig(config);
                    }
                    else{
                         System.out.println("Failed to save " + selectedFile.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        });

        final MenuItem newItem = new MenuItem("_New...");
        newItem.setOnAction((ActionEvent t) -> {
            File selectedFile = saveChooser.showSaveDialog(stage);
            if(selectedFile != null){
                ArrayList<Package> newList = new ArrayList();
                newList.add(new Package());
                try {
                    if(save(selectedFile, newList, false)){
                        //System.out.println("Successfully saved new file " + selectedFile.getCanonicalPath());
                        loadedFile = selectedFile;
                        lastModified = loadedFile.lastModified();
                        fileIsSet = true;
                        saveItem.setDisable(false);
                        Config.setPath(loadedFile.getPath());
                        saveConfig(config);
                        allPackages.clear();
                        allPackages.addAll(newList);
                        setPackageSelection(false, 0);
                    }
                    else{
                         System.out.println("Failed to save new file " + selectedFile.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        });

        //final MenuItem saveItem = new MenuItem("Save"); declared/initialized above other stuff for visibility
        saveItem.setOnAction((ActionEvent t) -> {
            try {
                if(save(loadedFile)){
                    // System.out.println("Successfully saved");
                    lastModified = loadedFile.lastModified();
                }
                else{
                     System.out.println("Failed to save " + loadedFile.getCanonicalPath());
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        });
        final MenuItem importItem = new MenuItem("Import...");
        importScene = initImportScene();
        importItem.setOnAction((ActionEvent t) -> {
            if(viewingSelection){
                setPackageSelection(false, allPackages.indexOf(crntPkg));
            }
            File selectedFile = loadChooser.showOpenDialog(stage);
            if(selectedFile != null) {
                switch(importPackages(selectedFile)){
                    case -1:
                        //System.out.println("Import failed");
                        impFailed.show(stage);
                        break;
                    case 0:
                        //System.out.println("Loaded packages. No conflicts");
                        impSuccess.show(stage);
                        change("importItem");
                        break;
                    case 1:
                        //System.out.println("Duplicate conflict with imported Packages");
                        impConflicted.update(conflictedImportPacks.size());
                        impConflicted.show(stage);
                }
            }
        });

        final Label exportLabel = new Label("Export...");
        Tooltip exportTip = new Tooltip("Make a selection through the search menu to export");
        Tooltip.install(exportLabel, exportTip);
        exportItem = new CustomMenuItem(exportLabel);
        exportItem.setDisable(true);
        exportItem.setOnAction((ActionEvent t) -> {
            //'save as' for selected packages
            File selectedFile = saveChooser.showSaveDialog(stage);
            if(selectedFile != null){
                try {
                    if(save(selectedFile, selectedPackages, true)){
                        //System.out.println("Successfully exported");
                    }
                    else{
                         System.out.println("Failed to export " + selectedFile.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        });
        final MenuItem helpItem = new MenuItem("Help");
        KeyCombination helpShortcut = new KeyCodeCombination(KeyCode.F1);
        helpItem.setAccelerator(helpShortcut);
        helpScene = initHelpScene();
        helpItem.setOnAction((ActionEvent t) -> {
            updateHelpTopic();
            changeScene(helpScene);
        });
        final MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction((ActionEvent t) -> {
            aboutPopup.update();    /* update current package/variant counts */
            aboutPopup.show(stage);
        });
        final MenuItem exitItem = new MenuItem("E_xit");
        exitItem.setOnAction((ActionEvent t) -> {
            saveAndQuit();
        });
        fileMenu.getItems().addAll(newItem, loadItem, saveItem, saveAsItem,
                                   new SeparatorMenuItem(), importItem, exportItem,
                                   new SeparatorMenuItem(), helpItem, aboutItem,
                                   new SeparatorMenuItem(), exitItem);
        menubar.getMenus().add(fileMenu);

        menuBox.getChildren().addAll(menubar);
        return menuBox;
    }

    private HBox initNavBox(){
        final HBox navButtons = new HBox(SPACING_HBOX); // spacing = 3
        navButtons.setPadding(new Insets(3, 0, 0, 0));

        navButtons.setAlignment(Pos.CENTER_LEFT);
        final Button prvPack = new Button("◀");
        prvPack.setStyle("-fx-font-size:12pt; -fx-padding:0 6px 0 6px;");
        prvPack.setTooltip(new Tooltip("Ctrl+PageUp"));
        prvPack.setOnAction((ActionEvent arg0) -> {
            if(crntIndex > 0){
                navigate(crntIndex - 1);
            }
        });

        displayedIndex = new Label(Integer.toString(crntIndex + 1) + " of " + Integer.toString(viewedPackages.size()));

        final Button nxtPack = new Button("▶");
        nxtPack.setStyle("-fx-font-size:12pt; -fx-padding:0 6px 0 6px;");
        nxtPack.setTooltip(new Tooltip("Ctrl+PageDown"));
        nxtPack.setOnAction((ActionEvent arg0) -> {
            if(crntIndex < viewedPackages.size() - 1){
                navigate(crntIndex + 1);
            }
        });

        newPack = new Button("new");
        newPack.setOnAction((ActionEvent arg0) -> {
            viewedPackages.add(new Package());
            change("newPack");
            navigate(viewedPackages.size() - 1);
        });

        final Button delPack = new Button("delete");
        delPack.setOnAction((ActionEvent arg0) -> {
            delWarning.show(stage);
        });

        final Button revert = new Button("revert");
        revert.setTooltip(new Tooltip("Undo all changes made since selecting this package"));
        revert.setOnAction((ActionEvent arg0) -> {
            //TODO: confirmation popup?
            crntPkg.copy(backupPkg);
            loadAll();
            change("revert");
        });

        backToFull = new Button("Back to full list");
        backToFull.setOnAction((ActionEvent arg0) -> {
            setPackageSelection(false, allPackages.indexOf(crntPkg));
        });
        backToFull.setVisible(false);   /* invisible by default */

        navButtons.getChildren().addAll(prvPack, displayedIndex, nxtPack, newPack, delPack, revert, backToFull);
        return navButtons;
    }

    private void initDescField(){
        descField = new TextField(crntPkg.description);
        descField.setPromptText("short description or keywords");
        descField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                crntPkg.description = descField.getText();
                if(!crntPkg.description.equals(backupPkg.description)){
                    change("descField");
                }
            }
        });
        descField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                crntPkg.description = descField.getText();
                if(!crntPkg.description.equals(backupPkg.description)){
                    if(!crntPkg.description.equals(backupPkg.description)){
                        change("descField");
                    }
                }
            }
        });
    }

    private VBox initImage(){
        final VBox imageBranch = new VBox();

        final Separator topSep = new Separator(Orientation.HORIZONTAL);

        final HBox midBox = new HBox();
        final Separator leftSep = new Separator(Orientation.VERTICAL);
        final Separator rightSep = new Separator(Orientation.VERTICAL);
        mainCanvas = new Canvas(IMAGE_WIDTH, IMAGE_HEIGHT);

        midBox.getChildren().addAll(leftSep, mainCanvas, rightSep);

        final Separator botSep = new Separator(Orientation.HORIZONTAL);

        imageBranch.getChildren().addAll(topSep, midBox, botSep);
        return imageBranch;
    }

    private VBox initCharaBranch(){
        VBox charaBranch = new VBox(SPACING_VBOX); //holds two HBoxes to from two rows of components

        final HBox topChaRow = new HBox(SPACING_HBOX);     //contains first row of the characteristics
        topChaRow.setAlignment(Pos.CENTER_LEFT);

        HBox typeBoxBox = new HBox(SPACING_HBOX);
        typeBoxBox.setAlignment(Pos.CENTER_LEFT);
        final Label chaTypeLabel = new Label("type");
        charatypeBox = new ComboBox();
        charatypeBox.getItems().addAll(Package.charTypeValues());
        charatypeBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.type = Package.charTypefromString((String) charatypeBox.getValue());
            if(crntPkg.type != backupPkg.type){
                change("charatypeBox");
            }
        });
        typeBoxBox.getChildren().addAll(chaTypeLabel, charatypeBox);

        HBox pinsBox = new HBox(SPACING_HBOX);
        pinsBox.setAlignment(Pos.CENTER);
        pinNumber = new TextField();
        pinNumber.setMaxWidth(intInputPrefWidth);
        pinNumber.setOnKeyPressed(event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(pinNumber, false, false)){
                    crntPkg.nrOfPins = Integer.parseInt(pinNumber.getText());
                    if(crntPkg.nrOfPins != backupPkg.nrOfPins){
                        change("pinNumber");
                    }
                }
            }
        });
        pinNumber.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pinNumber, false, false)){
                    crntPkg.nrOfPins = Integer.parseInt(pinNumber.getText());
                    if(crntPkg.nrOfPins != backupPkg.nrOfPins){
                        change("pinNumber");
                    }
                }
            }
        });
        final Label pincountLabel = new Label("pin count");
        pincountLabel.setPadding(new Insets(0, 0, 0, fieldSpacing));
        pinsBox.getChildren().addAll(pincountLabel, pinNumber);

        final HBox pitchBox = new HBox(SPACING_HBOX);
        pitchBox.setAlignment(Pos.CENTER);
        pitchField = new TextField();
        pitchField.setMaxWidth(dimInputPrefWidth);
        pitchField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(pitchField, false)){
                    crntPkg.pitch = Double.parseDouble(pitchField.getText());
                    if(crntPkg.pitch != backupPkg.pitch){
                        change("pitchField");
                    }
                }
            }
        });
        pitchField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pitchField, false)){
                    crntPkg.pitch = Double.parseDouble(pitchField.getText());
                    if(crntPkg.pitch != backupPkg.pitch){
                        change("pitchField");
                    }
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
                if(crntPkg.polarized != backupPkg.polarized){
                    change("polarCheck");
                }
            }
        });

        topChaRow.getChildren().addAll(typeBoxBox, pinsBox, pitchBox, polarCheck);

        final HBox botChaRow = new HBox(SPACING_HBOX);         //contains second row of the characteristics
        botChaRow.setAlignment(Pos.CENTER_LEFT);

        HBox termBoxBox = new HBox(SPACING_HBOX);
        termBoxBox.setAlignment(Pos.CENTER);
        terminationBox = new ComboBox();
        terminationBox.getItems().addAll(Package.termTypeValues());
        terminationBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.termination = Package.termTypefromString((String) terminationBox.getValue());
            checkLeadToLead();
            if(crntPkg.termination != backupPkg.termination){
                change("terminationBox");
            }
        });
        Label termLabel = new Label("terminals");
        termBoxBox.getChildren().addAll(termLabel, terminationBox);

        HBox tapeOrientBox = new HBox(SPACING_HBOX);
        tapeOrientBox.setAlignment(Pos.CENTER);
        tapeOrientation = new ComboBox();
        tapeOrientation.getItems().addAll(Package.orientationValues());
        tapeOrientation.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.tapeOrient = Package.orientationFromInt(Integer.parseInt((String)tapeOrientation.getValue()));
            if(crntPkg.tapeOrient != backupPkg.tapeOrient){
                change("tapeOrientation");
            }
        });
        Label tapeOrientationLabel = new Label("tape packaging orientation");
        tapeOrientationLabel.setPadding(new Insets(0, 0, 0, fieldSpacing));
        tapeOrientBox.getChildren().addAll(tapeOrientationLabel, tapeOrientation);

        botChaRow.getChildren().addAll(termBoxBox, tapeOrientBox);
        charaBranch.getChildren().addAll(topChaRow, botChaRow);

        return charaBranch;
    }

    private HBox initBodySizeBranch(){
        HBox bodySizeBranch = new HBox(SPACING_HBOX);
        bodySizeBranch.setAlignment(Pos.CENTER_LEFT);
        final HBox bodySizeBox = new HBox(SPACING_HBOX);
        bodySizeBox.setAlignment(Pos.CENTER_LEFT);

        bodyXsize = new TextField();
        bodyXsize.setMaxWidth(dimInputPrefWidth);
        bodyXsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyXsize, false)){
                    try{
                        crntPkg.body.bodyX = Double.parseDouble(bodyXsize.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyX = 0.0;
                    }
                    checkBodySize();
                    loadImage();
                    if(crntPkg.body.bodyX != backupPkg.body.bodyX){
                        change("bodyXsize");
                    }
                }
            }
        });
        bodyXsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyXsize, false)){
                    try{
                        crntPkg.body.bodyX = Double.parseDouble(bodyXsize.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyX = 0.0;
                    }
                    checkBodySize();
                    loadImage();
                    if(crntPkg.body.bodyX != backupPkg.body.bodyX){
                        change("bodyXsize");
                    }
                }
            }
        });
        bodyXtol = new TextField();
        bodyXtol.setMaxWidth(tolInputPrefWidth);
        bodyXtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyXtol, false)){
                    try{
                        crntPkg.body.bodyXtol = Double.parseDouble(bodyXtol.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyXtol = 0.0;
                    }
                    if(crntPkg.body.bodyXtol != backupPkg.body.bodyXtol){
                        change("bodyXtol");
                    }
                }
            }
        });
        bodyXtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyXtol, false)){
                    try{
                        crntPkg.body.bodyXtol = Double.parseDouble(bodyXtol.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyXtol = 0.0;
                    }
                    if(crntPkg.body.bodyXtol != backupPkg.body.bodyXtol){
                        change("bodyXtol");
                    }
                }
            }
        });
        bodyYsize = new TextField();
        bodyYsize.setMaxWidth(dimInputPrefWidth);
        bodyYsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyYsize, false)){
                    try{
                        crntPkg.body.bodyY = Double.parseDouble(bodyYsize.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyY = 0.0;
                    }
                    checkBodySize();
                    loadImage();
                    if(crntPkg.body.bodyY != backupPkg.body.bodyY){
                        change("bodyYsize");
                    }
                }
            }
        });
        bodyYsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyYsize, false)){
                    try{
                        crntPkg.body.bodyY = Double.parseDouble(bodyYsize.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyY = 0.0;
                    }
                    checkBodySize();
                    loadImage();
                    if(crntPkg.body.bodyY != backupPkg.body.bodyY){
                        change("bodyYsize");
                    }
                }
            }
        });
        bodyYtol = new TextField();
        bodyYtol.setMaxWidth(tolInputPrefWidth);
        bodyYtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyYtol, false)){
                    try{
                        crntPkg.body.bodyYtol = Double.parseDouble(bodyYtol.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyYtol = 0.0;
                    }
                    if(crntPkg.body.bodyYtol != backupPkg.body.bodyYtol){
                        change("bodyYtol");
                    }
                }
            }
        });
        bodyYtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyYtol, false)){
                    try{
                        crntPkg.body.bodyYtol = Double.parseDouble(bodyYtol.getText());
                    } catch(NumberFormatException e){
                        crntPkg.body.bodyYtol = 0.0;
                    }
                    if(crntPkg.body.bodyYtol != backupPkg.body.bodyYtol){
                        change("bodyYtol");
                    }
                }
            }
        });

        bodySizeBox.getChildren().addAll(bodyXsize, new Label("±"), bodyXtol, new Label("x"), bodyYsize, new Label("±"), bodyYtol, new Label("mm"));

        final HBox bodySizeOrgBox = new HBox(SPACING_HBOX);
        bodySizeOrgBox.setAlignment(Pos.CENTER_LEFT);

        bodyOrgX = new TextField();
        bodyOrgX.setMaxWidth(dimInputPrefWidth);
        bodyOrgX.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyOrgX)){
                    crntPkg.body.bodyOrgX = Double.parseDouble(bodyOrgX.getText());
                    loadImage();
                    if(crntPkg.body.bodyOrgX != backupPkg.body.bodyOrgX){
                        change("bodyOrgX");
                    }
                }
            }
        });
        bodyOrgX.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyOrgX)){
                    crntPkg.body.bodyOrgX = Double.parseDouble(bodyOrgX.getText());
                    loadImage();
                    if(crntPkg.body.bodyOrgX != backupPkg.body.bodyOrgX){
                        change("bodyOrgX");
                    }
                }
            }
        });

        bodyOrgY = new TextField();
        bodyOrgY.setMaxWidth(dimInputPrefWidth);
        bodyOrgY.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(bodyOrgY)){
                    crntPkg.body.bodyOrgY = Double.parseDouble(bodyOrgY.getText());
                    loadImage();
                    if(crntPkg.body.bodyOrgY != backupPkg.body.bodyOrgY){
                        change("bodyOrgY");
                    }
                }
            }
        });
        bodyOrgY.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(bodyOrgY)){
                    crntPkg.body.bodyOrgY = Double.parseDouble(bodyOrgY.getText());
                    loadImage();
                    if(crntPkg.body.bodyOrgY != backupPkg.body.bodyOrgY){
                        change("bodyOrgY");
                    }
                }
            }
        });

        final Label bodyOrgLbl = new Label("origin X");
        bodyOrgLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        bodySizeOrgBox.getChildren().addAll(bodyOrgLbl, bodyOrgX, new Label("Y"), bodyOrgY);

        bodySizeBranch.getChildren().addAll(bodySizeBox, bodySizeOrgBox, createHSpacer());
        return bodySizeBranch;
    }

    private HBox initLtolBranch(){
        HBox ltolBranch = new HBox(SPACING_HBOX);
        final HBox ltolBox = new HBox(SPACING_HBOX);
        ltolBox.setAlignment(Pos.CENTER_LEFT);

        ltolXsize = new TextField();
        ltolXsize.setMaxWidth(dimInputPrefWidth);
        ltolXsize.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolXsize, false)){
                    crntPkg.lead2lead.x = Double.parseDouble(ltolXsize.getText());
                    checkLeadToLead();
                    loadImage();
                    if(crntPkg.lead2lead.x != backupPkg.lead2lead.x){
                        change("ltolXsize");
                    }
                }
            }
        });
        ltolXsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolXsize, false)){
                    crntPkg.lead2lead.x = Double.parseDouble(ltolXsize.getText());
                    checkLeadToLead();
                    loadImage();
                    if(crntPkg.lead2lead.x != backupPkg.lead2lead.x){
                        change("ltolXsize");
                    }
                }
            }
        });
        ltolXtol = new TextField();
        ltolXtol.setMaxWidth(tolInputPrefWidth);
        ltolXtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolXtol, false)){
                    crntPkg.lead2lead.xTol = Double.parseDouble(ltolXtol.getText());
                    if(crntPkg.lead2lead.xTol != backupPkg.lead2lead.xTol){
                        change("ltolXtol");
                    }
                }
            }
        });
        ltolXtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolXtol, false)){
                    crntPkg.lead2lead.xTol = Double.parseDouble(ltolXtol.getText());
                    if(crntPkg.lead2lead.xTol != backupPkg.lead2lead.xTol){
                        change("ltolXtol");
                    }
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
                    if(crntPkg.lead2lead.y != backupPkg.lead2lead.y){
                        change("ltolYsize");
                    }
                }
            }
        });
        ltolYsize.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolYsize, false)){
                    crntPkg.lead2lead.y = Double.parseDouble(ltolYsize.getText());
                    checkLeadToLead();
                    loadImage();
                    if(crntPkg.lead2lead.y != backupPkg.lead2lead.y){
                        change("ltolYsize");
                    }
                }
            }
        });

        ltolYtol = new TextField();
        ltolYtol.setMaxWidth(tolInputPrefWidth);
        ltolYtol.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolYtol, false)){
                    crntPkg.lead2lead.yTol = Double.parseDouble(ltolYtol.getText());
                    if(crntPkg.lead2lead.yTol != backupPkg.lead2lead.yTol){
                        change("ltolYtol");
                    }
                }
            }
        });
        ltolYtol.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolYtol, false)){
                    crntPkg.lead2lead.yTol = Double.parseDouble(ltolYtol.getText());
                    if(crntPkg.lead2lead.yTol != backupPkg.lead2lead.yTol){
                        change("ltolYtol");
                    }
                }
            }
        });

        ltolBox.getChildren().addAll(ltolXsize, new Label("±"), ltolXtol, new Label("x"), ltolYsize, new Label("±"), ltolYtol, new Label("mm"));

        final HBox ltolOrgBox = new HBox(SPACING_HBOX);
        ltolOrgBox.setAlignment(Pos.CENTER_LEFT);

        ltolOrgX = new TextField();
        ltolOrgX.setMaxWidth(dimInputPrefWidth);
        ltolOrgX.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolOrgX)){
                    crntPkg.lead2lead.orgX = Double.parseDouble(ltolOrgX.getText());
                    loadImage();
                    if(crntPkg.lead2lead.orgX != backupPkg.lead2lead.orgX){
                        change("ltolOrgX");
                    }
                }
            }
        });
        ltolOrgX.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolOrgX)){
                    crntPkg.lead2lead.orgX = Double.parseDouble(ltolOrgX.getText());
                    loadImage();
                    if(crntPkg.lead2lead.orgX != backupPkg.lead2lead.orgX){
                        change("ltolOrgX");
                    }
                }
            }
        });
        ltolOrgY = new TextField();
        ltolOrgY.setMaxWidth(dimInputPrefWidth);
        ltolOrgY.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(ltolOrgY)){
                    crntPkg.lead2lead.orgY = Double.parseDouble(ltolOrgY.getText());
                    loadImage();
                    if(crntPkg.lead2lead.orgY != backupPkg.lead2lead.orgY){
                        change("ltolOrgY");
                    }
                }
            }
        });
        ltolOrgY.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(ltolOrgY)){
                    crntPkg.lead2lead.orgY = Double.parseDouble(ltolOrgY.getText());
                    loadImage();
                    if(crntPkg.lead2lead.orgY != backupPkg.lead2lead.orgY){
                        change("ltolOrgY");
                    }
                }
            }
        });

        final Label ltolOrgLbl = new Label("origin X");
        ltolOrgLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        ltolOrgBox.getChildren().addAll(ltolOrgLbl, ltolOrgX, new Label("Y"), ltolOrgY);
        ltolBranch.getChildren().addAll(ltolBox, ltolOrgBox, createHSpacer());
        return ltolBranch;
    }

    private TitledPane initSpepaHolder(){
        TitledPane spepaholder = new TitledPane();
        spepaholder.setText("Specific package variants");
        VBox spepaBranch = new VBox(SPACING_VBOX);

        /*The table */
        TableView<SpepaMirror> spepaTable = new TableView<>();
        spepaTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        spepaTable.setEditable(true);
        spepaTable.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn, TableCell> cellFactory = (TableColumn p) -> new CustomCell();
        Callback<TableColumn, TableCell> cellDoubleFactory = (TableColumn p) -> new EditingDoubleCell();
        Callback<TableColumn, TableCell> cellDelFactory = (TableColumn p) -> new DelButtonCell(spepaTable);
        //Callback<TableColumn, TableCell> cellCheckFactory = (TableColumn p) -> new CustomCheckBoxCell();


        TableColumn ipcNameCol = new TableColumn("name");
        ipcNameCol.setMinWidth(100);
        ipcNameCol.setCellValueFactory(new PropertyValueFactory<>("spepaNameppt"));
        ipcNameCol.setCellFactory(cellFactory);
        ipcNameCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, String>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, String> t) {
                    SpepaMirror spep = ((SpepaMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    spep.getVariant().variantName = t.getNewValue();
                    spep.setIpcName(t.getNewValue());
                    if(spep.getVariant(backupPkg) == null || !spep.getVariant().variantName.equals(spep.getVariant(backupPkg).variantName)){
                        change("spepipcname");
                    }
                }
            }
        );

        TableColumn minHeightCol = new TableColumn("min. height");
        minHeightCol.setMinWidth(THINCOLUMN_MINWIDTH);
        minHeightCol.setPrefWidth(THINCOLUMN_PREFWIDTH);
        minHeightCol.setMaxWidth(THINCOLUMN_MAXWIDTH);
        minHeightCol.setCellValueFactory(new PropertyValueFactory<>("minHeightppt"));
        minHeightCol.setCellFactory(cellDoubleFactory);
        minHeightCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, Double> t) {
                    SpepaMirror spep = ((SpepaMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    spep.setMinHeight(t.getNewValue());
                    spep.getVariant().heightRange.minHeight = t.getNewValue();
                    if(spep.getVariant(backupPkg) == null || spep.getVariant().heightRange.minHeight != spep.getVariant(backupPkg).heightRange.minHeight){
                        change("spepminheight");
                    }
                }
            }
        );

        TableColumn maxHeightCol = new TableColumn("max. height");
        maxHeightCol.setMinWidth(THINCOLUMN_MINWIDTH);
        maxHeightCol.setPrefWidth(THINCOLUMN_PREFWIDTH);
        maxHeightCol.setMaxWidth(THINCOLUMN_MAXWIDTH);
        maxHeightCol.setCellValueFactory(new PropertyValueFactory<>("maxHeightppt"));
        maxHeightCol.setCellFactory(cellDoubleFactory);
        maxHeightCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, Double> t) {
                    SpepaMirror spep = ((SpepaMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    spep.setMaxHeight(t.getNewValue());
                    spep.getVariant().heightRange.maxHeight = t.getNewValue();
                    if(spep.getVariant(backupPkg) == null || spep.getVariant().heightRange.maxHeight != spep.getVariant(backupPkg).heightRange.maxHeight){
                        change("spepmaxheight");
                    }
                }
            }
        );

        TableColumn standardCol = new TableColumn("standard");
        standardCol.setMinWidth(THINCOLUMN_MINWIDTH);
        standardCol.setPrefWidth(THINCOLUMN_PREFWIDTH);
        standardCol.setMaxWidth(THINCOLUMN_MAXWIDTH);
        standardCol.setCellValueFactory(new PropertyValueFactory<>("standardppt"));
        standardCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.nameStandardValues()));
        standardCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SpepaMirror, String>>() {
                @Override
                public void handle(CellEditEvent<SpepaMirror, String> t) {
                    SpepaMirror spep = ((SpepaMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    spep.setStandard(t.getNewValue());
                    spep.getVariant().standard = Package.nameStandardFromString(t.getNewValue());
                    if(spep.getVariant(backupPkg) == null || spep.getVariant().standard != spep.getVariant(backupPkg).standard){
                        change("spepstandard");
                    }
                }
            }
        );

        TableColumn xposedCol = new TableColumn("exposed pad");
        xposedCol.setMinWidth(90);  //needs to be a little wider than other thin columns in order for the name not to be abreviated
        xposedCol.setPrefWidth(THINCOLUMN_PREFWIDTH);
        xposedCol.setMaxWidth(THINCOLUMN_MAXWIDTH);
        xposedCol.setCellValueFactory(new PropertyValueFactory<>("padExposedppt"));
        xposedCol.setCellFactory(CustomCheckBoxCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(Integer param) {
                SpepaMirror spep = spePacks.get(param);
                spep.padExposed = spep.padExposedppt.get();
                spep.getVariant().centerPadExposed = spep.padExposed;
                if(spep.getVariant(backupPkg) == null || spep.getVariant().centerPadExposed != spep.getVariant(backupPkg).centerPadExposed){
                        change("spepexposedpad");
                    }
                return spep.padExposedppt;
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
                    SpepaMirror spep = ((SpepaMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    spep.setNotes(t.getNewValue());
                    spep.getVariant().variantNotes = t.getNewValue();
                    if(spep.getVariant(backupPkg) == null || !spep.getVariant().variantNotes.equals(spep.getVariant(backupPkg).variantNotes)){
                        change("spepnotes");
                    }
                }
            }
        );

        TableColumn delCol = new TableColumn("");
        delCol.setMinWidth(50);
        delCol.setPrefWidth(50);
        delCol.setMaxWidth(50);
        delCol.setCellFactory(cellDelFactory);

        spepaTable.setItems(spePacks);
        spepaTable.getColumns().addAll(ipcNameCol, minHeightCol, maxHeightCol, standardCol, xposedCol, notesCol, delCol);

        HBox spepaAdditionBox = new HBox(SPACING_HBOX);
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

        final ComboBox nameStandardInput =  new ComboBox(Package.nameStandardValues());

        final CheckBox exposedInput = new CheckBox("exposed center pad");
        exposedInput.setPadding(new Insets(0, fieldSpacing, 0, fieldSpacing));

        final TextField spepaNotesInput = new TextField();
        spepaNotesInput.setPromptText("notes");

        final Button addSpepaButton = new Button("Add");
        addSpepaButton.setOnAction((ActionEvent e) -> {
            if(ipcNameInput.getText().length() == 0){
                incompleteDataWarning.show(stage);
            } else if(notDuplicate(ipcNameInput.getText())){
                /* Create mirror object */
                SpepaMirror newSpep = new SpepaMirror( //args: String ipcName, NameStandard, double minHeight, double maxHeight, boolean padExposed, String alias
                    ipcNameInput.getText(),
                    Package.nameStandardFromString((String)nameStandardInput.getValue()),
                    (minHeightInput.getText().length() > 0) ? Double.parseDouble(minHeightInput.getText()) : 0,
                    (maxHeightInput.getText().length() > 0) ? Double.parseDouble(maxHeightInput.getText()) : 0,
                    exposedInput.isSelected(),
                    spepaNotesInput.getText()
                );
                /* Add to real list */
                crntPkg.specPacks.add(crntPkg.new Variant(newSpep));
                /* Add to mirror list*/
                spePacks.add(newSpep);
                change("addSpepaButton");
            } else{
                dupWarning.show(stage);
            }
            ipcNameInput.clear();
            nameStandardInput.valueProperty().set(null);
            minHeightInput.clear();
            maxHeightInput.clear();
            exposedInput.setSelected(false);
            spepaNotesInput.clear();
            spepaTable.scrollTo(spepaTable.getItems().size() - 1);
        });

        spepaAdditionBox.getChildren().addAll(ipcNameInput, minHeightInput, maxHeightInput, nameStandardInput, exposedInput, spepaNotesInput, addSpepaButton);

        spepaBranch.getChildren().addAll(spepaTable, spepaAdditionBox);
        spepaholder.setContent(spepaBranch);

        return spepaholder;
    }

    private HBox initFootprintBranch(){
        final HBox footprintBranch = new HBox(SPACING_HBOX);

        final HBox navBox = new HBox(SPACING_HBOX);
        navBox.setAlignment(Pos.CENTER);
        final Button prevButton = new Button("◀");
        prevButton.setStyle("-fx-font-size:12pt; -fx-padding:0 6px 0 6px;");
        prevButton.setOnAction((ActionEvent arg0) -> {
            if(fpIndex > 0){
                fpIndex -=1;
                loadFootPrintInclusive();
            }
        });
        fpIndexLabel = new Label(Integer.toString(fpIndex + 1) + " of " + Integer.toString(crntPkg.footPrints.size()));
        final Button nextButton = new Button("▶");
        nextButton.setStyle("-fx-font-size:12pt; -fx-padding:0 6px 0 6px;");
        nextButton.setOnAction((ActionEvent arg0) -> {
            if(fpIndex < crntPkg.footPrints.size() - 1){
                fpIndex +=1;
                loadFootPrintInclusive();
            }
        });
        final Button newButton = new Button("new");
        newButton.setOnAction((ActionEvent arg0) -> {
            crntPkg.addFootprint(crntPkg.new Footprint());
            fpIndex = crntPkg.footPrints.size() -1;
            loadFootPrintInclusive();
            change("new footprint");
        });
        final Button delButton = new Button("delete");
        delButton.setOnAction((ActionEvent arg0) -> {
            fpDelWarning.show(stage);
        });
        navBox.getChildren().addAll(prevButton, fpIndexLabel, nextButton, newButton, delButton);

        footprinttypeBox = new ComboBox();
        footprinttypeBox.getItems().addAll(Package.footprintTypeValues());
        footprinttypeBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            crntPkg.footPrints.get(fpIndex).ftprntType = Package.footprintTypefromString((String)footprinttypeBox.getValue());
            if(crntPkg.footPrints.get(fpIndex).ftprntType != backupPkg.footPrints.get(fpIndex).ftprntType){
                change("footprinttypeBox");
            }
        });

        final HBox spanBox = new HBox(SPACING_HBOX);
        spanBox.setAlignment(Pos.CENTER_LEFT);
        spanXField = new TextField();
        spanXField.setMaxWidth(dimInputPrefWidth);
        spanXField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(spanXField, false)){
                    crntPkg.footPrints.get(fpIndex).span.x = Double.parseDouble(spanXField.getText());
                    checkSpan();
                    if(crntPkg.footPrints.get(fpIndex).span.x != backupPkg.footPrints.get(fpIndex).span.x){
                        change("fpspan");
                    }
                }
            }
        });
        spanXField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(spanXField, false)){
                    crntPkg.footPrints.get(fpIndex).span.x = Double.parseDouble(spanXField.getText());
                    checkSpan();
                    if(crntPkg.footPrints.get(fpIndex).span.x != backupPkg.footPrints.get(fpIndex).span.x){
                        change("fpspan");
                    }
                }
            }
        });

        spanYField = new TextField();
        spanYField.setMaxWidth(dimInputPrefWidth);
        spanYField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(spanYField, false)){
                    crntPkg.footPrints.get(fpIndex).span.y = Double.parseDouble(spanYField.getText());
                    checkSpan();
                    if(crntPkg.footPrints.get(fpIndex).span.y != backupPkg.footPrints.get(fpIndex).span.y){
                        change("fpspanx");
                    }
                }
            }
        });
        spanYField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(spanYField, false)){
                    crntPkg.footPrints.get(fpIndex).span.y = Double.parseDouble(spanYField.getText());
                    checkSpan();
                    if(crntPkg.footPrints.get(fpIndex).span.y != backupPkg.footPrints.get(fpIndex).span.y){
                        change("fpspany");
                    }
                }
            }
        });

        final Label spanLbl = new Label("span");
        spanLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        spanBox.getChildren().addAll(spanLbl, spanXField, new Label("x"), spanYField, new Label("mm"));

        final HBox fpoutlineBox = new HBox(SPACING_HBOX);
        fpoutlineBox.setAlignment(Pos.CENTER_LEFT);
        fpolLength = new TextField();   //TODO: possibly swap length and width. Currently length is X and width is Y here, but it's different elsewhere
        fpolLength.setMaxWidth(dimInputPrefWidth);
        fpolLength.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolLength, false)){
                    crntPkg.footPrints.get(fpIndex).outline.length = Double.parseDouble(fpolLength.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.length != backupPkg.footPrints.get(fpIndex).outline.length){
                        change("fpoutlinelength");
                    }
                }
            }
        });
        fpolLength.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolLength, false)){
                    crntPkg.footPrints.get(fpIndex).outline.length = Double.parseDouble(fpolLength.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.length != backupPkg.footPrints.get(fpIndex).outline.length){
                        change("fpoutlinelength");
                    }
                }
            }
        });
        fpolWidth = new TextField();
        fpolWidth.setMaxWidth(dimInputPrefWidth);
        fpolWidth.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolWidth, false)){
                    crntPkg.footPrints.get(fpIndex).outline.width = Double.parseDouble(fpolWidth.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.width != backupPkg.footPrints.get(fpIndex).outline.width){
                        change("fpoutlinewidth");
                    }
                }
            }
        });
        fpolWidth.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolWidth, false)){
                    crntPkg.footPrints.get(fpIndex).outline.width = Double.parseDouble(fpolWidth.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.width != backupPkg.footPrints.get(fpIndex).outline.width){
                        change("fpoutlinewidth");
                    }
                }
            }
        });
        fpolOrgX = new TextField();
        fpolOrgX.setMaxWidth(dimInputPrefWidth);
        fpolOrgX.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolOrgX)){
                    crntPkg.footPrints.get(fpIndex).outline.orgX = Double.parseDouble(fpolOrgX.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.orgX != backupPkg.footPrints.get(fpIndex).outline.orgX){
                        change("fpoutlineorgX");
                    }
                }
            }
        });
        fpolOrgX.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolOrgX)){
                    crntPkg.footPrints.get(fpIndex).outline.orgX = Double.parseDouble(fpolOrgX.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.orgX != backupPkg.footPrints.get(fpIndex).outline.orgX){
                        change("fpoutlineorgX");
                    }
                }
            }
        });
        fpolOrgY = new TextField();
        fpolOrgY.setMaxWidth(dimInputPrefWidth);
        fpolOrgY.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                if(verifyInput(fpolOrgY)){
                    crntPkg.footPrints.get(fpIndex).outline.orgY = Double.parseDouble(fpolOrgY.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.orgY != backupPkg.footPrints.get(fpIndex).outline.orgY){
                        change("fpoutlineorgY");
                    }
                }
            }
        });
        fpolOrgY.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(fpolOrgY)){
                    crntPkg.footPrints.get(fpIndex).outline.orgY = Double.parseDouble(fpolOrgY.getText());
                    checkContour();
                    loadImage();
                    if(crntPkg.footPrints.get(fpIndex).outline.orgY != backupPkg.footPrints.get(fpIndex).outline.orgY){
                        change("fpoutlineorgY");
                    }
                }
            }
        });

        final Label fpoutlineLbl = new Label("contour X");
        fpoutlineLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        final Label fporiginLbl = new Label("origin X");
        fporiginLbl.setPadding(new Insets(0, 0, 0, fieldSpacing));
        fpoutlineBox.getChildren().addAll(fpoutlineLbl, fpolLength, new Label("Y"), fpolWidth, fporiginLbl, fpolOrgX, new Label("Y"), fpolOrgY);
        footprintBranch.getChildren().addAll(navBox, createHSpacer(), footprinttypeBox, spanBox, createHSpacer(), fpoutlineBox, createHSpacer());

        return footprintBranch;
    }

    private TitledPane initPadshapeHolder(){
        TitledPane padshapeholder = new TitledPane();
        padshapeholder.setText("Pad shapes");
        padshapeholder.setExpanded(false);
        VBox shaBranch = new VBox(SPACING_VBOX);//parameter is spacing

        /* the table */
        TableView<PadDimMirror> padshapeTable = new TableView<>();
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
            new EventHandler<CellEditEvent<PadDimMirror, Integer>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, Integer> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    dim.getDimension().setPadId(t.getNewValue());
                    dim.setPadId(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().padId != dim.getDimension(backupPkg).padId){
                        change("pad shape pad-id");
                    }
                }
            }
        );

        TableColumn widCol = new TableColumn("cx");
        widCol.setMinWidth(50);
        widCol.setCellValueFactory(new PropertyValueFactory<>("widthppt"));
        widCol.setCellFactory(cellDoubleFactory);
        widCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, Double> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    dim.getDimension().setWidth(t.getNewValue());
                    dim.setWidth(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().getWidth() != dim.getDimension(backupPkg).getWidth()){
                        change("pad shape width");
                    }
                }
            }
        );

        TableColumn lenCol = new TableColumn("cy");
        lenCol.setMinWidth(50);
        lenCol.setCellValueFactory(new PropertyValueFactory<>("lengthppt"));
        lenCol.setCellFactory(cellDoubleFactory);
        lenCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, Double> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    dim.getDimension().setLength(t.getNewValue());
                    dim.setLength(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().getLength() != dim.getDimension(backupPkg).getLength()){
                        change("pad shape Length");
                    }
                }
            }
        );

        TableColumn holeCol = new TableColumn("Hole");
        holeCol.setMinWidth(70);
        holeCol.setCellValueFactory(new PropertyValueFactory<>("holeDiamppt"));
        holeCol.setCellFactory(cellDoubleFactory);
        holeCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, Double> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    dim.getDimension().setHoleDiam(t.getNewValue());
                    dim.setHoleDiam(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().getHoleDiam() != dim.getDimension(backupPkg).getHoleDiam()){
                        change("pad shape hole diam");
                    }
                }
            }
        );

        TableColumn ognxCol = new TableColumn("origin X");
        ognxCol.setMinWidth(70);
        ognxCol.setCellValueFactory(new PropertyValueFactory<>("originXppt"));
        ognxCol.setCellFactory(cellDoubleFactory);
        ognxCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, Double> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    dim.getDimension().setOriginX(t.getNewValue());
                    dim.setOriginX(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().getOriginX() != dim.getDimension(backupPkg).getOriginX()){
                        change("pad shape orgX");
                    }
                }
            }
        );

        TableColumn ognyCol = new TableColumn("origin Y");
        ognyCol.setMinWidth(70);
        ognyCol.setCellValueFactory(new PropertyValueFactory<>("originYppt"));
        ognyCol.setCellFactory(cellDoubleFactory);
        ognyCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, Double> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    dim.getDimension().setOriginY(t.getNewValue());
                    dim.setOriginY(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().getOriginY() != dim.getDimension(backupPkg).getOriginY()){
                        change("pad shape orgY");
                    }
                }
            }
        );

        TableColumn shapeCol = new TableColumn("Shape");
        shapeCol.setMinWidth(100);
        shapeCol.setCellValueFactory(new PropertyValueFactory<>("shapeppt"));
        shapeCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.padShapeValues()));
        shapeCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, String> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    Package.PadShape shape = Package.padShapefromString(t.getNewValue());
                    dim.getDimension().setPadShape(shape);
                    dim.setShape(t.getNewValue());
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().getPadshape() != dim.getDimension(backupPkg).getPadshape()){
                        change("pad shape shape");
                    }
                    if(shape == Package.PadShape.POLYGON){
                        dim.setIsPoly(true);
                    } else{
                        dim.setIsPoly(false);
                    }
                }
            }
        );

        TableColumn xposedCol = new TableColumn("pad type");
        xposedCol.setMinWidth(80);
        xposedCol.setCellValueFactory(new PropertyValueFactory<>("padTypeppt"));
        xposedCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.padTypeValues()));
        xposedCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadDimMirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadDimMirror, String> t) {
                    PadDimMirror dim = ((PadDimMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    Package.PadType type = Package.padTypeFromString(t.getNewValue());
                    dim.getDimension().padType = type;
                    dim.setPadExposed(type);
                    dimChecks();
                    if(dim.getDimension(backupPkg) == null || dim.getDimension().padType != dim.getDimension(backupPkg).padType){
                        change("pad shape type");
                    }
                }
            }
        );

        Callback<TableColumn, TableCell> cellDelFactory = (TableColumn p) -> new DelButtonCell(padshapeTable);
        TableColumn delCol = new TableColumn("");
        delCol.setMinWidth(50);
        delCol.setPrefWidth(50);
        delCol.setMaxWidth(50);
        delCol.setCellFactory(cellDelFactory);

        Callback<TableColumn, TableCell> cellEditFactory = (TableColumn p) -> new EditButtonCell(padshapeTable);
        TableColumn editCol = new TableColumn("Edit");
        editCol.setMinWidth(45);
        editCol.setPrefWidth(45);
        editCol.setMaxWidth(45);
        editCol.setCellValueFactory(new PropertyValueFactory<>("isPoly"));
        editCol.setCellFactory(cellEditFactory);

        padshapeTable.setItems(pdDimensions);
        padshapeTable.getColumns().addAll(nrCol, widCol, lenCol, holeCol, ognxCol, ognyCol, shapeCol, xposedCol, delCol, editCol);

        /* shapeAdditionBox contains controls for adding items the list, and therefore the padShape table */
        HBox shapeAdditionBox = new HBox(SPACING_HBOX);
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

        final ComboBox shapeBox = new ComboBox(Package.padShapeValues());
        shapeBox.setTooltip(new Tooltip("Pad shape (mandatory)."));

        final ComboBox padTypeInput = new ComboBox(Package.padTypeValues());
        padTypeInput.setPadding(new Insets(0, fieldSpacing, 0, fieldSpacing));
        padTypeInput.setValue(Package.padTypeAsString(Package.PadType.STANDARD));

        final Button addShapeButton = new Button("Add");
        addShapeButton.setOnAction((ActionEvent e) -> {
            /* check mandatory fields */
            if(padIdInput.getText().length() == 0 ||
               lengthInput.getText().length() == 0 ||
               widthInput.getText().length() == 0 ||
               lengthInput.getText().length() == 0 ||
               shapeBox.getValue() == null || shapeBox.getValue().toString().length() == 0){
                incompleteDataWarning.show(stage);
            } else {
                /* Add to mirror list */
                PadDimMirror newDim = new PadDimMirror( //args: int padId, double length, double width, Package.PadShape shape, double holeDiam, double originX, double originY, PadType pt
                        Integer.parseInt(padIdInput.getText()),
                        Double.parseDouble(lengthInput.getText()),
                        Double.parseDouble(widthInput.getText()),
                        Package.padShapefromString((String)shapeBox.getValue()),
                        (holeInput.getText().length() > 0) ? Double.parseDouble(holeInput.getText()) : 0,
                        (oriXinput.getText().length() > 0) ? Double.parseDouble(oriXinput.getText()) : 0,
                        (oriYinput.getText().length() > 0) ? Double.parseDouble(oriYinput.getText()) : 0,
                        Package.padTypeFromString((String) padTypeInput.getValue()),
                        null //this is where a Polygon would go... if I had one!
                );
                pdDimensions.add(newDim);
                /* Add to real list */
                crntPkg.footPrints.get(fpIndex).dimensions.add(crntPkg.footPrints.get(fpIndex).new PadDimension(newDim));
                change("added padshape");

                /* reset input fields */
                padIdInput.setText(Integer.toString(highestPadId() + 1));
                lengthInput.clear();
                widthInput.clear();
                shapeBox.valueProperty().set(null);
                holeInput.clear();
                oriXinput.clear();
                oriYinput.clear();
                padTypeInput.setValue(Package.padTypeAsString(Package.PadType.STANDARD));
                padshapeTable.scrollTo(padshapeTable.getItems().size() - 1);
                dimChecks();
            }
        });

        shapeAdditionBox.getChildren().addAll(padIdInput, lengthInput, widthInput, holeInput, oriXinput, oriYinput,
                                              shapeBox, padTypeInput, addShapeButton, createHSpacer()/*, polyInstruct, editPolyButton*/);

        shaBranch.getChildren().addAll(padshapeTable, shapeAdditionBox);
        padshapeholder.setContent(shaBranch);

        return padshapeholder;
    }

    private TitledPane initPadposHolder(){
        TitledPane padposholder = new TitledPane();
        padposholder.setText("Pad positions");
        padposholder.setExpanded(false);
        VBox posBranch = new VBox(SPACING_VBOX);//parameter is spacing

        TableView<PadPosMirror> padposTable = new TableView<>();
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
            new EventHandler<CellEditEvent<PadPosMirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadPosMirror, String> t) {
                    PadPosMirror pos = ((PadPosMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    pos.getPosition().setPinId(t.getNewValue());
                    pos.setPinId(t.getNewValue());
                    posChecks();
                    if(pos.getPosition(backupPkg) == null || !pos.getPosition().getPinId().equals(pos.getPosition(backupPkg).getPinId())){
                        change("padpos pin-id");
                    }
                }
            }
        );

        TableColumn padidCol = new TableColumn("pad-id");
        padidCol.setMinWidth(100);
        padidCol.setCellValueFactory(new PropertyValueFactory<>("padIdppt"));
        padidCol.setCellFactory(cellIntFactory);
        padidCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosMirror, Integer>>() {
                @Override
                public void handle(CellEditEvent<PadPosMirror, Integer> t) {
                    PadPosMirror pos = ((PadPosMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    pos.getPosition().setPadId(t.getNewValue());
                    pos.setPadId(t.getNewValue());
                    posChecks();
                    if(pos.getPosition(backupPkg) == null || pos.getPosition().getPadId() != pos.getPosition(backupPkg).getPadId()){
                        change("padpos pad-id");
                    }
                }
            }
        );

        TableColumn xposCol = new TableColumn("X");
        xposCol.setMinWidth(100);
        xposCol.setCellValueFactory(new PropertyValueFactory<>("xPosppt"));
        xposCol.setCellFactory(cellDoubleFactory);
        xposCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadPosMirror, Double> t) {
                    PadPosMirror pos = ((PadPosMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    pos.getPosition().setXPos(t.getNewValue());
                    pos.setXPos(t.getNewValue());
                    posChecks();
                    if(pos.getPosition(backupPkg) == null || pos.getPosition().getXPos() != pos.getPosition(backupPkg).getXPos()){
                        change("padpos x");
                    }
                }
            }
        );

        TableColumn yposCol = new TableColumn("Y");
        yposCol.setMinWidth(100);
        yposCol.setCellValueFactory(new PropertyValueFactory<>("yPosppt"));
        yposCol.setCellFactory(cellDoubleFactory);
        yposCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosMirror, Double>>() {
                @Override
                public void handle(CellEditEvent<PadPosMirror, Double> t) {
                    PadPosMirror pos = ((PadPosMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    pos.getPosition().setYPos(t.getNewValue());
                    pos.setYPos(t.getNewValue());
                    posChecks();
                    if(pos.getPosition(backupPkg) == null || pos.getPosition().getYPos() != pos.getPosition(backupPkg).getYPos()){
                        change("padpos y");
                    }
                }
            }
        );

        TableColumn rotCol = new TableColumn("rotation");
        rotCol.setMinWidth(100);
        rotCol.setCellValueFactory(new PropertyValueFactory<>("rotationppt"));
        rotCol.setCellFactory(ComboBoxTableCell.forTableColumn(Package.orientationValues()));
        rotCol.setOnEditCommit(
            new EventHandler<CellEditEvent<PadPosMirror, String>>() {
                @Override
                public void handle(CellEditEvent<PadPosMirror, String> t) {
                    PadPosMirror pos = ((PadPosMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    pos.getPosition().setRotation(Package.orientationFromString(t.getNewValue()));
                    pos.setRotation(t.getNewValue());
                    posChecks();
                    if(pos.getPosition(backupPkg) == null || pos.getPosition().getRotation() != pos.getPosition(backupPkg).getRotation()){
                        change("padpos rotation");
                    }
                }
            }
        );

        Callback<TableColumn, TableCell> cellDelFactory = (TableColumn p) -> new DelButtonCell(padposTable);
        TableColumn delCol = new TableColumn("");
        delCol.setMinWidth(50);
        delCol.setPrefWidth(50);
        delCol.setMaxWidth(50);
        delCol.setCellFactory(cellDelFactory);

        padposTable.setItems(pdPositions);
        padposTable.getColumns().addAll(idCol, padidCol, xposCol, yposCol, rotCol, delCol);

        HBox positionAdditionBox = new HBox(SPACING_HBOX);

        pinIdInput = new TextField(Integer.toString(highestPinId() + 1));
        pinIdInput.setMaxWidth(dimInputPrefWidth);
        pinIdInput.setPromptText("pin-id");
        pinIdInput.setTooltip(new Tooltip("Pin number or pin name (may be a * for a mechanical pad)"));
        pinIdInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue){
                if(verifyInput(pinIdInput, false, false, true, true)){
                    //TODO: maybe disable 'add'button
                }
            }
        });

        pinPadIdInput = new TextField();
        pinPadIdInput.setMaxWidth(dimInputPrefWidth);
        pinPadIdInput.setPromptText("pad-id");
        pinPadIdInput.setTooltip(new Tooltip("Pad number, as defined above"));
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
        posXinput.setTooltip(new Tooltip("Pin X position"));
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
        posYinput.setTooltip(new Tooltip("Pin Y position"));
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
            /* Add to mirror list */
            PadPosMirror newPos = new PadPosMirror( //args: String pinId, double xPos, double yPos, int padId, Package.Orientation rotation
                    pinIdInput.getText(),
                    Double.parseDouble(posXinput.getText()),
                    Double.parseDouble(posYinput.getText()),
                    Integer.parseInt(pinPadIdInput.getText()),
                    Package.orientationFromInt(Integer.parseInt((String)rotationBox.getValue()))
            );
            pdPositions.add(newPos);
            /* Add to real list */
            crntPkg.footPrints.get(fpIndex).padPositions.add(crntPkg.footPrints.get(fpIndex).new PadPosition(newPos));
            change("Added pad position");
            /* Reset input fields */
            pinIdInput.setText(predictPinId());
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
            posChecks();
        });
        positionAdditionBox.getChildren().addAll(pinIdInput, pinPadIdInput, posXinput, posYinput, rotationBox, addPosButton);

        posBranch.getChildren().addAll(padposTable, positionAdditionBox);
        padposholder.setContent(posBranch);

        return padposholder;
    }
    private int highestPadId(){
        int highest = 0;
        for(PadDimMirror p: pdDimensions){
            if(p.padId > highest){
                highest = p.padId;
            }
        }
        return highest;
    }
    private int highestPinId(){
        int highest = 0;
        for(PadPosMirror p: pdPositions){
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

    private String predictPinId(){
        int count = pdPositions.size();
        if(count < 1){
            return "";
        }
        String lastPin = pdPositions.get(count - 1).pinId;

        /* check if this pin has the specification of a grid */
        if(lastPin.length() > 0){
            char c = lastPin.charAt(0);
            if(Character.isAlphabetic(c)){
                lastPin = lastPin.substring(1);
                int nr = Integer.parseInt(lastPin);
                return String.format("%c%d", Character.toUpperCase(c), nr + 1);
            }
        }

        /* general case */
        return Integer.toString(highestPinId() + 1);
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

    private TitledPane initRefHolder(){
        TitledPane refholder = new TitledPane();
        refholder.setText("Normative specifications");
        refholder.setExpanded(false);
        final VBox refBranch = new VBox(SPACING_VBOX);

        final TableView<ReferenceMirror> refTable = new TableView<>();
        refTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        refTable.setEditable(true);
        refTable.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn, TableCell> cellFactory = (TableColumn p) -> new CustomCell();
        Callback<TableColumn, TableCell> cellDelFactory = (TableColumn p) -> new DelButtonCell(refTable);

        final TableColumn standardCol = new TableColumn("standard");
        //standardCol.setMinWidth(100);
        standardCol.setCellValueFactory(new PropertyValueFactory<>("standard"));
        standardCol.setCellFactory(cellFactory);
        standardCol.setOnEditCommit(
            new EventHandler<CellEditEvent<ReferenceMirror, String>>() {
                @Override
                public void handle(CellEditEvent<ReferenceMirror, String> t) {
                    ReferenceMirror ref = ((ReferenceMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    ref.getReference().standard = t.getNewValue();
                    ref.standard.set(t.getNewValue());
                    if(ref.getReference(backupPkg) == null || !ref.getReference().standard.equals(ref.getReference(backupPkg).standard)){
                        change("reference standard");
                    }
                }
            }
        );

        final TableColumn companyCol = new TableColumn("organization");
        //companyCol.setMinWidth(100);
        companyCol.setCellValueFactory(new PropertyValueFactory<>("company"));
        companyCol.setCellFactory(cellFactory);
        companyCol.setOnEditCommit(
            new EventHandler<CellEditEvent<ReferenceMirror, String>>() {
                @Override
                public void handle(CellEditEvent<ReferenceMirror, String> t) {
                    ReferenceMirror ref = ((ReferenceMirror) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    ref.getReference().company = t.getNewValue();
                    ref.company.set(t.getNewValue());
                    if(ref.getReference(backupPkg) == null || !ref.getReference().company.equals(ref.getReference(backupPkg).company)){
                        change("reference company");
                    }
                }
            }
        );

        TableColumn delCol = new TableColumn("");
        delCol.setMinWidth(50);
        delCol.setPrefWidth(50);
        delCol.setMaxWidth(50);
        delCol.setCellFactory(cellDelFactory);

        refTable.setItems(referenceList);
        refTable.getColumns().addAll(standardCol, companyCol, delCol);

        final HBox refAdditionBox = new HBox(SPACING_HBOX);
        refAdditionBox.setAlignment(Pos.CENTER_LEFT);
        final TextField standardInput = new TextField();
        standardInput.setPromptText("standard");
        final TextField companyInput = new TextField();
        companyInput.setPromptText("organization");

        final Button addButton = new Button("Add");
        addButton.setOnAction((ActionEvent e) -> {
            if(standardInput.getText().length() == 0){
                incompleteDataWarning.show(stage);
            } else{
                /* Add to mirror list */
                referenceList.add(new ReferenceMirror(
                    standardInput.getText(),
                    companyInput.getText()
                ));
                /* Add to real list */
                crntPkg.references.add(crntPkg.new Reference(standardInput.getText(), companyInput.getText()));
                change("Added reference");
            }
            standardInput.clear();
            companyInput.clear();
        });
        refAdditionBox.getChildren().addAll(standardInput, companyInput, addButton);

        refBranch.getChildren().addAll(refTable, refAdditionBox/* ,refDeletionBox */);
        refholder.setContent(refBranch);
        return refholder;
    }

    private TitledPane initRelPackHolder(){
        TitledPane relpaholder = new TitledPane();
        relpaholder.setText("Related Packages");
        relpaholder.setExpanded(false);
        final VBox relpaBranch = new VBox(SPACING_VBOX);

        final TableView<RelatedPack> relpaTable = new TableView<>();
        relpaTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        relpaTable.setEditable(false);

        Callback<TableColumn, TableCell> cellDelFactory = (TableColumn p) -> new DelButtonCell(relpaTable);

        relpaTable.setRowFactory(tv -> {
            TableRow<RelatedPack> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    RelatedPack rel = row.getItem();
                    int newIndex;

                    if(viewingSelection){
                        newIndex = rel.getIndex(viewedPackages);
                        if(newIndex == -1){
                            newIndex = rel.getIndex(allPackages);
                            setPackageSelection(false, newIndex);
                            selectionCanceledWarning.show(stage);
                        }
                        navigate(newIndex);
                    } else{
                        newIndex = rel.getIndex(allPackages);
                        navigate(newIndex);
                    }
                }
            });
            return row;
        });

        final TableColumn nameCol = new TableColumn("name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        final TableColumn descCol = new TableColumn("description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn delCol = new TableColumn("");
        delCol.setMinWidth(50);
        delCol.setPrefWidth(50);
        delCol.setMaxWidth(50);
        delCol.setCellFactory(cellDelFactory);

        relpaTable.setItems(relatedList);
        relpaTable.getColumns().addAll(nameCol, descCol, delCol);

        final HBox relpaAdditionBox = new HBox(SPACING_HBOX);
        relpaAdditionBox.setAlignment(Pos.CENTER_LEFT);
        final TextField nameInput = new TextField();
        nameInput.setPromptText("name");

        final Button addButton = new Button("Add");
        addButton.setOnAction((ActionEvent e) -> {
            if(nameInput.getText().length() == 0){
                incompleteDataWarning.show(stage);
            } else{
                if(notDuplicate(nameInput.getText())){
                    packageNotFound.show(stage);
                } else{
                    relatedList.add(new RelatedPack(nameInput.getText()));
                    if(crntPkg.relatedPackNames == null) crntPkg.relatedPackNames = new ArrayList();
                    crntPkg.relatedPackNames.add(nameInput.getText());
                    change("Added related package");
                }
            }
            nameInput.clear();
        });
        relpaAdditionBox.getChildren().addAll(nameInput, addButton, createHSpacer(), new Label("Double click a package to navigate to it"));

        relpaBranch.getChildren().addAll(relpaTable, relpaAdditionBox /*,refDeletionBox */);
        relpaholder.setContent(relpaBranch);
        return relpaholder;
    }

    private void initPopups(){
        /* the style String will be used for all Popups. */
        final String style = "-fx-padding: 16px 32px 8px 32px;";

        /* dupWarning is shown when attempting to add a name/alias/ipcname that already exists */
        dupWarning = initDupWarning(style);
        dupSoftWarning = new BasicPopup(style, "Cannot enter duplicate name", "close");

        /* delWarning is shown when clicking the delete button. It's a confirmation window */
        delWarning = initDelWarning(style);
        fpDelWarning = initfpDelWarning(style);

        /* aboutPopup shows some typical 'about' info*/
        aboutPopup = new AboutPopup(style);

        /* importPopups let the user know the result of the attempted import and bring them to the import scene if neccesary */
        impSuccess = initImpSucc(style);
        impConflicted = new ImportConflictPopup(style);
        impFailed = new BasicPopup(style, "Import failed.", "Close");

        /* warning popup that the selection has been canceled */
        String selectionCanceledText = "The package that you double-clicked on did not appear in the active selection.\n" +
                                       "Therefore, the selection was canceled (you are now viewing the full package list again).";
        selectionCanceledWarning = new BasicPopup(style, selectionCanceledText, "Close");

        /* warning that an item could not be added because some mandatory data was missing */
        String incompleteDataText = "The information could not be added, because one or more mandatory fields were left empty or undefined.\n" +
                                    "Please complete the data.";
        incompleteDataWarning = new BasicPopup(style, incompleteDataText, "Close");

        /* Notice that the name of the related package was not found */
        packageNotFound = new BasicPopup(style, "No package matched the entered name." , "Close");

        polyBuilder = new PolygonBuilder(style);
        vertexIdWarning = new BasicPopup(style, "Vertex ID must be unique", "Close");
        //notPolyWarn = new BasicPopup(style, "Selected pad shape is not a polygon.", "Close");

        previewPopup = new ImagePopup(style);
    }

    private DuplicateWarning initDupWarning(String style){
        DuplicateWarning dup = new DuplicateWarning();
        final VBox dupWaBranch = new VBox(SPACING_VBOX);
        dupWaBranch.setAlignment(Pos.CENTER);
        dupWaBranch.getStyleClass().add("message-box");
        dupWaBranch.setStyle(style);
        final Label warn = new Label("The name already exists as a\npackage name, alias or variant.");
        warn.setPadding(new Insets(0, 0, fieldSpacing, 0)); // add spacing below text (above buttons)

        final HBox btnBox = new HBox(SPACING_HBOX);
        btnBox.setAlignment(Pos.CENTER);
        final Button goLook = new Button("View");
        goLook.setOnAction((ActionEvent arg0) -> {
            navigate(dup.index());
            dup.hide();
        });
        final Button okButt = new Button("Close");
        okButt.setOnAction((ActionEvent arg0) -> {
            dup.hide();
        });
        btnBox.getChildren().addAll(goLook, okButt);
        dupWaBranch.getChildren().addAll(warn, btnBox);
        dup.getContent().add(dupWaBranch);
        return dup;
    }

    private Popup initDelWarning(String style){
        Popup del = new Popup();
        final VBox delWaBranch = new VBox(SPACING_VBOX);
        delWaBranch.setAlignment(Pos.CENTER);
        delWaBranch.getStyleClass().add("message-box");
        delWaBranch.setStyle(style);
        final Label notice = new Label("Delete package:\nthis operation cannot be undone.\n\nAre you sure?");

        final HBox btnBox = new HBox(SPACING_HBOX);
        final Button confirm = new Button(" Yes ");
        confirm.setOnAction((ActionEvent arg0) -> {
            if(viewedPackages.size() == 1){             //never delete final Package, just reset it unless viewing selection
                if(!viewingSelection){
                    crntPkg.reset();
                    navigate(crntIndex);
                    del.hide();
                } else if(viewingSelection && allPackages.size() > 1){
                    viewedPackages.remove(crntPkg);
                    allPackages.remove(crntPkg);
                    setPackageSelection(false, 0);
                    del.hide();
                }
            } else if(crntIndex == (viewedPackages.size() - 1)){    //if it's the last one than navigate one slot back
                viewedPackages.remove(crntPkg);
                allPackages.remove(crntPkg);
                if(!results.isEmpty()){
                    rectifySearchResults(crntIndex);
                }
                navigate(crntIndex - 1);
                del.hide();
            } else{
                viewedPackages.remove(crntPkg);
                allPackages.remove(crntPkg);
                if(!results.isEmpty()){
                    rectifySearchResults(crntIndex);
                }
                navigate(crntIndex);
                del.hide();
            }
            change("deleted package");
        });
        final Button cancel = new Button(" No ");
        cancel.setOnAction((ActionEvent arg0) -> {
            del.hide();
        });
        btnBox.getChildren().addAll(confirm, cancel);
        delWaBranch.getChildren().addAll(notice, btnBox);
        del.getContent().add(delWaBranch);
        return del;
    }

    private Popup initfpDelWarning(String style){
        Popup del = new Popup();
        final VBox delWaBranch = new VBox(SPACING_VBOX);
        delWaBranch.setAlignment(Pos.CENTER);
        delWaBranch.getStyleClass().add("message-box");
        delWaBranch.setStyle(style);
        final Label notice = new Label("Delete footprint:\nthis operation cannot be undone.\n\nAre you sure?");

        final HBox btnBox = new HBox(SPACING_HBOX);
        btnBox.setAlignment(Pos.CENTER);
        final Button confirm = new Button(" Yes ");
        confirm.setOnAction((ActionEvent arg0) -> {
            if(crntPkg.footPrints.size() == 1){             //never delete final Footprint, just reset it
                crntPkg.footPrints.get(fpIndex).reset();
                loadFootPrintInclusive();
                del.hide();
            } else if(fpIndex == (crntPkg.footPrints.size() - 1)){
                crntPkg.removeFootprint(crntPkg.footPrints.get(fpIndex));
                fpIndex -= 1;
                loadFootPrintInclusive();
                del.hide();
            } else{
                crntPkg.removeFootprint(crntPkg.footPrints.get(fpIndex));
                loadFootPrintInclusive();
                del.hide();
            }
            change("deleted footprint");
        });
        final Button cancel = new Button(" No ");
        cancel.setOnAction((ActionEvent arg0) -> {
            del.hide();
        });
        btnBox.getChildren().addAll(confirm, cancel);
        delWaBranch.getChildren().addAll(notice, btnBox);
        del.getContent().add(delWaBranch);
        return del;
    }

    private Popup initImpSucc(String style){
        Popup suc = new Popup();
        final VBox impSucBranch = new VBox(SPACING_VBOX);
        impSucBranch.setAlignment(Pos.CENTER);
        impSucBranch.getStyleClass().add("message-box");
        impSucBranch.setStyle(style);
        final Label sucMsg = new Label("Packages imported. No conflicts found.");
        final HBox btnBox = new HBox(SPACING_HBOX);
        btnBox.setAlignment(Pos.CENTER);
        final Button inspect = new Button("Inspect");
        inspect.setOnAction((ActionEvent arg0) -> {
            changeScene(importScene);
            suc.hide();
        });
        final Button addAll = new Button("Add all");
        addAll.setOnAction((ActionEvent arg0) -> {
            allPackages.addAll(loadedPackages);
            selectedPackages.clear();
            selectedPackages.addAll(loadedPackages);
            setPackageSelection(true, 0);
            suc.hide();
        });
        btnBox.getChildren().addAll(inspect, addAll);
        impSucBranch.getChildren().addAll(sucMsg, btnBox);
        suc.getContent().add(impSucBranch);
        return suc;
    }

    private class ImportConflictPopup extends Popup{
        Label confMsg;

        ImportConflictPopup(String style){
            super();
            final VBox impConfBranch;
            impConfBranch = new VBox(SPACING_VBOX);
            impConfBranch.setAlignment(Pos.CENTER);
            impConfBranch.getStyleClass().add("message-box");
            impConfBranch.setStyle(style);
            confMsg = new Label("--");

            final HBox btnBox = new HBox(SPACING_HBOX);
            btnBox.setAlignment(Pos.CENTER);
            final Button resolve = new Button("Resolve");
            resolve.setOnAction((ActionEvent arg0) -> {
                changeScene(importScene);
                this.hide();
            });
            final Button cancelImp = new Button("Cancel");
            cancelImp.setOnAction((ActionEvent arg0) -> {
                loadedPackages.clear();
                this.hide();
            });
            btnBox.getChildren().addAll(resolve, cancelImp);
            impConfBranch.getChildren().addAll(confMsg, btnBox);
            this.getContent().add(impConfBranch);
        }

        void update(int conflict_count){
            assert(conflict_count > 0);
            String message;
            if (conflict_count > 1)
                message = String.format("%d packages conflict with packages\n" +
                                        "that already exist in the file.",
                                        conflict_count);
            else
                message = "1 package conflicts with a package\n" +
                          "that already exists in the file.";
            message += "\n\nHow would you like to proceed?";
            confMsg.setText(message);
        }
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
        if(fileIsSet && changesWereMade){
            try{
                save(loadedFile);
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        /* get current size of the stage, and save this */
        double stageWidth = stage.getWidth();
        double stageHeight = stage.getHeight();
        Config.setWindowSize((int)stageWidth, (int)stageHeight);
        saveConfig(config);
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
            saveConfig(new Config(false, ""));
        } else {
            //Do nothing.
        }
    }
    private static boolean saveConfig(Config con){
        String configpath = usingWindows() ? windowsConfigPath() : linuxConfigPath();
        String json = Jsoner.serialize(con);
        ioTryCounter = 0;
        try{
            if(write(json, configpath)){
                return true;
            }
        } catch(IOException e){
            System.out.println("Failed to create config.json!");
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
            e.printStackTrace();
            System.out.println("Failed to load config.json!");
        }
        config = new Config(pathisset, tmpPath);
        Config.setWindowSize((int)prevWidth, (int)prevHeight);
    }

    /* Methods for creating and loading .json files */
    private boolean save(File file) throws IOException{ //if no list of Packages is supplied as an argument then save allPackages
        if(file == null){//prevents errors when user hits cancel on 'save as' window
            return false;
        }
        return save(file, allPackages, false);
    }
    private boolean save(File file, ArrayList<Package> list, boolean exportFlag) throws IOException{
        if(file == null){//prevents errors when user hits cancel on 'save as' window
            return false;
        }
        /* Auto-update if neccesary */
        if(!exportFlag && file.exists()){
            //print("known modify: " + lastModified);
            //print("file modify:  " + file.lastModified());
            if(lastModified < file.lastModified()){
                //auto-update
                if(!update()){
                    print("auto update failed");
                }
            }
        }
        /* check path */
        String path = file.getPath();

        /* Java objects to JSON String */
        String json = Jsoner.serialize(list); //maybe serialize separately for more control?

        /* pretty print */
        json = Jsoner.prettyPrint(json);

        /* JSON String to JSON file */
        ioTryCounter = 0;
        boolean success = write(json, path);
        if(!success){
            return false;
        } else{
            if(!exportFlag){
                lastModified = file.lastModified();
                changesWereMade = false;
            }
            return true;
        }
    }

    private boolean loadedFileChanged(){
        return lastModified < loadedFile.lastModified();
    }

    private static boolean write(String content, String path) throws IOException{
        try (RandomAccessFile writer = new RandomAccessFile(path, "rw")) {
            byte[] bytes = content.getBytes("UTF-8");
            FileLock lock = writer.getChannel().lock();
            writer.write(bytes);
            writer.setLength(bytes.length);
            lock.release();
        } catch(IOException e){
            if(ioTryCounter <= IO_RETRIES){   //retry
                ++ioTryCounter;
                try {
                    Thread.sleep(100 + ioRandomDelay.nextInt(100));
                    return write(content, path);
                } catch (InterruptedException ex) {
                    return write(content, path);
                }
            }
            System.out.println("Failed to write file");
            return false;
        }
        return true;
    }

    /* Fill loadedPackages and replace allPackages with it */
    private boolean loadMainList(File file){
        ioTryCounter = 0;
        if(loadPackages(file)){
            allPackages.clear();
            allPackages.addAll(loadedPackages);
            setPackageSelection(false, 0);
            changesWereMade = false;
            return true;
        } else{
            return false;
        }
    }

    /* Fill loadedPackages, and fill observablelists with ImportedPackages */
    private int importPackages(File file){
        ioTryCounter = 0;
        if(loadPackages(file)){
            cleanImportPacks.clear();
            conflictedImportPacks.clear();
            boolean conflictFlag = false;
            for(Package p: loadedPackages){
                ImportedPackage imp = new ImportedPackage(p);
                if(imp.checkConflicted()){
                    conflictFlag = true;
                } else{
                    imp.setSelected(true);
                    cleanImportPacks.add(imp);
                }
            }
            if(conflictFlag){
                return 1;   //if Packages are loaded but there are duplicate conflicts
            } else{
                return 0;   //if all Packages are loaded without conflicts
            }
        } else{
            return -1;      //if loading is not successful
        }
    }

    /* Import new Packages and replace Packages that have newer versions */
    private boolean update(){
        if(loadPackages(loadedFile)){
            for(Package p: loadedPackages){
                Package localPack = retrieveByName(p.names);
                if(localPack == null){  //if there is no local version of a Package in the file, just add it
                    allPackages.add(p);
                } else{
                    //print("local timestamp: " + localPack.dateModified);
                    //print("file timestamp : " + p.dateModified);
                    if(localPack.dateModified < p.dateModified){
                        allPackages.set(allPackages.indexOf(localPack), p);
                        print(localPack.names[0] + " was replaced with a newer version.");
                    }
                }
            }
            loadAll();
            return true;
        } else{
            return false;
        }
    }

    /* Deserialize JSON to fill loadedPackages */
    private boolean loadPackages(File file){
        if(file == null){          //prevents errors when user hits cancel on 'open file' window
            return false;
        }
        try{
            RandomAccessFile raFile = new RandomAccessFile(file, "r");
            //lock
            FileLock lock = raFile.getChannel().lock(0, Long.MAX_VALUE, true);
            // read all bytes
            int length = (int)raFile.length();
            byte[] bytes = new byte[length];
            raFile.read(bytes, 0, length);

            //unlock & close
            lock.release();
            raFile.close();

            // convert bytes to string
            String content = new String(bytes, "UTF-8");

            JsonArray deserialize = (JsonArray) Jsoner.deserialize(content);
            ArrayList<JsonObject> jsonList = new ArrayList<>();
            loadedPackages.clear();
            deserialize.asCollection(jsonList);
            if(jsonList.isEmpty()){
                //System.out.println("json objects not loaded");
                return false;
            }

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
                    newpac.specPacks = new ArrayList<>();
                    for(SpepaMirror spep : loadedSpepas){
                        newpac.specPacks.add(newpac.new Variant(spep));
                    }
                }

                /* footprints[] */
                ArrayList<JsonObject> jsnFootprints = obj.getCollection(new SimpleKey("footprints"));
                newpac.footPrints = new ArrayList<>();
                for(JsonObject jsnFootprint : jsnFootprints){
                    int index = jsnFootprints.indexOf(jsnFootprint);

                    newpac.footPrints.add(newpac.new Footprint());
                    newpac.footPrints.get(index).ftprntType = Package.footprintTypefromString(jsnFootprint.getStringOrDefault(new StringKey("type")));

                    /* Span span */
                    JsonObject jsnspan = (JsonObject)jsnFootprint.getMapOrDefault(new SimpleKey("span"));
                    if(jsnspan != null){
                        newpac.footPrints.get(index).span.x = jsnspan.getDoubleOrDefault(new DoubleKey("cx"));
                        newpac.footPrints.get(index).span.y = jsnspan.getDoubleOrDefault(new DoubleKey("cy"));
                    }

                    /* Outline / contour */
                    JsonObject jsnotl = (JsonObject)jsnFootprint.getMapOrDefault(new SimpleKey("contour"));
                    if(jsnotl != null){
                        newpac.footPrints.get(index).outline.length = jsnotl.getDoubleOrDefault(new DoubleKey("cx"));
                        newpac.footPrints.get(index).outline.width = jsnotl.getDoubleOrDefault(new DoubleKey("cy"));
                        newpac.footPrints.get(index).outline.orgX = jsnotl.getDoubleOrDefault(new DoubleKey("x"));
                        newpac.footPrints.get(index).outline.orgY = jsnotl.getDoubleOrDefault(new DoubleKey("y"));
                    }


                    /* padDimensions[] */
                    ArrayList<JsonObject> jsnDimensions = jsnFootprint.getCollectionOrDefault(new SimpleKey("pad-shapes"));
                    ArrayList<PadDimMirror> loadedDimensions = new ArrayList<>();
                    if(jsnDimensions != null){
                        Polygon tempPoly = null;
                        for(JsonObject jsnDim : jsnDimensions){
                            JsonObject jsnPoly = (JsonObject)jsnDim.getMapOrDefault(new SimpleKey("polygon"));
                            if(jsnPoly != null){
                                tempPoly = new Polygon();
                                ArrayList<JsonObject> jsnVertices = jsnPoly.getCollectionOrDefault(new SimpleKey("vertices"));
                                for(JsonObject jsnVertix : jsnVertices){
                                    tempPoly.addVertex(
                                            jsnVertix.getDoubleOrDefault(new DoubleKey("x")),
                                            jsnVertix.getDoubleOrDefault(new DoubleKey("y")),
                                            jsnVertix.getIntegerOrDefault(new IntegerKey("id"))
                                    );
                                }
                            }
                            PadDimMirror dim = new PadDimMirror(
                                jsnDim.getIntegerOrDefault(new IntegerKey("pad-id")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("cx")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("cy")),
                                Package.padShapefromString(jsnDim.getStringOrDefault(new StringKey("shape"))),
                                jsnDim.getDoubleOrDefault(new DoubleKey("hole-diameter")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("x")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("y")),
                                Package.padTypeFromString(jsnDim.getStringOrDefault(new StringKey("pad-type"))),
                                tempPoly
                            );
                            loadedDimensions.add(dim);
                        }
                        newpac.footPrints.get(index).dimensions = new ArrayList<>();
                        for(PadDimMirror dim : loadedDimensions){
                            newpac.footPrints.get(index).dimensions.add(newpac.footPrints.get(index).new PadDimension(dim));
                        }
                    }

                    /* padPositions[] */
                    ArrayList<JsonObject> jsnPositions = jsnFootprint.getCollectionOrDefault(new SimpleKey("pad-positions"));
                    ArrayList<PadPosMirror> loadedPositions = new ArrayList<>();
                    if(jsnPositions != null){
                        for(JsonObject jsnDim : jsnPositions){
                            PadPosMirror pos = new PadPosMirror(
                                jsnDim.getStringOrDefault(new StringKey("pin-id")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("x")),
                                jsnDim.getDoubleOrDefault(new DoubleKey("y")),
                                jsnDim.getIntegerOrDefault(new IntegerKey("pad-id")),
                                Package.orientationFromInt(jsnDim.getIntegerOrDefault(new IntegerKey("rotation")))
                            );
                            loadedPositions.add(pos);
                        }
                        newpac.footPrints.get(index).padPositions = new ArrayList<>();
                        for(PadPosMirror pos : loadedPositions){
                            newpac.footPrints.get(index).padPositions.add(newpac.footPrints.get(index).new PadPosition(pos));
                        }
                    }
                }
                /* references[] */
                ArrayList<JsonObject> jsnReferences = obj.getCollection(new SimpleKey("references"));
                if(jsnReferences != null){
                    newpac.references = new ArrayList<>();
                    for(JsonObject jsnReference : jsnReferences){
                        newpac.references.add(newpac.new Reference(
                                jsnReference.getStringOrDefault(new DoubleKey("standard")),
                                jsnReference.getStringOrDefault(new DoubleKey("organization"))
                        ));
                    }
                }
                /* Related Package names */
                ArrayList<String> jsnRelatedList = obj.getCollectionOrDefault(new SimpleKey("related packages"));
                if(jsnRelatedList != null){
                    newpac.relatedPackNames = new ArrayList(jsnRelatedList);
                }

                /* Date of last modification */
                String datestr = obj.getStringOrDefault(new DateKey("date-modified"));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date datestamp = sdf.parse(datestr);
                    newpac.dateModified = datestamp.getTime();
                } catch (Exception e) {
                    newpac.dateModified = 0;
                }

                /* After filling all the data fields in newPac, add it to list */
                loadedPackages.add(newpac);
            }
            Collections.sort(loadedPackages, Package.FirstName);
        } catch(JsonException | IOException e){
            if(ioTryCounter <= IO_RETRIES){   //retry
                ++ioTryCounter;
                try {
                    Thread.sleep(100 + ioRandomDelay.nextInt(100));
                    return loadPackages(file);
                } catch (InterruptedException ex) {
                    return loadPackages(file);
                }
            }
            System.out.println("something went wrong reading json");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /* 6 */
    /* This method swaps crntPackage with another Package from viewedPackages*/
    private void navigate(int newIndex){
        /* Auto-save if neccesary */
        if(fileIsSet){
            if(loadedFileChanged()){
                update();
            }
            if(changesWereMade){
                try{
                    if(!save(loadedFile)){
                        System.out.println("auto-save failed");
                    }
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }
        crntPkg = viewedPackages.get(newIndex);
        backupPkg.copy(crntPkg);
        crntIndex = newIndex;
        fpIndex = 0; //just display the first footprint when loading a Package
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
        loadImage(mainCanvas, mainGc);
    }
    private void loadImage(Canvas canvas, GraphicsContext gc){
        prepImage(canvas, gc); //clears, whitens, draws axes

        double factor = setScale(canvas);

        /* draw scale indicator */
        drawIndicator(canvas, gc, factor);

        /* draw body */
        double scaledWidth = scale(crntPkg.body.bodyX, factor);
        double scaledHeight = scale(crntPkg.body.bodyY, factor);
        double drawOrgX = imgXcenter(canvas) + offset(scaledWidth) + scale(crntPkg.body.bodyOrgX, factor);
        double drawOrgY = imgYcenter(canvas) + offset(scaledHeight) - scale(crntPkg.body.bodyOrgY, factor);

        gc.setStroke(Color.color(0, 0.2, 0.4));
        gc.setLineWidth(1.5);
        gc.strokeRect(drawOrgX, drawOrgY, scaledWidth, scaledHeight);  //X, Y, W, H

        /* draw lead-to-lead */
        if(crntPkg.lead2lead.x > crntPkg.body.bodyX + 0.1 || crntPkg.lead2lead.y > crntPkg.body.bodyY + 0.1){
            scaledWidth = scale(crntPkg.lead2lead.x, factor);
            scaledHeight = scale(crntPkg.lead2lead.y, factor);
            drawOrgX = imgXcenter(canvas) + offset(scaledWidth) + scale(crntPkg.lead2lead.orgX, factor);
            drawOrgY = imgYcenter(canvas) + offset(scaledHeight) - scale(crntPkg.lead2lead.orgY, factor);

            gc.setGlobalAlpha(0.7);
            gc.setStroke(Color.color(0, 0.2, 0.4));
            gc.setLineWidth(0.75);
            gc.setLineDashes(3);
            gc.strokeRect(drawOrgX, drawOrgY, scaledWidth, scaledHeight);  //X, Y, W, H
            gc.setLineDashes(null);
        }

        /* draw pads */
        gc.setGlobalAlpha(0.7);
        for(PadDimMirror dim : pdDimensions){

            ArrayList<PadPosMirror> drawnPads = new ArrayList();    //fill a list with padpositions that match the pad id
            for(PadPosMirror pad : pdPositions){
                if(pad.padId == dim.padId){
                    drawnPads.add(pad);
                }
            }
            for(PadPosMirror pad : drawnPads){
                Color padcolor = Color.BLACK;   /* to avoid "might not have been initialized" warning */
                switch(dim.padType){
                    case STANDARD: padcolor = Color.color(1.0, 0.1, 0.2);   break;
                    case EXPOSED: padcolor = Color.color(0.9, 0.3, 0.1);    break;
                    case MECHANICAL: padcolor = Color.color(0.9, 0.4, 0.0); break;
                }
                gc.setGlobalAlpha(0.5);
                gc.setFill(padcolor);

                Polygon padPoly = Polygon.FromRect(dim.width, dim.length);

                if(dim.shape == Package.PadShape.POLYGON){
                    Polygon tmpPol = dim.retrievePolyCopy();
                    if(tmpPol != null) padPoly = tmpPol;
                }
                padPoly.move(-dim.originX, -dim.originY);               /* apply origin of the pad shape */
                padPoly.rotate(Package.orientationAsInt(pad.rotation)); /* apply pad rotation */
                padPoly.move(pad.xPos, pad.yPos);                       /* add pad offset from the body */
                padPoly.Scale(factor);                                  /* scale pad dimensions & position for drawing */
                padPoly.Flip(Polygon.FlipType.FLIP_Y);                  /* toggle Y-axis */
                padPoly.move(imgXcenter(canvas), imgYcenter(canvas));               /* reposition relative to centre of drawing */

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
                        arcWidth = scale(smallestDim(dim), factor)*0.67;
                        arcHeight = scale(smallestDim(dim), factor)*0.67;
                        gc.fillRoundRect(padPoly.Left(), padPoly.Bottom(), scaledPadWidth, scaledPadHeight, arcWidth, arcHeight);
                        break;
                    case OBROUND:
                        arcWidth = scale(smallestDim(dim), factor);
                        arcHeight = scale(smallestDim(dim), factor);
                        gc.fillRoundRect(padPoly.Left(), padPoly.Bottom(), scaledPadWidth, scaledPadHeight, arcWidth, arcHeight);
                        break;
                    case POLYGON:
                        Polygon.Drawable pd = padPoly.getDrawable();
                        gc.fillPolygon(pd.xPoints, pd.yPoints, pd.nPoints);
                }
                /* Draw pad holes */
                if(!roughCompare(dim.holeDiam, 0)){
                    gc.setStroke(padcolor);
                    gc.setFill(Color.WHITE);
                    gc.setGlobalAlpha(1.0);
                    double holeX = imgXcenter(canvas) + offset(scale(dim.holeDiam, factor)) + scale(pad.xPos, factor);
                    double holeY = imgYcenter(canvas) + offset(scale(dim.holeDiam, factor)) - scale(pad.yPos, factor);
                    gc.fillOval(holeX, holeY, scale(dim.holeDiam, factor), scale(dim.holeDiam, factor));
                    gc.strokeOval(holeX, holeY, scale(dim.holeDiam, factor), scale(dim.holeDiam, factor));
                }
            }
        }
    }
    /* image support methods */
    private void prepImage(Canvas canvas, GraphicsContext gc){
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); //wipe canvas
        gc.setGlobalAlpha(1.0);     //set brush opacity (back) to full

        /* make background white*/
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        /* draw Y axis */
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.2);
        gc.strokeLine(imgXcenter(canvas), 0, imgXcenter(canvas), canvas.getHeight()); // startX, startY, endX, endY
        /* draw X axis */
        gc.strokeLine(0, imgYcenter(canvas), canvas.getWidth(), imgYcenter(canvas));
    }
    private void drawIndicator(Canvas canvas, GraphicsContext gc, double factor){
        gc.setLineWidth(1);
        double indicWidth = scale(1, factor);
        double indicXstart = 8;
        double indicXend = indicXstart + indicWidth;
        double indicY = canvas.getHeight() - (canvas.getHeight()/40) - 2; //set at XX% of window height
        gc.strokeLine(indicXstart, indicY, indicXend, indicY); //main line
        gc.strokeLine(indicXstart, indicY - 4, indicXstart, indicY + 4); //start clarity line
        gc.strokeLine(indicXend, indicY - 4, indicXend, indicY + 4); //end clarity line
        gc.setFill(Color.BLACK);
        gc.fillText("1mm", indicXend + 4.5, indicY + 3.5, 20); //draw "1mm" text
    }
    private double imgXcenter(Canvas canvas){
        return canvas.getWidth()/2;
    }
    private double imgYcenter(Canvas canvas){
        return canvas.getHeight()/2;
    }
    private double setScale(Canvas canvas){
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
        bbox.AddBoundingBox(crntPkg.footPrints.get(fpIndex).outline.length,
                            crntPkg.footPrints.get(fpIndex).outline.width,
                            crntPkg.footPrints.get(fpIndex).outline.orgX,
                            crntPkg.footPrints.get(fpIndex).outline.orgY);

        return setScale(canvas, bbox);
    }
    private double setScale(Canvas canvas, PadDimMirror dim){
        Polygon source = dim.retrievePolyCopy();
        Polygon toAppend = dim.retrievePolyCopy();
        toAppend.move(-dim.originX, -dim.originY);
        source.AppendPolygon(toAppend);
        //generate courtyard
        Courtyard bbox = new Courtyard(source);

        return setScale(canvas, bbox);
    }
    private double setScale(Canvas canvas, Courtyard bbox){
        //TODO: fix for polygons that have an origin offset in pad shape
        /* cx & cy are half of the required courtyard span, in mm */
        double cx = Math.max(-bbox.Left(), bbox.Right());
        double cy = Math.max(-bbox.Bottom(), bbox.Top());

        /* calculate the scale factor for width & height, and pick the smallest one */
        final int margin = 8;   /* margin on all sides of the footprint */
        double scale_x = (canvas.getWidth() - 2 * margin) / (2 * cx);
        double scale_y = (canvas.getHeight() - 2 * margin) / (2 * cy);
        double scaleFactor = Math.min(scale_x, scale_y);

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
        return scaleFactor;
    }
    private double smallestDim(PadDimMirror dim){
        return dim.length <= dim.width ? dim.length : dim.width;
    }
    private double scale(double input, double scaleFactor){ //TODO set variable scaling
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
        pinNumber.setText(Integer.toString(crntPkg.nrOfPins));
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

    private void loadSpePacks(){
        loadSpePacks(crntPkg, spePacks);
    }
    private void loadSpePacks(Package source, List<SpepaMirror> dest){
        if(!dest.isEmpty()){        //if this list is not empty, empty it
            dest.clear();
        }
        if(source.specPacks != null){
            for(Package.Variant spepa : source.specPacks){
                dest.add(new SpepaMirror(spepa));
            }
        }
    }
    private void loadImpVariants(Package source, ObservableList<ImportedVariant> dest){
        dest.clear();
        ArrayList<SpepaMirror> convert = new ArrayList();
        loadSpePacks(source, convert);
        for(SpepaMirror spep : convert){
            dest.add(new ImportedVariant(spep));
        }
    }

    private void loadFootPrint(){
        fpIndexLabel.setText(Integer.toString(fpIndex + 1) + " of " + Integer.toString(crntPkg.footPrints.size()));
        footprinttypeBox.setValue(Package.footprintTypeasString(crntPkg.footPrints.get(fpIndex).getType()));
        spanXField.setText(Double.toString(crntPkg.footPrints.get(fpIndex).span.x));
        spanYField.setText(Double.toString(crntPkg.footPrints.get(fpIndex).span.y));
        fpolLength.setText(Double.toString(crntPkg.footPrints.get(fpIndex).outline.length));
        fpolWidth.setText(Double.toString(crntPkg.footPrints.get(fpIndex).outline.width));
        fpolOrgX.setText(Double.toString(crntPkg.footPrints.get(fpIndex).outline.orgX));
        fpolOrgY.setText(Double.toString(crntPkg.footPrints.get(fpIndex).outline.orgY));
    }

    private void loadDimensions(){
        if(!pdDimensions.isEmpty()){
            pdDimensions.clear();
        }
        if(crntPkg.footPrints.get(fpIndex).dimensions != null){
            for(Package.Footprint.PadDimension dimension : crntPkg.footPrints.get(fpIndex).dimensions){
                pdDimensions.add(new PadDimMirror(dimension));
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
        if(crntPkg.footPrints.get(fpIndex).padPositions != null){
            for(Package.Footprint.PadPosition position : crntPkg.footPrints.get(fpIndex).padPositions){
                pdPositions.add(new PadPosMirror(position));
            }
        }
        if(pinIdInput != null){ //the first time this method is called the UI elements will not have been initialized
            pinIdInput.setText(Integer.toString(highestPinId() + 1));
        }
        if(pinPadIdInput != null){
            pinPadIdInput.setText("1"); //padId '2' is usually for exposed pads for which there will only be one
        }
    }

    private void loadReferences(){
        if(!referenceList.isEmpty()){
            referenceList.clear();
        }
        if(crntPkg.references != null){
            for(Package.Reference ref : crntPkg.references){
                referenceList.add(new ReferenceMirror(ref));
            }
        }
    }

    private void loadRelatedPacks(){
        if(!relatedList.isEmpty()){
            relatedList.clear();
        }
        if(crntPkg.relatedPackNames != null){
            for(String s : crntPkg.relatedPackNames){
                relatedList.add(new RelatedPack(s));
            }
        }
    }

    private void loadFootPrintInclusive(){
        loadFootPrint();
        loadDimensions();
        loadPositions();
        loadImage();
        checkSpan();
    }

    private void loadAll(){
        updateIndex();
        nameBox.load(crntPkg);
        loadDescription();
        loadCharacteristics();
        loadBodySize();
        loadLeadToLead();
        loadSpePacks();
        loadFootPrintInclusive();
        loadReferences();
        loadRelatedPacks();

        /* check dimensions are loading everything */
        checkBodySize();
        checkLeadToLead();
        checkContour();
    }

    private void updateVariants(List<SpepaMirror> source, Package dest){
        dest.specPacks = new ArrayList();
        for(SpepaMirror spep: source){
            dest.specPacks.add(dest.new Variant(spep.spepaName, spep.standard, spep.minHeight, spep.maxHeight, spep.padExposed, spep.spepaNotes));
        }
    }
    private void dimChecks(){
        loadImage();
        checkContour();
    }
    private void posChecks(){
        loadImage();
        checkSpan();
        checkContour();
    }

    private void checkBodySize(){
        try {
            String warnText = "This value should not be zero.";

            boolean warn = roughCompare(crntPkg.body.bodyX, 0);
            bodyXsize.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
            String ttipText = "Horizontal size of the body (excluding pins).";
            if(warn){
                ttipText += "\n" + warnText;
            }
            final Tooltip bodyXtip = new Tooltip(ttipText);
            bodyXsize.setTooltip(bodyXtip);

            warn = roughCompare(crntPkg.body.bodyY, 0);
            bodyYsize.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
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
            ltolXsize.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
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
            ltolYsize.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
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
            PadPosMirror pin1 = pdPositions.get(idx1);
            for(int idx2 = idx1 + 1; idx2 < pdPositions.size(); idx2++){
                PadPosMirror pin2 = pdPositions.get(idx2);
                if(pin1.padId != pin2.padId)
                    continue;   /* only consider pads with the same pad-id */
                if (roughCompare(pin1.yPos, pin2.yPos)){
                    /* pins are horizontally aligned, check span between them */
                    double span = Math.abs(pin1.xPos - pin2.xPos);
                    if(roughCompare(span, crntPkg.footPrints.get(fpIndex).span.x)){
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
                    if(roughCompare(span, crntPkg.footPrints.get(fpIndex).span.y)){
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
        if(!roughCompare(crntPkg.footPrints.get(fpIndex).span.x, 0)){
            for(int idx = 0; idx < padflag.length; idx++){
                if((padflag[idx] & mismatchSpanX) != 0){
                    warn_span_x = true;
                }
            }
        }

        boolean warn_span_y = false;
        if(!roughCompare(crntPkg.footPrints.get(fpIndex).span.y, 0)){
            for(int idx = 0; idx < padflag.length; idx++){
                if((padflag[idx] & mismatchSpanY) != 0){
                    warn_span_y = true;
                }
            }
        }

        spanXField.setStyle("TextFieldBkgnd:" + (warn_span_x ? "#fff0a0;" : "white;"));
        String ttipText = "Distance between the left & right rows (pad centres)";
        if(warn_span_x){
            ttipText += "\nMismatch between this value and the pad definitions";
        }
        final Tooltip spanXFieldTip = new Tooltip(ttipText);
        spanXField.setTooltip(spanXFieldTip);

        spanYField.setStyle("TextFieldBkgnd:" + (warn_span_y ? "#fff0a0;" : "white;"));
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
        for(PadPosMirror pin: pdPositions){
            /* find pad definition (ignore any pin for which no pad definition cannot be found) */
            for(PadDimMirror pad: pdDimensions){
                if(pad.padId == pin.getPadIdppt()){
                    Polygon padPoly;
                    if(pad.shape == Package.PadShape.POLYGON && pad.verMirList != null){
                        padPoly = new Polygon();
                        pad.verMirList.forEach(vm -> {
                            padPoly.addVertex(vm.x.get(), vm.y.get());
                        });
                    } else {
                        padPoly = Polygon.FromRect(pad.width, pad.length);
                    }
                    padPoly.move(-pad.originX, -pad.originY);               /* apply origin */
                    padPoly.rotate(Package.orientationAsInt(pin.rotation)); /* apply pad rotation */
                    padPoly.move(pin.xPos, pin.yPos);                       /* add pin offset */
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

        warn = !roughCompare(crntPkg.footPrints.get(fpIndex).outline.length, cx);
        fpolLength.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Horizontal dimension of the contour of the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the bounding box around the pads (" + Double.toString(Math.round(cx*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolLengthTip = new Tooltip(ttipText);
        fpolLength.setTooltip(fpolLengthTip);

        warn = !roughCompare(crntPkg.footPrints.get(fpIndex).outline.width, cy);
        fpolWidth.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Vertical dimension of the contour of the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the bounding box around the pads (" + Double.toString(Math.round(cy*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolWidthTip = new Tooltip(ttipText);
        fpolWidth.setTooltip(fpolWidthTip);

        double ox = (contourLeft + contourRight) / 2;
        double oy = (contourBottom + contourTop) / 2;

        warn = !roughCompare(crntPkg.footPrints.get(fpIndex).outline.orgX, ox);
        fpolOrgX.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Horizontal offset of the origin from the geometric centre the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the origin calculated from the pads (" + Double.toString(Math.round(ox*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolOrgXTip = new Tooltip(ttipText);
        fpolOrgX.setTooltip(fpolOrgXTip);

        warn = !roughCompare(crntPkg.footPrints.get(fpIndex).outline.orgY, oy);
        fpolOrgY.setStyle("TextFieldBkgnd:" + (warn ? "#fff0a0;" : "white;"));
        ttipText = "Vertical offset of the origin from the geometric centre the footprint.";
        if(warn){
            ttipText += "\nThis value does not match the origin calculated from the pads (" + Double.toString(Math.round(oy*roundFactor)/roundFactor) + ")";
        }
        final Tooltip fpolOrgYTip = new Tooltip(ttipText);
        fpolOrgY.setTooltip(fpolOrgYTip);
    }


    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Import functionality methods !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */

    private Scene initImportScene(){
        final ScrollPane root = new ScrollPane();
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        final ObservableList<ImportedVariant> importVariants = FXCollections.observableArrayList();

        final VBox importBranch = new VBox(SPACING_VBOX);
        importBranch.setPadding(new Insets(fieldSpacing, fieldSpacing, fieldSpacing, fieldSpacing));
        importBranch.setFillWidth(true);
        importBranch.prefWidthProperty().bind(root.widthProperty());
        final HBox buttonBox = new HBox(SPACING_HBOX);
        buttonBox.setPadding(new Insets(fieldSpacing, 0, 0, fieldSpacing));
        final Button cancelBut = new Button("Cancel");
        cancelBut.setOnAction((ActionEvent arg0) -> {
            loadedPackages.clear();
            importVariants.clear();
            changeScene(mainScene);
        });
        buttonBox.getChildren().addAll(createHSpacer(), cancelBut);

        final HBox tableBox = new HBox(SPACING_HBOX);

        //tableview for clean (no conflict) ImportedPackages, with checkbox to mark whether to add to main list
        final VBox cleanBox = new VBox(SPACING_VBOX);
        final TableView<ImportedPackage> cleanImportsTable = new TableView<>();
        cleanImportsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cleanImportsTable.setMinWidth(150);
        cleanImportsTable.setPrefHeight(prevHeight * 0.75);
        cleanImportsTable.setEditable(true);

        TableColumn nameCol = new TableColumn("Non-conflicting\nor Resolved");
        nameCol.setEditable(false);
        nameCol.setMinWidth(100);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn selectCol = new TableColumn();
        selectCol.setMinWidth(25);
        selectCol.setEditable(true);
        selectCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectCol.setCellFactory(new Callback<TableColumn<ImportedPackage,Boolean>,TableCell<ImportedPackage,Boolean>>(){
            @Override public TableCell<ImportedPackage,Boolean> call( TableColumn<ImportedPackage,Boolean> p ){
                CheckBoxTableCell<ImportedPackage, Boolean> cbt = new CheckBoxTableCell<>();
                cbt.setSelectedStateCallback(new Callback<Integer, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(Integer index) {
                        return cleanImportsTable.getItems().get(index).selectedProperty();
                    }
                });
                return cbt;
            }
        });
        CheckBox selectAll = new CheckBox("all");
        selectCol.setGraphic(selectAll);
        selectAll.setOnAction(e -> {
            for (ImportedPackage imp : cleanImportPacks) {
                imp.setSelected(((CheckBox) e.getSource()).isSelected());
            }
        });

        cleanImportsTable.setItems(cleanImportPacks);
        cleanImportsTable.getColumns().addAll(nameCol, selectCol);
        final Button confirmButton = new Button("Confirm imports");
        confirmButton.setOnAction((ActionEvent arg0) -> {
            selectedPackages.clear();
            for(ImportedPackage p : cleanImportPacks){
                if(p.selectedProperty().get()){
                    allPackages.add(p.pack);
                    selectedPackages.add(p.pack);
                }
            }
            if(!selectedPackages.isEmpty()){
                setPackageSelection(true, 0);
            }
            changeScene(mainScene);
            loadedPackages.clear();
            importVariants.clear();
        });
        cleanBox.getChildren().addAll(cleanImportsTable, confirmButton);

        /* tableview for conflicted ImportedPackages */
        final VBox conflictedBox = new VBox(SPACING_VBOX);
        final TableView<ImportedPackage> conflictedImportsTable = new TableView<>();
        conflictedImportsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        conflictedImportsTable.setMinWidth(150);
        conflictedImportsTable.setMaxWidth(250);
        conflictedImportsTable.setPrefHeight(prevHeight * 0.75);
        conflictedImportsTable.setEditable(false);

        TableColumn confNameCol = new TableColumn("Conflicting\n");
        confNameCol.setEditable(false);
        confNameCol.setMinWidth(150);
        confNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        conflictedImportsTable.setItems(conflictedImportPacks);
        conflictedImportsTable.getColumns().addAll(confNameCol);
        final Label inspectLabel = new Label("Select a Package\nto inspect it");
        conflictedBox.getChildren().addAll(conflictedImportsTable, inspectLabel);

        final VBox inspectionBox = new VBox(SPACING_VBOX);
        final dtfManagerImports inspectionManager = new dtfManagerImports(4);   //not as much space for it so 4 fields per row

        conflictedImportsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                inspectedImport = conflictedImportsTable.getSelectionModel().getSelectedItem();
                inspectionManager.load(inspectedImport);
                inspectedPackage = inspectedImport.pack;
                loadImpVariants(inspectedPackage, importVariants);
            }
        });

        /* tableview for selected Package variants */
        TableView<ImportedVariant> spepaTable = new TableView<>();
        spepaTable.setMinWidth(360);
        spepaTable.setPrefHeight(prevHeight * 0.75 - 30);
        spepaTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        spepaTable.setEditable(true);
        spepaTable.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn, TableCell> cellFactory = (TableColumn p) -> new CustomCell();
        Callback<TableColumn, TableCell> cellDoubleFactory = (TableColumn p) -> new EditingDoubleCell();

        final int IMP_THIN_COLUMN = 50;

        TableColumn ipcNameCol = new TableColumn("name");
        ipcNameCol.setMinWidth(IMP_THIN_COLUMN);
        ipcNameCol.setCellValueFactory(new PropertyValueFactory<>("spepaNameppt"));
        ipcNameCol.setCellFactory(cellFactory);
        ipcNameCol.setOnEditCommit(
            new EventHandler<CellEditEvent<ImportedVariant, String>>() {
                @Override
                public void handle(CellEditEvent<ImportedVariant, String> t) {
                    ImportedVariant imp = (t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    imp.spep.getVariant(inspectedPackage).variantName = t.getNewValue();
                    imp.setIpcName(t.getNewValue());
                    imp.checkConflict();
                    boolean nameConflict = inspectedImport.checkConflicted();
                    if(!nameConflict && noConflicts(importVariants)){
                        importVariants.clear();
                        /* remove selected item from the "conflicting" table */
                        inspectedImport = conflictedImportsTable.getSelectionModel().getSelectedItem();
                        conflictedImportPacks.remove(inspectedImport);
                        /* add it to the "resolved" table */
                        inspectedImport.setSelected(true);
                        cleanImportPacks.add(inspectedImport);
                    }
                }
            }
        );

        TableColumn minHeightCol = new TableColumn("min\nheight");
        minHeightCol.setEditable(false);
        minHeightCol.setMinWidth(IMP_THIN_COLUMN);
        minHeightCol.setMaxWidth(IMP_THIN_COLUMN);
        minHeightCol.setCellValueFactory(new PropertyValueFactory<>("minHeightppt"));
        minHeightCol.setCellFactory(cellDoubleFactory);

        TableColumn maxHeightCol = new TableColumn("max\nheight");
        maxHeightCol.setEditable(false);
        maxHeightCol.setMinWidth(IMP_THIN_COLUMN);
        maxHeightCol.setMaxWidth(IMP_THIN_COLUMN);
        maxHeightCol.setCellValueFactory(new PropertyValueFactory<>("maxHeightppt"));
        maxHeightCol.setCellFactory(cellDoubleFactory);

        TableColumn standardCol = new TableColumn("std");
        standardCol.setEditable(false);
        standardCol.setMinWidth(IMP_THIN_COLUMN);
        standardCol.setCellValueFactory(new PropertyValueFactory<>("standardppt"));

        TableColumn xposedCol = new TableColumn("exp.\npad");
        xposedCol.setEditable(false);
        xposedCol.setMinWidth(IMP_THIN_COLUMN);
        xposedCol.setMaxWidth(IMP_THIN_COLUMN);
        xposedCol.setCellValueFactory(new PropertyValueFactory<>("padExposedppt"));
        xposedCol.setCellFactory(CheckBoxTableCell.forTableColumn(xposedCol));

        TableColumn notesCol = new TableColumn("notes");
        notesCol.setEditable(false);
        notesCol.setMinWidth(IMP_THIN_COLUMN);
        notesCol.setCellValueFactory(new PropertyValueFactory<>("spepaNotesppt"));

        TableColumn confCol = new TableColumn("conflict");
        confCol.setEditable(false);
        confCol.setMinWidth(IMP_THIN_COLUMN);
        confCol.setMaxWidth(IMP_THIN_COLUMN);
        confCol.setCellValueFactory(new PropertyValueFactory<>("conflicted"));
        confCol.setCellFactory(CheckBoxTableCell.forTableColumn(confCol));

        TableColumn selectVarCol = new TableColumn();
        selectVarCol.setMinWidth(IMP_THIN_COLUMN/2);
        selectVarCol.setMaxWidth(IMP_THIN_COLUMN/2);
        selectVarCol.setEditable(true);
        selectVarCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectVarCol.setCellFactory(new Callback<TableColumn<ImportedVariant,Boolean>,TableCell<ImportedVariant,Boolean>>(){
            @Override public TableCell<ImportedVariant,Boolean> call( TableColumn<ImportedVariant,Boolean> p ){
                CheckBoxTableCell<ImportedVariant, Boolean> cbt = new CheckBoxTableCell<>();
                cbt.setSelectedStateCallback(new Callback<Integer, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(Integer index) {
                        return spepaTable.getItems().get(index).selectedProperty();
                    }
                });
                return cbt;
            }
        });
        CheckBox selectAllVars = new CheckBox();
        selectVarCol.setGraphic(selectAllVars);
        selectAllVars.setOnAction(e -> {
            for (ImportedVariant imp : importVariants) {
                imp.setSelected(((CheckBox) e.getSource()).isSelected());
            }
        });

        spepaTable.setItems(importVariants);
        spepaTable.getColumns().addAll(ipcNameCol, minHeightCol, maxHeightCol, standardCol, xposedCol, notesCol, confCol, selectVarCol);

        final HBox inspectionBtnBox = new HBox(SPACING_HBOX);   //TODO:reposition button... well... redo entire layout of this scene
        inspectionBtnBox.setAlignment(Pos.CENTER_RIGHT);
        final Button mergeVariants = new Button("Merge selected variants");
        mergeVariants.setTooltip(new Tooltip("Copy the non-conflicting variants to the existing package"));
        mergeVariants.setOnAction((ActionEvent arg0) -> {
            /* Add selected variants to a list */
            ArrayList<ImportedVariant> impSelection = new ArrayList();
            for(ImportedVariant imp : importVariants){
                if(imp.selected.get()){
                    impSelection.add(imp);
                }
            }
            /* Retrieve destination Package */
            Package dest = retrieveByName(inspectedPackage.names);
            ArrayList<SpepaMirror> merge = new ArrayList();
            loadSpePacks(dest, merge);  /* Get its current variants */
            for(ImportedVariant imp : impSelection){
                merge.add(imp.spep);    /* Merge lists */
            }
            updateVariants(merge, dest);    /* Update Package */
            for(ImportedVariant imp : impSelection){
                imp.checkConflict();    /* Update conflicted status of imported variants */
            }
            selectAllVars.setSelected(false);
        });
        inspectionBtnBox.getChildren().add(mergeVariants);

        inspectionBox.getChildren().addAll(inspectionManager, spepaTable, inspectionBtnBox);
        tableBox.getChildren().addAll(cleanBox, conflictedBox, inspectionBox);
        importBranch.getChildren().addAll(buttonBox, tableBox);
        root.setContent(importBranch);
        Scene scene = new Scene(root);
        scene.setUserData(new String("import"));
        scene.getStylesheets().add("file://" + getResourcePath() + "/packages.css");
        return scene;
    }

    public boolean noConflicts(List<ImportedVariant> list){
        for(ImportedVariant imp : list){
            if(imp.conflicted.get()){
                return false;
            }
        }
        return true;
    }

    /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Help functionality methods !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    private Scene initHelpScene(){
        final HBox buttonBox = new HBox();
        buttonBox.setPadding(new Insets(5));

        /* go back to main Scene */
        final Button backButton = new Button("back");
        backButton.setCancelButton(true);
        backButton.setOnAction((ActionEvent arg0) -> {
            assert(previousScene != null);
            changeScene(previousScene);
        });
        buttonBox.getChildren().addAll(createHSpacer(), backButton);

        String url = getResourcePath();
        url = "file://" + url + "/help-index.html";

        webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.getEngine().load(url);

        String wvStylePath = getResourcePath() + "/webviewstyle.css";   //stylesheet currently only disables horizontal scrollbar
        File file = new File(wvStylePath);
        if(!file.exists()){
            System.out.println("Warning: can't find webview stylesheet");
        } else{
            webView.getEngine().setUserStyleSheetLocation("file://" + wvStylePath);
        }

        BorderPane root = new BorderPane(webView);
        root.setTop(buttonBox);
        Scene scene = new Scene(root);
        scene.setUserData(new String("help"));
        scene.getStylesheets().add("file://" + getResourcePath() + "/packages.css");
        return scene;
    }
    private void updateHelpTopic(){
        String baseName = "index";
        if (currentSceneName != null && currentSceneName.length() > 0)
            baseName = currentSceneName;

        String url = getResourcePath();
        url = "file://" + url + "/help-" + baseName + ".html";
        webView.getEngine().load(url);
    }
    static String getResourcePath(){
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
             * so, strip it off
             */
            last_slash = url.lastIndexOf('/');
            String subpath = url.substring(last_slash + 1);
            if(subpath.equalsIgnoreCase("target") || subpath.equalsIgnoreCase("bin")){
                url = url.substring(0, last_slash);
            }
            url += "/doc";
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
        root.setFitToWidth(true);
        final VBox searchBranch = new VBox(SPACING_VBOX);
        searchBranch.setFillWidth(true);
        searchBranch.prefWidthProperty().bind(root.widthProperty());
        final HBox topBox = new HBox(SPACING_HBOX);
        topBox.setPadding(new Insets(0, 0, 0, fieldSpacing));
        final HBox midBox = new HBox(SPACING_HBOX);
        midBox.setPadding(new Insets(fieldSpacing, fieldSpacing, 0, fieldSpacing));
        final GridPane searchGrid = new GridPane(); //is in midbox
        //searchGrid.setStyle("-fx-grid-lines-visible: true");  // to help visualize the layout when debugging
        final HBox botBox = new HBox(SPACING_HBOX);

        final SearchConstraint sc = new SearchConstraint();

        /* go back to main Scene */
        final Button backButton = new Button("back");
        backButton.setCancelButton(true);
        backButton.setOnAction((ActionEvent arg0) -> {
            changeScene(mainScene);
        });

        /* input for searching */
        searchField = new TextField();
        /* input action for searchField is set later, because it references a
         * control that is declared below
         */

        topBox.getChildren().addAll(searchField, new Label("press ENTER to search"), createHSpacer(), backButton);


        final VBox searchBox = new VBox(SPACING_VBOX);
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
                            selectionCanceledWarning.show(stage);
                        } else {
                            newIndex = selectionIndex;
                        }
                    }
                    navigate(newIndex);
                    changeScene(mainScene);
                }
            });
            return row ;
        });

        TableColumn matchCol = new TableColumn("matches");
        matchCol.setEditable(false);
        matchCol.setMinWidth(200);
        matchCol.setCellValueFactory(new PropertyValueFactory<>("match"));

        TableColumn nameCol = new TableColumn("Package name");
        nameCol.setEditable(false);
        nameCol.setMinWidth(100);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

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
        searchTable.getColumns().addAll(matchCol, nameCol, selectCol);

        searchField.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });

        final Label instruction = new Label("Double-click to view package");
        searchBox.getChildren().addAll(searchTable, instruction);

        /* defining content voor searchGrid */
        final int labelRow = 0;
        final int pinRow = labelRow + 1;
        final int pitchRow = pinRow + 1;
        final int spanRow = pitchRow + 1;
        final int heightRow = spanRow + 1;
        final int termRow = heightRow + 1;
        final int selTableRow = termRow + 1;
        final int selContRow = selTableRow + 1;
        final int buttonRow = selContRow + 1;

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
                sc.isActive = true;
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });
        pinConstraint.setOnKeyReleased( event -> {
            String value = pinConstraint.getText();
            sc.hasPinConstraint = !value.equals("");
            pinCountCheck.setSelected(sc.hasPinConstraint);
            try{
                sc.pinConstraint = Integer.parseInt(value);
            } catch(Exception e){
                sc.hasPinConstraint = false;
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
                sc.isActive = true;
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });
        pitchConstraint.setOnKeyReleased( event -> {
            String value = pitchConstraint.getText();
            sc.hasPitchConstraint = !value.equals("");
            pitchCheck.setSelected(sc.hasPitchConstraint);
            try{
                sc.pitchConstraint = Double.parseDouble(value);
            } catch(Exception e){
                sc.hasPitchConstraint = false;
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
                sc.isActive = true;
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });
        spanConstraint.setOnKeyReleased( event -> {
            String value = spanConstraint.getText();
            sc.hasSpanConstraint = !value.equals("");
            spanCheck.setSelected(sc.hasSpanConstraint);
            try{
                sc.spanConstraint = Double.parseDouble(value);
            } catch(Exception e){
                sc.hasSpanConstraint = false;
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
                sc.isActive = true;
                fullSearch(allPackages, searchField.getText(), results, sc);
                selectAll.setSelected(false);
            }
        });
        heightConstraint.setOnKeyReleased( event -> {
            String value = heightConstraint.getText();
            sc.hasSpanConstraint = !value.equals("");
            heightCheck.setSelected(sc.hasSpanConstraint);
            try{
                sc.heightConstraint = Double.parseDouble(value);
            } catch(Exception e){
                sc.hasSpanConstraint = false;
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

        final Button searchButton = new Button("Search");
        searchButton.setOnAction((ActionEvent arg0) -> {
            fullSearch(allPackages, searchField.getText(), results, sc);
            selectAll.setSelected(false);
        });
        searchButton.setDefaultButton(true);
        GridPane.setConstraints(searchButton, 0, buttonRow);

        searchGrid.getChildren().addAll(
                advancedSearch,
                pinCountCheck, pinConstraint,
                pitchCheck, pitchConstraint,
                spanCheck, spanConstraint,
                heightCheck, heightConstraint,
                termCheck, termConstraint,
                searchButton
        );


        final VBox selectionBox = new VBox(SPACING_VBOX);
        /* selectionTable displays selected SearchResults, which can be viewed separately from other Packages. */
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
                    changeScene(mainScene);
                }
            });
            return row ;
        });

        TableColumn selectedCol = new TableColumn("selection");
        selectedCol.setMinWidth(200);
        selectedCol.setCellValueFactory(new PropertyValueFactory<>("match"));

        selectionTable.setItems(selection);
        selectionTable.getColumns().addAll(selectedCol);

        final HBox selControlBox = new HBox(SPACING_HBOX);
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
        selectionBox.getChildren().addAll(selectionTable, selControlBox);


        midBox.getChildren().addAll(searchGrid, createHSpacer(), searchBox, createHSpacer(), selectionBox);



        //botBox.getChildren().addAll(instruction);

        searchBranch.setPadding(new Insets(fieldSpacing, fieldSpacing, 0, fieldSpacing));
        searchBranch.getChildren().addAll(topBox, midBox, botBox);
        root.setContent(searchBranch);
        Scene scene = new Scene(root); //node, width, minHeight
        scene.setUserData(new String("search"));
        scene.getStylesheets().add("file://" + getResourcePath() + "/packages.css");
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
        exportItem.setDisable(!filtered);
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
                res.setName(p.names[0]);
                resultList.add(res);
                gotOne = true;
            }
            if(sc.searchDescription &&
               (sc.exactMatch ? p.description.toLowerCase().equals(s.toLowerCase()) : p.description.toLowerCase().contains(s.toLowerCase()))){ //search description
                res = new SearchResult(searchList.indexOf(p));
                res.match = new SimpleStringProperty(p.description);
                res.setName(p.names[0]);
                if(!gotOne){
                    resultList.add(res);
                    gotOne = true;
                }
            }
            if(sc.searchReferences && p.references != null){
                for(Package.Reference ref: p.references){
                    if(sc.exactMatch ? ref.standard.equalsIgnoreCase(s) : ref.standard.toLowerCase().contains(s.toLowerCase())){
                        res = new SearchResult(searchList.indexOf(p));
                        matchedIndex = searchList.indexOf(p);
                        res.match = new SimpleStringProperty(ref.standard);
                        res.setName(p.names[0]);
                        if(!gotOne){
                            resultList.add(res);
                            gotOne = true;
                        }
                    }
                    if(sc.exactMatch ? ref.company.equalsIgnoreCase(s) : ref.company.toLowerCase().contains(s.toLowerCase())){
                        res = new SearchResult(searchList.indexOf(p));
                        matchedIndex = searchList.indexOf(p);
                        res.match = new SimpleStringProperty(ref.company);
                        res.setName(p.names[0]);
                        if(!gotOne){
                            resultList.add(res);
                            gotOne = true;
                        }
                    }
                }
            }
            if(p.specPacks != null){                            //search SpecificPackages/Variants
                for(Package.Variant sp: p.specPacks){
                    if(sc.exactMatch ? sp.variantName.equalsIgnoreCase(s) : sp.variantName.toLowerCase().contains(s.toLowerCase())){
                        res = new SearchResult(searchList.indexOf(p));
                        matchedIndex = searchList.indexOf(p);
                        res.match = new SimpleStringProperty(sp.variantName);
                        res.setName(p.names[0]);
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
                        res.setName(p.names[0]);
                        if(!gotOne){
                            resultList.add(res);
                            gotOne = true;
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
                    boolean constraintMetFlag = false;
                    for(Package.Footprint fp : searchList.get(sr.getOrgIndex()).footPrints){
                        if(roughCompare(sc.spanConstraint, fp.span.x, 0.1)){
                            constraintMetFlag = true;
                            //TODO: Also compare to span-Y
                        }
                    }
                    if(!constraintMetFlag){
                        toRemove.add(sr);
                        gotOne = true;
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
        for(Package.Variant spep: viewedPackages.get(sr.getOrgIndex()).specPacks){
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
        sc.searchReferences = false;
        fullSearch(allPackages, s, otherSearchList, sc);

        if(!otherSearchList.isEmpty()){
            dupWarning.res = otherSearchList.get(0); //just get the first item from the list (if the file is valid, there can at most be a single conflict: the one just entered)
            return false;
        }
        return true;
    }

    private Package retrieveByName(String name){
        String[] arr = new String[1];
        arr[0] = name;
        return retrieveByName(arr);
    }
    private Package retrieveByName(String[] names){
        for(String s : names){
            if(!notDuplicate(s)){
                return allPackages.get(dupWarning.index());
            }
        }
        return null;    //should never happen, unless user attempts to merge after resolving a name conflict, which would be dumb
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

    /* for convenience */
    private void print(String s){
        System.out.println(s);
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
        int fieldsPerRow;
        Package pack;   //source and destination package. Is set in the load method.
        Stack<Row> rowBin;
        Row supplyRow;
        Stack<Row.DTF> dtfSupply;

        /* add all names from current Package plus an empty field */
        public void load(Package newSource){
            pack = newSource;
            this.getChildren().clear();
            rowBin.clear();
            dtfSupply.clear();
            supplyRow.fillSupply(50); //Max number of TextFields that I think will be in use at any give time.
            for(String s: pack.names){
                if(s.length() > 0){
                    baseAdd(s);
                }
            }
            if (appendEmptyField())
                baseAdd("");
        }
        protected boolean appendEmptyField(){
            return true;    // can be overridden in derived classes
        }

        protected void baseAdd(String s){
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
        protected void addRow(){
            if(!rowBin.empty()){
                this.getChildren().add(rowBin.pop());
            } else{
                this.getChildren().add(createRow());
            }
        }

        protected Row createRow(){
            return new Row();
        }

        /*Take the first entry from the next row and put it in the current one, then repeat for next row if there is one */
        protected void takeFromNext(Row crntRow){
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

        protected int dtfCount(){
            int count = 0;
            for(Node n: this.getChildren()){
                Row r = (Row) n;
                count += r.getChildren().size();
            }
            return count;
        }

        protected int dtfIndex(Row.DTF d){
            int index = 0;
            for(Node n: this.getChildren()){
                Row r = (Row) n;
                if(r.getChildren().contains(d)){
                    index += r.getChildren().indexOf(d);
                    return index;
                }
                index += r.getChildren().size();
            }
            return -1;
        }

        protected Row.DTF dtfGetTextField(int index){
            for(Node n: this.getChildren()){
                Row r = (Row) n;
                for(Node n2 : r.getChildren()){
                    if (index == 0)
                        return (Row.DTF) n2;
                    index -= 1;
                }
            }
            return null;
        }

        protected Row dtfBelongsTo(Row.DTF d){
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
            pack.names = tmpNameArray; //replace Package.names
            storeDoMore();
        }
        protected void storeDoMore(){}  //empty in parent class but can be overridden for extra functionality in children

        protected class Row extends HBox{
            protected String identifier;

            protected void add(String s){
                if(!this.isFull()){
                    DTF dtf = dtfSupply.pop();
                    dtf.setText(s);
                    dtf.check = s;
                    dtf.setPromptText("name");
                    this.getChildren().add(dtf);
                    addDoMore(dtf);
                } else{
                    dtfManager.this.baseAdd(s);
                }

            }
            protected void addDoMore(DTF dtf){}    //empty in parent class but can be overridden for extra functionality in children

            protected void remove(DTF dtf){
                if(lastRow(this)){
                    this.getChildren().remove(dtf); //if it's in the last row just remove it
                } else{
                    this.getChildren().remove(dtf);
                    takeFromNext(this);             //if it's not the last row than textfields will need to be shifted back
                }
            }

            protected boolean lastRow(Row r){
                dtfManager dtf = dtfManager.this;
                int index = dtf.getChildren().indexOf(r);
                int maxIndex = dtf.getChildren().size() - 1;
                return index == maxIndex;
            }

            protected boolean isFull(){
                boolean full = this.getChildren().size() == fieldsPerRow;
                return(full);
            }
            protected boolean isEmpty(){
                return(this.getChildren().isEmpty());
            }

            private void fillSupply(int supply){
                assert(dtfSupply.size() == 0);
                for(int i = 0; i < supply; i++){
                    dtfSupply.push(createDTF());
                }
            }

            protected DTF createDTF(){
                return new DTF("");
            }

            protected class DTF extends TextField{
                String check;

                protected boolean lastInRow(){
                    Row r = dtfBelongsTo(this);
                    int index = r.getChildren().indexOf(this);
                    int maxIndex = r.getChildren().size() - 1;
                    return index == maxIndex;
                }

                protected boolean finalOne(){
                    Row r = dtfBelongsTo(this);
                    return this.lastInRow() && r.lastRow(r);
                }

                protected void handle(String s){
                    s = s.trim();
                    if(!s.equals(check)){
                        if(notDuplicate(s)){
                            if(s.equals("")){
                                if(this.finalOne()){
                                    //do nothing - not sure if this ever runs, after all, check should also reference an emty string
                                } else{
                                    // delete entry & shift remaining entries back to fall into place
                                    dtfBelongsTo(this).remove(this);
                                    check = s;
                                    store();
                                }
                            } else{
                                if(this.finalOne()){
                                    if (appendEmptyField())
                                        baseAdd("");
                                    check = s;
                                    store();
                                    flagDuplicate(false); //to undo yellowing potentially done during loading in 'imports' child class
                                } else{
                                    check = s;
                                    store();
                                    flagDuplicate(false);
                                }
                            }
                            change("names changed");
                        } else{
                            flagDuplicate(true);
                        }
                    }
                }

                protected void move_position(Row.DTF field, int step){
                    assert field != null;
                    int first = dtfIndex(field);
                    int second = first + step;
                    int total = dtfCount() - 1; /* -1 because last field is empty */
                    if (second >= 0 && second < total){
                        Row.DTF secondField = dtfGetTextField(second);
                        assert secondField != null;
                        String t1 = field.getText();
                        String t2 = secondField.getText();
                        field.setText(t2);
                        field.check = t2;
                        secondField.setText(t1);
                        secondField.check = t1;
                        store();
                        change("names swapped");
                        secondField.requestFocus();
                    }
                }

                protected void flagDuplicate(boolean dup){
                    if (dup){
                        this.clear();
                        dupWarning.show(stage);
                    } else {
                        // make sure to undo yellowing
                        this.setStyle("TextFieldBkgnd: white;");
                    }
                }

                DTF(String s){
                    super(s);
                    check = s;

                    this.setOnKeyPressed( event -> {
                        KeyCode keycode = event.getCode();
                        if ( event.isControlDown() ){
                            if ( keycode == KeyCode.LEFT || keycode == KeyCode.RIGHT ){
                                move_position(this, (keycode == KeyCode.LEFT) ? -1 : 1);
                            }
                        } else if( keycode == KeyCode.ENTER ){
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
            Row(){
                super(SPACING_HBOX);
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
        dtfManager(){
            this(5);
        }
        dtfManager(int nrOfFields){
            super(SPACING_VBOX);
            fieldsPerRow = nrOfFields;
            rowBin = new Stack();
            supplyRow = createRow();
            dtfSupply = new Stack();
        }
    }

    private class dtfManagerImports extends dtfManager{
        ImportedPackage importPack;

        //override methods and create constructors
        public void load(ImportedPackage importPack){
            this.importPack = importPack;
            super.load(importPack.pack);
        }

        @Override
        protected boolean appendEmptyField(){
            return false;
        }

        @Override
        protected Row createRow(){
            return new RowImports();
        }

        @Override
        protected void storeDoMore(){
            importPack.checkConflicted();
        }

        protected class RowImports extends dtfManager.Row{
            //override methods and create constructors
            @Override
            protected void addDoMore(DTF dtf){
                //if it's a duplicate, mark it with a yellow background
                if(!notDuplicate(dtf.getText())){
                    dtf.setStyle("TextFieldBkgnd: #fff0a0;");
                }
            }

            @Override
            protected DTF createDTF(){
                return new DTFImports("");
            }

            protected class DTFImports extends dtfManager.Row.DTF{
                //override methods and create constructors
                protected void flagDuplicate(boolean dup){
                    if (dup){
                        this.setStyle("TextFieldBkgnd: #fff0a0;");
                        dupSoftWarning.show(stage);
                    } else {
                        this.setStyle("TextFieldBkgnd: white;");
                    }
                }
                DTFImports(String s){
                    super(s);
                }
            }

            RowImports(){
                super();
            }
        }

        dtfManagerImports(){
            this(5);
        }
        dtfManagerImports(int fields){
            super(fields);
        }
    }

    /* Mirror class for SpecificPackage/Variant */
    public class SpepaMirror implements Removable{
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

        /* Support methods */
        public Package.Variant getVariant(){
            return getVariant(crntPkg);
        }
        public Package.Variant getVariant(Package source){
            for(Package.Variant v : source.specPacks){
                if(spepaName.equals(v.variantName)){
                    return v;
                }
            }
            return null;
        }
        /* Interface implementation */
        @Override
        public void remove(){
            crntPkg.specPacks.remove(getVariant());
            change("deleted variant");
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
        SpepaMirror(Package.Variant spepa){
            this(spepa.variantName,
                 spepa.standard,
                 spepa.heightRange.minHeight,
                 spepa.heightRange.maxHeight,
                 spepa.centerPadExposed,
                 spepa.variantNotes);
        }
    }

    /* This is a wrapper for spepamirror that provides additional functionality needed for imports */
    public class ImportedVariant{
        SpepaMirror spep;
        /* reference spepamirror properties */
        public SimpleStringProperty spepaNamepptProperty(){
            return spep.spepaNameppt;
        }
        public void setIpcName(String name){
            spep.spepaName = name;
            spep.spepaNameppt.set(name);
            checkConflict();
        }

        public SimpleStringProperty standardpptProperty(){
            return spep.standardppt;
        }

        public SimpleDoubleProperty minHeightpptProperty(){
            return spep.minHeightppt;
        }

        public SimpleDoubleProperty maxHeightpptProperty(){
            return spep.maxHeightppt;
        }

        public SimpleBooleanProperty padExposedpptProperty(){
            return spep.padExposedppt;
        }

        public SimpleStringProperty spepaNotespptProperty(){
            return spep.spepaNotesppt;
        }

        /* Properties/methods for import */
        SimpleBooleanProperty conflicted;
        public SimpleBooleanProperty conflictedProperty(){
            return conflicted;
        }

        SimpleBooleanProperty selected;
        public SimpleBooleanProperty selectedProperty(){
            return selected;
        }
        public void setSelected(boolean b){
            selected.set(b);
        }

        private void checkConflict(){
            if(notDuplicate(spep.spepaName)){
                conflicted.set(false);
            } else{
                conflicted.set(true);
                setSelected(false);
            }
        }


        ImportedVariant(SpepaMirror spep){
            this.spep = spep;
            selected = new SimpleBooleanProperty(false);
            conflicted = new SimpleBooleanProperty(!notDuplicate(spep.spepaName));
            selected.addListener((ObservableValue<? extends Boolean> obs, Boolean wasSelected, Boolean isSelected) -> {
                /* Undo selection if conflicted */
                if(isSelected){
                    if(conflicted.get()){
                        setSelected(false);
                    }
                }
            });
        }
    }

    public class PadDimMirror implements Removable, Editable{
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

        double length;                      //cy
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

        double width;                       //cx
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

        Package.PadType padType;
        SimpleStringProperty padTypeppt;
        public SimpleStringProperty padTypepptProperty(){
            return padTypeppt;
        }
        public void setPadExposed(Package.PadType pt){
            this.padType = pt;
            setPadExposedppt(Package.padTypeAsString(pt));
        }
        public void setPadExposedppt(String s){
            padTypeppt.set(s);
        }

        SimpleBooleanProperty isPoly;
        public SimpleBooleanProperty isPolyProperty(){
            return isPoly;
        }
        public void setIsPoly(boolean b){
            isPoly.set(b);
        }

        public class VertexMirror implements Comparable<VertexMirror> {
            SimpleIntegerProperty id;
            public SimpleIntegerProperty idProperty(){
                return id;
            }
            public void setId(int i){
                id.set(i);
            }
            public int id(){
                return id.get();
            }

            SimpleDoubleProperty x;
            public SimpleDoubleProperty xProperty(){
                return x;
            }
            public void setX(double d){
                x.set(d);
            }

            SimpleDoubleProperty y;
            public SimpleDoubleProperty yProperty(){
                return y;
            }
            public void setY(double d){
                y.set(d);
            }

            VertexMirror(int id, double x, double y) {
                this.id = new SimpleIntegerProperty(id);
                this.x = new SimpleDoubleProperty(x);
                this.y = new SimpleDoubleProperty(y);
            }
            VertexMirror(Polygon.Vertex v){
                this(v.id, v.x, v.y);
            }

            @Override
            public int compareTo(VertexMirror v) {
                if(id() == v.id()){
                    return 0;
                } else if(id() > v.id()){
                    return 1;
                } else{
                    return -1;
                }
            }
        }
        final ObservableList<VertexMirror> verMirList;

        public Polygon getPolyFromList(){
            if(verMirList.isEmpty()) return null;
            Polygon pol = new Polygon();
            this.verMirList.forEach(vm -> {
                pol.addVertex(vm.x.get(), vm.y.get());
            });
            return pol;
        }
        public void storePoly(){
            /* retrieve reference to PadDimension */
            Package.Footprint.PadDimension dest = getDimension();
            if(dest == null){
                System.out.println("Something went wrong retrieving destination PadDimension.");
            } else{
                /* create a Polygon and store it */
                dest.polygon = getPolyFromList();
            }
        }
        /* Returns a copy of a Polygon because we don't want the transformations that happen for drawing it to be stored */
        public Polygon retrievePolyCopy(){
            Package.Footprint.PadDimension dest = getDimension();
            if(dest == null){
                System.out.println("Something went wrong retrieving source PadDimension.");
                return null;
            } else{
                if(dest.polygon == null){
                    System.out.println("Polygon not stored in package.");
                    return null;
                }
                return new Polygon(dest.polygon);
            }
        }
        /* retrieve the PadDimension that this is a mirror of */
        public Package.Footprint.PadDimension getDimension(){
            return getDimension(crntPkg);
        }
        public Package.Footprint.PadDimension getDimension(Package p){
            for(Package.Footprint.PadDimension dim : p.footPrints.get(fpIndex).dimensions){
                if(dim.padId == this.padId) return dim;
            }
            return null;
        }

        public void updateValues(){
            Polygon poly = retrievePolyCopy();
            /* Get new values */
            double newX = poly.getWidth();
            double newY = poly.getLength();

            /* Store in mirror object */
            this.setWidth(newX);
            this.setLength(newY);

            /* Store in Package */
            Package.Footprint.PadDimension pd = getDimension();
            pd.setWidth(newX);
            pd.setLength(newY);
        }

        /* Implement interface */
        @Override
        public void remove(){
            crntPkg.footPrints.get(fpIndex).dimensions.remove(getDimension());
            change("deleted pad dimension");
        }

        @Override
        public void edit() {
            polyBuilder.setSource(this);
            polyBuilder.show(stage);
        }

        PadDimMirror(int padId, double width, double length, Package.PadShape shape, double holeDiam, double originX, double originY, Package.PadType pt, Polygon pol){
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

            this.padType = pt;
            padTypeppt = new SimpleStringProperty(Package.padTypeAsString(pt));

            this.isPoly = new SimpleBooleanProperty();
            if(shape == Package.PadShape.POLYGON){
                this.isPoly.set(true);
            } else{
                this.isPoly.set(false);
            }

            verMirList = FXCollections.observableArrayList();
            if(pol != null){
                pol.vertices.forEach(v -> {
                    verMirList.add(new VertexMirror(v));
                });
            }
        }
        PadDimMirror(Package.Footprint.PadDimension dim){
            this(dim.padId,
                 dim.width,
                 dim.length,
                 dim.shape,
                 dim.holeDiam,
                 dim.originX,
                 dim.originY,
                 dim.padType,
                 dim.polygon);
        }
    }


    public class PadPosMirror implements Removable{
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

        /* retrieve the PadPosition that this is a mirror of */
        public Package.Footprint.PadPosition getPosition(){
            return getPosition(crntPkg);
        }
        public Package.Footprint.PadPosition getPosition(Package p){
            for(Package.Footprint.PadPosition pos : p.footPrints.get(fpIndex).padPositions){
                if(pos.pinId.equals(this.pinId)) return pos;
            }
            return null;
        }

        /* Implement interface */
        @Override
        public void remove(){
            crntPkg.footPrints.get(fpIndex).padPositions.remove(getPosition());
            change("deleted pad position");
        }

        PadPosMirror(String pinId, double xPos, double yPos, int padId, Package.Orientation rotation){
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
        PadPosMirror(Package.Footprint.PadPosition pos){
            this(pos.pinId,
                 pos.xPos,
                 pos.yPos,
                 pos.padId,
                 pos.rotation);
        }
    }


    public class ReferenceMirror implements Removable{
        SimpleStringProperty standard;
        public SimpleStringProperty standardProperty(){
            return standard;
        }

        SimpleStringProperty company;
        public SimpleStringProperty companyProperty(){
            return company;
        }

        Package.Reference getReference() {
            return getReference(crntPkg);
        }
        Package.Reference getReference(Package p) {
            for(Package.Reference r : p.references){
                if(r.standard.equals(standard.get())) return r;
            }
            return null;
        }

        /* Interface implementation */
        @Override
        public void remove(){
            crntPkg.references.remove(getReference());
            change("deleted reference");
        }

        /* Constructors */
        ReferenceMirror(){
            this("", "");
        }
        ReferenceMirror(String standard, String company){
            this.standard = new SimpleStringProperty(standard);
            this.company = new SimpleStringProperty(company);
        }
        ReferenceMirror(Package.Reference ref){
            this(ref.standard, ref.company);
        }
    }


    public class RelatedPack implements Removable{
        Package pack;

        SimpleStringProperty name;
        public SimpleStringProperty nameProperty(){
            return name;
        }

        SimpleStringProperty description;
        public SimpleStringProperty descriptionProperty(){
            return description;
        }

        public int getIndex(List<Package> list){
            return list.indexOf(pack);
        }

        /* Implementation interface */
        @Override
        public void remove() {
            crntPkg.relatedPackNames.remove(name.get());
            change("deleted related package");
        }

        RelatedPack(String name){
            this.name = new SimpleStringProperty(name);
            pack = retrieveByName(name);
            if (pack != null)
                description = new SimpleStringProperty(pack.description);
        }
    }

    /* ImportedPackage is a wrapper for the Package class that to allows it to be used in TableViews */
    public class ImportedPackage{
        Package pack;

        SimpleStringProperty firstName;
        public StringProperty firstNameProperty(){
            return firstName;
        }
        public void setFirstName(String s){
            firstName.set(s);
        }
        public String getFirstName(){
            return firstName.get();
        }

        SimpleBooleanProperty conflicted;
        public SimpleBooleanProperty conflictedProperty(){
            return conflicted;
        }
        public void setConflicted(boolean b){
            conflicted.set(b);
        }
        public boolean getConflicted(){
            return conflicted.get();
        }

        SimpleBooleanProperty selected;
        public SimpleBooleanProperty selectedProperty(){
            return selected;
        }
        public void setSelected(boolean b){
            selected.set(b);
        }

        public boolean checkConflicted(){
            // check package names and variant names for conflicts and set booleanproperty 'conflicted' accordingly
            for(String s: pack.names){
                if(!notDuplicate(s)){
                    setConflicted(true);
                    return true;
                }
            }
            if(pack.specPacks != null){
                for(Package.Variant spep: pack.specPacks){
                    if(!notDuplicate(spep.variantName)){
                        setConflicted(true);
                        return true;
                    }
                }
            }
            setConflicted(false);
            setFirstName(pack.names[0]);    //in case this method is called from dtfmanagerImports, because first name may have changed.
            return false;
        }

        /* Constructor */
        ImportedPackage(Package pack){
            this.pack = pack;
            firstName = new SimpleStringProperty(pack.names[0]);
            conflicted = new SimpleBooleanProperty(false);
            conflicted.addListener((ObservableValue<? extends Boolean> obs, Boolean wasConflicted, Boolean isConflicted) -> {
                /* Move to approriate importlist depending on conflicted status
                 * It technically shoudn't be possible for an ImportedPackage to go from a clean state to a conflicted state, but who knows
                 * Also, the safeguarding against duplicates on the list should be redundant, but why not be safe */
                if(isConflicted){
                    if(cleanImportPacks.contains(this)){
                        cleanImportPacks.remove(this);
                    }
                    if(!conflictedImportPacks.contains(this)){
                        conflictedImportPacks.add(this);
                    }
                }
                if(!isConflicted){
                    if(conflictedImportPacks.contains(this)){
                        conflictedImportPacks.remove(this);
                    }
                    if(!cleanImportPacks.contains(this)){
                        cleanImportPacks.add(this);
                    }
                }
            });
            selected = new SimpleBooleanProperty(false);
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

        public SimpleStringProperty name;
        public SimpleStringProperty nameProperty(){
            return name;
        }
        public void setName(String s){
            name.set(s);
        }
        public String getName(){
            return name.get();
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
            this.name = new SimpleStringProperty("");
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
        boolean searchReferences;

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
            searchReferences = true;
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
    private class LongKey extends SimpleKey{
        LongKey(String key){
            super(key);
            this.value = 0L;
        }
    }
    private class DateKey extends LongKey{
        DateKey(String key){
            super(key);
            this.value = new Date().getTime();
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


    private class BasicPopup extends Popup{
        VBox branch;
        Label messageLbl;
        Button close;

        BasicPopup(String style, String message, String btnText){
            super();
            branch = new VBox(SPACING_VBOX);
            branch.setAlignment(Pos.CENTER);
            branch.getStyleClass().add("message-box");
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
        Label FileInfo;

        AboutPopup(String style){
            super();
            final VBox branch;
            branch = new VBox(SPACING_VBOX);
            branch.setAlignment(Pos.CENTER_LEFT);
            branch.getStyleClass().add("message-box");
            branch.setStyle(style);

            final Label Caption = new Label("PACKAGES " + programVersion);
            Caption.setStyle("-fx-font-size:14; -fx-font-weight:bold");
            final Label Copyright = new Label("Copyright 2021, 2022 CompuPhase\n" +
                                              "Developed by Guido Wolff");

            String version = System.getProperty("java.version");
            String fxversion = System.getProperty("javafx.version");
            String jre_path = System.getProperty("java.home");
            String sysmsg = "JDK: \t" + version + " (" + jre_path + ")\n"
                            + "JavaFx: \t" + fxversion;
            final Label SystemInfo = new Label(sysmsg);

            FileInfo = new Label("Data file: \t(none)");
            FileInfo.setPadding(new Insets(0, 0, fieldSpacing, 0)); // add spacing below text (above button)

            final HBox buttonBox = new HBox(SPACING_HBOX);
            buttonBox.setAlignment(Pos.CENTER);
            final Button close = new Button("Close");
            close.setAlignment(Pos.CENTER);
            close.setOnAction((ActionEvent arg0) -> {
                this.hide();
            });
            buttonBox.getChildren().add(close);

            branch.getChildren().addAll(Caption, Copyright, SystemInfo, FileInfo, buttonBox);
            this.getContent().add(branch);
            this.setAutoHide(true);
        }

        void update(){
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
                        count_variants += p.specPacks.size();
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

    private class PolygonBuilder extends Popup{
        PadDimMirror dim;

        TableView<PadDimMirror.VertexMirror> polyTable;

        TextField idInput;

        public void setSource(PadDimMirror dim){
            this.dim = dim;
            polyTable.setItems(dim.verMirList);
            idInput.setText(Integer.toString(nextVertexId()));
        }

        PolygonBuilder(String style){
            super();
            final VBox branch = new VBox(SPACING_VBOX);
            branch.setAlignment(Pos.CENTER_LEFT);
            branch.getStyleClass().add("message-box");
            branch.setStyle(style);

            final Label instruction = new Label("Add vertices to construct a polygon");

            final VBox tableBox = new VBox(SPACING_VBOX);
            //table
            polyTable = new TableView<>();
            polyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            polyTable.setEditable(true);
            polyTable.getSelectionModel().setCellSelectionEnabled(true);

            Callback<TableColumn, TableCell> cellIntFactory = (TableColumn p) -> new EditingIntCell();
            Callback<TableColumn, TableCell> cellDoubleFactory = (TableColumn p) -> new EditingDoubleCell();

            TableColumn idCol = new TableColumn("ID");
            idCol.setMinWidth(60);
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setCellFactory(cellIntFactory);
            idCol.setOnEditCommit(
                new EventHandler<CellEditEvent<PadDimMirror.VertexMirror, Integer>>() {
                    @Override
                    public void handle(CellEditEvent<PadDimMirror.VertexMirror, Integer> t) {
                        ((PadDimMirror.VertexMirror) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setId(t.getNewValue());
                    }
                }
            );

            final TableColumn xCol = new TableColumn("X");
            xCol.setMinWidth(50);
            xCol.setCellValueFactory(new PropertyValueFactory<>("x"));
            xCol.setCellFactory(cellDoubleFactory);
            xCol.setOnEditCommit(
                new EventHandler<CellEditEvent<PadDimMirror.VertexMirror, Double>>() {
                    @Override
                    public void handle(CellEditEvent<PadDimMirror.VertexMirror, Double> t) {
                        ((PadDimMirror.VertexMirror) t.getTableView().getItems().get(
                                t.getTablePosition().getRow())
                                ).setX(t.getNewValue());
                    }
                }
            );

            final TableColumn yCol = new TableColumn("Y");
            yCol.setMinWidth(50);
            yCol.setCellValueFactory(new PropertyValueFactory<>("y"));
            yCol.setCellFactory(cellDoubleFactory);
            yCol.setOnEditCommit(
                new EventHandler<CellEditEvent<PadDimMirror.VertexMirror, Double>>() {
                    @Override
                    public void handle(CellEditEvent<PadDimMirror.VertexMirror, Double> t) {
                        ((PadDimMirror.VertexMirror) t.getTableView().getItems().get(
                                t.getTablePosition().getRow())
                                ).setY(t.getNewValue());
                    }
                }
            );

            polyTable.getColumns().addAll(idCol, xCol, yCol);

            final HBox tableInputBox = new HBox(SPACING_HBOX);
            idInput = new TextField();
            idInput.setMaxWidth(dimInputPrefWidth);
            idInput.setPromptText("vertex-id");
            idInput.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue){
                    if(verifyId(Integer.parseInt(idInput.getText()))){
                        vertexIdWarning.show(stage);
                        idInput.clear();
                    }
                }
            });
            final TextField xInput = new TextField();
            xInput.setMaxWidth(dimInputPrefWidth);
            xInput.setPromptText("x");
            final TextField yInput = new TextField();
            yInput.setMaxWidth(dimInputPrefWidth);
            yInput.setPromptText("y");

            final Button addButton = new Button("Add");
            addButton.setOnAction((ActionEvent e) -> {
                /* Ensure fields are filled */
                if(idInput.getText().length() == 0 || xInput.getText().length() == 0 || yInput.getText().length() == 0){
                        incompleteDataWarning.show(stage);
                } else {
                    //add VertexMirror to list
                    dim.verMirList.add(dim.new VertexMirror(Integer.parseInt(idInput.getText()),
                                                        Double.parseDouble(xInput.getText()),
                                                        Double.parseDouble(yInput.getText())));
                }
                idInput.setText(Integer.toString(nextVertexId()));
                xInput.clear();
                yInput.clear();
            });
            tableInputBox.getChildren().addAll(idInput, xInput, yInput, addButton);

            final HBox tableButtonBox = new HBox(SPACING_HBOX);
            final Button delButton = new Button("Delete selected");
            delButton.setOnAction((ActionEvent e) -> {
                PadDimMirror.VertexMirror selectedItem = polyTable.getSelectionModel().getSelectedItem();
                polyTable.getItems().remove(selectedItem);
            });
            final Button previewButton = new Button("Preview");
            previewButton.setOnAction((ActionEvent e) -> {
                //store it first
                Collections.sort(dim.verMirList);
                dim.storePoly();

                previewPopup.load(dim);
                previewPopup.show(stage);
                change("edited polygon");
            });
            tableButtonBox.getChildren().addAll(delButton, createHSpacer(), previewButton);

            tableBox.getChildren().addAll(polyTable, tableInputBox, tableButtonBox);

            final HBox mainButtonBox = new HBox(SPACING_HBOX);
            //acceptbutton, cancelbutton
            final Button acceptButton = new Button("Accept");
            acceptButton.setOnAction((ActionEvent e) -> {
                //turn list of vertexmirrors into actual polygon object in crentpkg.dimensions[*]
                Collections.sort(dim.verMirList);
                dim.storePoly();
                dim.updateValues();
                checkContour();
                loadImage();
                this.hide();
                change("edited polygon");
            });
            final Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction((ActionEvent e) -> {
                this.hide();    //note that cancel won't undo changes if preview has been used, as it stores the polygon
            });

            mainButtonBox.getChildren().addAll(acceptButton, cancelButton);

            branch.getChildren().addAll(instruction, tableBox, mainButtonBox);
            this.getContent().add(branch);
        }

        private int nextVertexId() {
            if(dim.verMirList.isEmpty()){
                return 0;
            } else{
                int highest = 0;
                for(PadDimMirror.VertexMirror v : dim.verMirList){
                    if(v.id.get() > highest){
                        highest = v.id.get();
                    }
                }
                return highest + 1;
            }
        }

        /* This method checks if id is unique and returns true if it is not */
        private boolean verifyId(int id) {
            return dim.verMirList.stream().anyMatch(v -> (v.id.get() == id));
        }
    }

    private class ImagePopup extends Popup{
        private final int POP_IMG_WIDTH = 400;
        private final int POP_IMG_HEIGHT = 340;
        Canvas canvas;
        GraphicsContext popGC;

        public void load(PadDimMirror dim){
            prepImage(canvas, popGC);
            Polygon poly = dim.retrievePolyCopy();
            double factor = setScale(canvas, dim);

            drawIndicator(canvas, popGC, factor);

            Color color = Color.color(1.0, 0.1, 0.2);
            popGC.setFill(color);
            popGC.setGlobalAlpha(0.5);

            //scale & move polygon
            poly.move(-dim.originX, -dim.originY);               /* apply origin of the pad shape */
            poly.Scale(factor);                                  /* scale pad dimensions & position for drawing */
            poly.Flip(Polygon.FlipType.FLIP_Y);                  /* toggle Y-axis */
            poly.move(imgXcenter(canvas), imgYcenter(canvas));   /* reposition relative to centre of drawing */

            //draw polygon
            Polygon.Drawable pd = poly.getDrawable();
            popGC.fillPolygon(pd.xPoints, pd.yPoints, pd.nPoints);
        }

        ImagePopup(String style){
            super();
            final VBox branch = new VBox(SPACING_VBOX);
            branch.setAlignment(Pos.CENTER_LEFT);
            branch.getStyleClass().add("message-box");
            branch.setStyle(style);

            canvas = new Canvas(POP_IMG_WIDTH, POP_IMG_HEIGHT);
            popGC = canvas.getGraphicsContext2D();

            final Button close = new Button("close");
            close.setOnAction((ActionEvent arg0) -> {
                this.hide();
            });
            branch.getChildren().addAll(canvas, close);
            this.getContent().add(branch);
            this.setAutoHide(true);
        }
    }

    private class Courtyard{
        Polygon boundbox;

        Courtyard(){
            boundbox = new Polygon();
        }
        Courtyard(Polygon poly){
            boundbox = new Polygon(poly);
        }

        public void AddBoundingBox(double cx, double cy, double orgx, double orgy){
            /* don't do anything when the bounding box has zero width or length */
            if(!roughCompare(cx, 0.0) && !roughCompare(cy, 0.0)){
                Polygon box = Polygon.FromRect(cx, cy);
                box.move(-orgx, -orgy);
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

qol: remember file location: http://javatutorialsx.blogspot.com/2012/03/javafx-20-filechooser-set-initial.html

-------------

qol: change config to not store as json

https://www.amitph.com/introduction-to-java-preferences-api/
https://www.vogella.com/tutorials/JavaPreferences/article.html

*/
