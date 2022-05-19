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
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsonable;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javafx.collections.*;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;


/**
 *
 * @author Guido Daniel Wolff
 *
 */
public class Package implements Jsonable{
    final double epsilon = 0.0001;   /* for checking values close to zero */

    public String[] names;
    public void printNames(){
        System.out.println("Package names:");
        for (String name1 : names) {
            System.out.println(name1);
        }
    }

    public String description;

    public enum CharType{
        SMD,
        THROUGHHOLE
    }
    CharType type;
    static String charTypeasString(CharType c){
        switch(c){
            case SMD: return "SMD";
            case THROUGHHOLE: return "Through-hole";
            default: return "SMD";
        }
    }
    static CharType charTypefromString(String s){
        switch(s){
            case "SMD": return CharType.SMD;
            case "Through-hole": return CharType.THROUGHHOLE;
            default: return CharType.SMD;
        }
    }
    static ObservableList<String> charTypeValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("SMD", "Through-hole");
        return values;
    }

    public enum TermType{
        ENDCAP,
        GULLWING,
        INWARDL,
        LUGLEAD,
        JLEAD,
        NOLEAD,
        BALLGRID,
        LANDGRID,
        CASTELLATED,
        THROUGHHOLE
    }
    TermType termination;
    static String termTypeasString(TermType t){
        switch(t){
            case ENDCAP: return "endcap";
            case GULLWING: return "gull-wing";
            case INWARDL: return "inward-L";
            case LUGLEAD: return "lug-lead";
            case JLEAD: return "J-lead";
            case NOLEAD: return "no-lead";
            case BALLGRID: return "ball-grid";
            case LANDGRID: return "land-grid";
            case CASTELLATED: return "castellated";
            case THROUGHHOLE: return "through-hole";
            default: return "endcap";
        }
    }
    static TermType termTypefromString(String s){
        switch(s){
            case "endcap": return TermType.ENDCAP;
            case "gull-wing": return TermType.GULLWING;
            case "inward-L": return TermType.INWARDL;
            case "lug-lead": return TermType.LUGLEAD;
            case "J-lead": return TermType.JLEAD;
            case "no-lead": return TermType.NOLEAD;
            case "ball-grid": return TermType.BALLGRID;
            case "land-grid": return TermType.LANDGRID;
            case "castellated": return TermType.CASTELLATED;
            case "through-hole": return TermType.THROUGHHOLE;
            default: return TermType.ENDCAP;
        }
    }
    static ObservableList<String> termTypeValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("endcap", "gull-wing", "inward-L", "lug-lead", "J-lead", "no-lead", "ball-grid", "land-grid", "castellated", "through-hole");
        return values;
    }

    boolean polarized;

    class Body implements Jsonable{
        double bodyX;
        double bodyXtol;
        double bodyY;
        double bodyYtol;
        double bodyOrgX;
        double bodyOrgY;

        /*constructors*/
        Body(){
            this(0, 0, 0, 0, 0, 0);
        }
        Body(double x, double xTol, double y, double yTol, double orgX, double orgY){
            this.bodyX = x;
            this.bodyY = y;
            this.bodyXtol = xTol;
            this.bodyYtol = yTol;
            this.bodyOrgX = orgX;
            this.bodyOrgY = orgY;
        }
        private Body(Body body) {   //for copying
            this(body.bodyX, body.bodyY, body.bodyXtol, body.bodyYtol, body.bodyOrgX, body.bodyOrgY);
        }

        /*implementation for interface Jsonable*/
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
            json.put("cx", this.bodyX);
            json.put("cy", this.bodyY);
            if(this.bodyXtol > epsilon || this.bodyYtol > epsilon){
                json.put("tol-x", this.bodyXtol);
                json.put("tol-y", this.bodyYtol);
            }
            if(!checkZero(this.bodyOrgX) || !checkZero(this.bodyOrgY)){
                json.put("x", this.bodyOrgX);
                json.put("y", this.bodyOrgY);
            }
            json.toJson(writer);
        }
    }
    Body body;

    class Lead2Lead implements Jsonable{
        double x;
        double xTol;
        double y;
        double yTol;
        double orgX;
        double orgY;

        /*constructors*/
        Lead2Lead(){
            this(0, 0, 0, 0, 0, 0);
        }
        Lead2Lead(double x, double xTol, double y, double yTol){
            this(x, xTol, y, yTol, 0, 0);

        }
        Lead2Lead(double x, double xTol, double y, double yTol, double orgX, double orgY){
            this.x = x;
            this.y = y;
            this.xTol = xTol;
            this.yTol = yTol;
            this.orgX = orgX;
            this.orgY = orgY;
        }
        Lead2Lead(Lead2Lead l){ //it's a lowercase L
            this(l.x, l.xTol, l.y, l.yTol, l.orgX, l.orgY);
        }

        /*implementation for interface Jsonable*/
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
            json.put("cx", this.x);
            json.put("cy", this.y);
            if(this.xTol > epsilon || this.yTol > epsilon){
                json.put("tol-x", this.xTol);
                json.put("tol-y", this.yTol);
            }
            if(!checkZero(this.orgX) || !checkZero(this.orgY)){
                json.put("x", this.orgX);
                json.put("y", this.orgY);
            }
            json.toJson(writer);
        }
    }
    Lead2Lead lead2lead;

    int nrOfPins;

    double pitch;


    public enum Orientation{
        ZERO,
        NINETY,
        ONEEIGHTY,
        TWOSEVENTY
    }
    Orientation tapeOrient;
    public static int orientationAsInt(Orientation o){
        switch(o){
            case ZERO: return 0;
            case NINETY: return 90;
            case ONEEIGHTY: return 180;
            case TWOSEVENTY: return 270;
            default: return 0;
        }
    }
    static Orientation orientationFromInt(int i){
        switch(i){
            case 0: return Orientation.ZERO;
            case 90: return Orientation.NINETY;
            case 180: return Orientation.ONEEIGHTY;
            case 270: return Orientation.TWOSEVENTY;
            default: return Orientation.ZERO;
        }
    }
    static Orientation orientationFromString(String s){
        switch(s){
            case "0": return Orientation.ZERO;
            case "90": return Orientation.NINETY;
            case "180": return Orientation.ONEEIGHTY;
            case "270": return Orientation.TWOSEVENTY;
            default: return Orientation.ZERO;
        }
    }
    static ObservableList<String> orientationValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("0", "90", "180", "270");
        return values;
    }

    public enum NameStandard{
        EIA_IMPERIAL,
        EIA_METRIC,
        IPC_7351,
        ED_7303,
        NA
    }
    static String nameStandardAsString(NameStandard n){
        switch(n){
            case EIA_IMPERIAL: return "EIA imperial";
            case EIA_METRIC: return "EIA metric";
            case IPC_7351: return "IPC-7351";
            case ED_7303: return "ED-7303";
            case NA: return "";
            default: return "";
        }
    }
    static NameStandard nameStandardFromString(String s){
        if(s == null){
            return NameStandard.NA;
        }
        switch(s){
            case "EIA imperial": return NameStandard.EIA_IMPERIAL;
            case "EIA metric": return NameStandard.EIA_METRIC;
            case "IPC-7351": return NameStandard.IPC_7351;
            case "ED-7303": return NameStandard.ED_7303;
            case "": return NameStandard.NA;
            default: return NameStandard.NA;
        }
    }
    static ObservableList<String> nameStandardValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("", "EIA imperial", "EIA metric", "IPC-7351", "ED-7303");
        return values;
    }

    public class Variant implements Jsonable{    //renamed to "variants" in Json. Keeps old name in code
        /* fields and getter/setters */
        String variantName;
        NameStandard standard;  //TODO: implement everywhere else
        boolean centerPadExposed;
        String variantNotes;
        void setExposed(boolean exposed){
            this.centerPadExposed = exposed;
        }
        boolean getExposed(){
            return this.centerPadExposed;
        }

        class HeightRange implements Jsonable{
            double minHeight;
            double maxHeight;

            /*constructors*/
            HeightRange(){
                this(0.0, 0.0);
            }
            HeightRange(double min, double max){
                this.minHeight = min;
                this.maxHeight = max;
            }

            /*implementation for interface Jsonable of HeoghtRange */
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
                if(this.minHeight > epsilon) {
                    json.put("low", this.minHeight);
                }
                json.put("high", this.maxHeight);
                json.toJson(writer);
            }
        }
        HeightRange heightRange;

        /*constructors*/
        Variant(String ipcName, NameStandard standard, double minHeight, double maxHeight, boolean centerPadExposed, String notes){
            this.variantName = ipcName;
            this.standard =  standard;
            this.centerPadExposed = centerPadExposed;
            this.variantNotes =  notes;
            this.heightRange = new HeightRange(minHeight, maxHeight);
        }
        Variant(Variant spep){
            this(spep.variantName, spep.standard, spep.heightRange.minHeight, spep.heightRange.maxHeight, spep.centerPadExposed, spep.variantNotes);
        }
        Variant(App.SpepaMirror spep){
            this(spep.spepaName, spep.standard, spep.minHeight, spep.maxHeight, spep.padExposed, spep.spepaNotes);
        }

        /*implementation for interface Jsonable */
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
            json.put("name", this.variantName);
            json.put("standard", nameStandardAsString(this.standard));
            if(this.centerPadExposed){
                json.put("exposed-pad", this.centerPadExposed);
            }
            if(this.variantNotes.length() > 0){
                json.put("notes", this.variantNotes);
            }
            if(this.heightRange.minHeight > epsilon || this.heightRange.maxHeight > epsilon){
                json.put("height", this.heightRange);
            }
            json.toJson(writer);
        }

    }
    ArrayList<Variant> specPacks; //renamed to "variants" in Json. Keeps old name in code

    public enum FootprintType{
        NOMINAL,
        LEAST,
        MOST
    }
    static String footprintTypeasString(FootprintType t){
        switch(t){
            case NOMINAL: return "nominal";
            case LEAST: return "least";
            case MOST: return "most";
            default: return "nominal";
        }
    }
    static FootprintType footprintTypefromString(String s){
        switch(s){
            case "nominal": return FootprintType.NOMINAL;
            case "least": return FootprintType.LEAST;
            case "most": return FootprintType.MOST;
            default: return FootprintType.NOMINAL;
        }
    }
    static ObservableList<String> footprintTypeValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("nominal", "least", "most");
        return values;
    }

    public enum PadShape{
        RECTANGLE,
        ROUND,
        ROUNDEDRECT,
        OBROUND,
        POLYGON,
        SPECIAL
    }
    static String padShapeasString(PadShape p){
        switch(p){
            case RECTANGLE: return "rectangle";
            case ROUND: return "round";
            case ROUNDEDRECT: return "roundedrect";
            case OBROUND: return "obround";
            case POLYGON: return "polygon";
            case SPECIAL: return "special";
            default: return "rectangle";
        }
    }
    static PadShape padShapefromString(String s){
        switch(s){
            case "rectangle": return PadShape.RECTANGLE;
            case "round": return PadShape.ROUND;
            case "roundedrect": return PadShape.ROUNDEDRECT;
            case "obround": return PadShape.OBROUND;
            case "polygon": return PadShape.POLYGON;
            case "special": return PadShape.SPECIAL;
            default: return PadShape.RECTANGLE;
        }
    }
    static ObservableList<String> padShapeValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("rectangle", "round", "roundedrect", "obround", "polygon", "special");
        return values;
    }


    public enum PadType{
        STANDARD,
        EXPOSED,
        MECHANICAL
    }
    static String padTypeAsString(PadType pt){
        switch(pt){
            case STANDARD: return "standard";
            case EXPOSED: return "exposed";
            case MECHANICAL: return "mechanical";
            default: return "standard";
        }
    }
    static PadType padTypeFromString(String s){
        switch(s){
            case "standard": return PadType.STANDARD;
            case "exposed": return PadType.EXPOSED;
            case "mechanical": return PadType.MECHANICAL;
            default: return PadType.STANDARD;
        }
    }
    static ObservableList<String> padTypeValues(){
        ObservableList<String> values = FXCollections.observableArrayList();
        values.addAll("standard", "exposed", "mechanical");
        return values;
    }


    public class Footprint implements Jsonable{
        /* fields and getter/setters */
        FootprintType ftprntType;
        void setType(FootprintType type){
            this.ftprntType = type;
        }
        FootprintType getType(){
            return this.ftprntType;
        }

        class Span implements Jsonable{
            double x;
            double y;

            /*constructors*/
            Span(){
                this(0, 0);
            }
            Span(double x, double y){
                this.x = x;
                this.y = y;
            }
            Span(Span source){
                this(source.x, source.y);
            }

            /*implementation for interface Jsonable of Span*/
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
                if(this.x > epsilon){
                    json.put("cx", this.x);
                }
                if(this.y > epsilon){
                    json.put("cy", this.y);
                }
                json.toJson(writer);
            }
        }
        Span span;


        class Outline implements Jsonable{
            double length;
            double width;
            double orgX;
            double orgY;

            /*constructors*/
            Outline(){
                this(0, 0, 0, 0);
            }
            Outline(double length, double width, double orgX, double orgY){
                this.length = length;
                this.width = width;
                this.orgX = orgX;
                this.orgY = orgY;
            }
            Outline(Outline source){
                this(source.length, source.width, source.orgX, source.orgY);
            }

            /*implementation for interface Jsonable of Outline*/
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
                json.put("cx", this.length);    //TODO: possibly swap these to X = width & Y = length
                json.put("cy", this.width);
                if(!checkZero(this.orgX) || !checkZero(this.orgY)){
                    json.put("x", this.orgX);
                    json.put("y", this.orgY);
                }
                json.toJson(writer);
            }
        }
        Outline outline;


        public class PadDimension implements Jsonable{
            int padId;
            void setPadId(int i){
                this.padId = i;
            }
            int getPadId(){
                return this.padId;
            }

            double length;
            void setLength(double l){
                this.length = l;
            }
            double getLength(){
                return this.length;
            }

            double width;
            void setWidth(double w){
                this.width = w;
            }
            double getWidth(){
                return this.width;
            }

            PadShape shape;
            void setPadShape(PadShape p){
                this.shape = p;
            }
            PadShape getPadshape(){
                return this.shape;
            }

            double holeDiam;
            void setHoleDiam(double h){
                this.holeDiam = h;
            }
            double getHoleDiam(){
                return this.holeDiam;
            }

            double originX;
            void setOriginX(double x){
                this.originX = x;
            }
            double getOriginX(){
                return this.originX;
            }
            double originY;
            void setOriginY(double y){
                this.originY = y;
            }
            double getOriginY(){
                return this.originY;
            }

            PadType padType;

            Polygon polygon;


            /*constructor*/
            PadDimension(int padId, double length, double width, PadShape shape, double holeDiam, double originX, double originY, PadType padType, Polygon polygon){
                this.padId = padId;
                this.length = length;
                this.width = width;
                this.shape = shape;
                this.holeDiam = holeDiam;
                this.originX = originX;
                this.originY = originY;
                this.padType = padType;
                if(polygon != null){
                    this.polygon = new Polygon(polygon);
                }
            }
            PadDimension(PadDimension source){
                this(source.padId, source.length, source.width, source.shape, source.holeDiam, source.originX, source.originY, source.padType, source.polygon);
            }
            PadDimension(App.PadDimMirror dim){
                this(dim.padId, dim.length, dim.width, dim.shape, dim.holeDiam, dim.originX, dim.originY, dim.padType, dim.getPolyFromList());
            }

            /*implementation for interface Jsonable of PadDimension*/
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
                json.put("pad-id", this.padId);
                json.put("cx", this.width);
                json.put("cy", this.length);
                json.put("shape", padShapeasString(this.shape));
                if(this.holeDiam > epsilon){   //don't add to json if irrelevant
                    json.put("hole-diameter", this.holeDiam);
                }
                if(!checkZero(this.originX) || !checkZero(this.originY)){
                    json.put("x", this.originX);
                    json.put("y", this.originY);
                }
                json.put("pad-type", padTypeAsString(padType));

                if(polygon != null && this.shape == PadShape.POLYGON){
                    json.put("polygon", this.polygon);
                }

                json.toJson(writer);
            }
        }
        ArrayList<PadDimension> dimensions;

        /* PadDimension copy method */
        private ArrayList<PadDimension> copyDimensions(){
            ArrayList<PadDimension> dest = new ArrayList<>();
            for(PadDimension pd : dimensions){
                dest.add(new PadDimension(pd));
            }
            return dest;
        }

        public class PadPosition implements Jsonable{
            /* fields and getter/setters */
            String pinId; //main pin identifier
            void setPinId(String s){
                this.pinId = s;
            }
            String getPinId(){
                return this.pinId;
            }

            double xPos;
            void setXPos(double p){
                this.xPos = p;
            }
            double getXPos(){
                return this.xPos;
            }

            double yPos;
            void setYPos(double p){
                this.yPos = p;
            }
            double getYPos(){
                return this.yPos;
            }

            int padId;
            void setPadId(int i){
                this.padId = i;
            }
            int getPadId(){
                return this.padId;
            }

            Orientation rotation;
            void setRotation(Orientation r){
                this.rotation = r;
            }
            Orientation getRotation(){
                return this.rotation;
            }

            /* constructor */
            PadPosition(String pinId, double xPos, double yPos, int padId, Orientation rotation){
                this.pinId = pinId;
                this.xPos = xPos;
                this.yPos = yPos;
                this.padId = padId;
                this.rotation = rotation;
            }
            PadPosition(PadPosition source){
                this(source.pinId, source.xPos, source.yPos, source.padId, source.rotation);
            }
            PadPosition(App.PadPosMirror pos){
                this(pos.pinId, pos.xPos, pos.yPos, pos.padId, pos.rotation);
            }

            /*implementatie for interface Jsonable of PadPosition*/
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
                json.put("pin-id", this.pinId);
                json.put("x", this.xPos);
                json.put("y", this.yPos);
                json.put("pad-id", this.padId);
                json.put("rotation", orientationAsInt(rotation));
                json.toJson(writer);
            }
        }
        ArrayList<PadPosition> padPositions;

        /* PadPosition copy method */
        private ArrayList<PadPosition> copyPositions(){
            ArrayList<PadPosition> dest = new ArrayList<>();
            for(PadPosition pos : padPositions){
                dest.add(new PadPosition(pos));
            }
            return dest;
        }

        public void reset(){
            ftprntType = FootprintType.NOMINAL;
            span = new Span();
            outline = new Outline();
        }

        /* Footprint constructors */
        Footprint(){
            ftprntType = FootprintType.NOMINAL;
            span = new Span();
            outline = new Outline();
            dimensions = new ArrayList<>();
            padPositions = new ArrayList<>();
        }
        Footprint(Footprint source){    //for copying
            ftprntType = source.ftprntType;
            span = new Span(source.span);
            outline = new Outline(source.outline);
            if(source.dimensions != null){
                dimensions = source.copyDimensions();
            }
            if(source.padPositions != null){
                padPositions = source.copyPositions();
            }
        }

        /*implementation for interface Jsonable of Footprint*/
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
            json.put("type", footprintTypeasString(this.ftprntType));
            if(!checkZero(this.span.x) || !checkZero(this.span.y)){
                json.put("span", this.span);
            }
            json.put("contour", this.outline);
            if(dimensions != null && dimensions.size() > 0){
                json.put("pad-shapes", this.dimensions);
            }
            if(padPositions != null && padPositions.size() > 0){
                json.put("pad-positions", this.padPositions);
            }
            json.toJson(writer);
        }
    }
    ArrayList<Footprint> footPrints;

    public void addFootprint(Footprint fp){
        footPrints.add(fp);
    }
    public void removeFootprint(Footprint fp){
        footPrints.remove(fp);
    }

    class Reference implements Jsonable{
        String standard;    /* standard document name or number */
        String company;     /* organisation that drafted the standard */
        /* constructor */
        Reference(){
            this("", "");
        }
        Reference(String standard, String company){
            this.standard = standard;
            this.company = company;
        }
        Reference(Reference source){
            this(source.standard, source.company);
        }

        /*implementation for interface Jsonable of Reference*/
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
            json.put("standard", this.standard);
            json.put("organization", this.company);
            json.toJson(writer);
        }
    }
    ArrayList<Reference> references;

    ArrayList<String> relatedPackNames;

    long dateModified;
    public void dateUpdate(){
        dateModified = new Date().getTime();
    }

    private boolean checkZero(double compare){
        double upper = 0 + epsilon;
        double lower = 0 - epsilon;
        return compare < upper && compare > lower;
    }

    public void reset(){
        names = new String[1];
        names[0] = "";

        description = "";

        type = CharType.SMD;

        body = new Body();

        lead2lead = new Lead2Lead();

        nrOfPins = 0;

        pitch = 0.0;

        termination = TermType.ENDCAP;

        polarized = false;

        tapeOrient = Orientation.ZERO;

        specPacks = null;

        footPrints.clear();
        Footprint f = new Footprint();
        f.ftprntType = FootprintType.NOMINAL;
        f.span = f.new Span();
        f.outline = f.new Outline();
        footPrints.add(f);

        references = null;
        relatedPackNames = null;

        dateUpdate();
    }

    public void copy(Package source){
        names = new String[source.names.length];
        System.arraycopy(source.names, 0, names, 0, source.names.length);
        description = source.description;
        type = source.type;
        body = new Body(source.body);
        lead2lead = new Lead2Lead(source.lead2lead);
        nrOfPins = source.nrOfPins;
        pitch = source.pitch;
        termination = source.termination;
        polarized = source.polarized;
        tapeOrient = source.tapeOrient;
        if(source.specPacks != null){
            specPacks = copyVariants(source.specPacks);
        }
        footPrints = copyFootprints(source.footPrints);
        if(source.references != null){
            references = copyReferences(source.references);
        }
        if(source.relatedPackNames != null){
            relatedPackNames = new ArrayList(source.relatedPackNames);
        }
        dateModified = source.dateModified;
    }
    private ArrayList<Variant> copyVariants(ArrayList<Variant> source){
        ArrayList<Variant> dest = new ArrayList();
        source.forEach(spep -> {
            dest.add(new Variant(spep));
        });
        return dest;
    }
    private ArrayList<Footprint> copyFootprints(ArrayList<Footprint> source){
        ArrayList<Footprint> dest = new ArrayList<>();
        source.forEach(f -> {
            dest.add(new Footprint(f));
        });
        return dest;
    }
    private ArrayList<Reference> copyReferences(ArrayList<Reference> source){
        ArrayList<Reference> dest = new ArrayList<>();
        for(Reference r : source){
            dest.add(new Reference(r));
        }
        return dest;
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
        json.put("names", this.names);
        json.put("description", this.description);
        json.put("type", charTypeasString(this.type));
        json.put("terminal", termTypeasString(this.termination));

        if(this.polarized){
            json.put("polarized", this.polarized);
        }

        json.put("body", this.body);

        if(!checkZero(this.lead2lead.x) || !checkZero(this.lead2lead.y) || !checkZero(this.lead2lead.orgX) || !checkZero(this.lead2lead.orgY)){
            json.put("lead-to-lead", this.lead2lead);
        }

        json.put("pin-count", this.nrOfPins);

        if(this.pitch > epsilon){
            json.put("pitch", this.pitch);
        }

        json.put("tape-orientation", orientationAsInt(this.tapeOrient));

        if(this.specPacks != null && this.specPacks.size() > 0){
            json.put("variants", specPacks);
        }
        if(this.footPrints != null && this.footPrints.size() > 0){
            json.put("footprints", this.footPrints);
        }
        if(this.references != null && this.references.size() > 0){
            json.put("references", this.references);
        }
        if(this.relatedPackNames != null && this.relatedPackNames.size() > 0){
            json.put("related packages", this.relatedPackNames);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        json.put("date-modified", sdf.format(this.dateModified));
        json.toJson(writer);
    }

    /* comparator for sorting the list on the first name (in the list of names/aliases) */
    public static Comparator<Package> FirstName = new Comparator<Package>() {
        @Override
        public int compare(Package p1, Package p2) {
            /* the comparator uses three stages, because we want QFN-8 to come
               before QFN-10, but also DO214A to come before DO214BA -> compare
               up to 3 parts:
               1) alphabetic (prefix, e.g. "QFN-")
               2) numeric (middle part, e.g. 8 versus 10)
               3) alphabetic for whatever comes behind the number (like "A" and
                  "BA" in the case of DO214)
            */
            String name1 = p1.names[0].toUpperCase();
            String name2 = p2.names[0].toUpperCase();

            /* quick exit (but should not occur) */
            if (name1.length() == 0)
                return -1;
            else if (name2.length() == 0)
                return 1;

            /* get prefixes: part running up to a digit in case the name starts
               with an alpha character; or part that starts after first run of
               digits in case the name starts with a digit */
            String prefix1 = "";
            int i = 0;
            if (Character.isDigit(name1.charAt(0)))
                while (i < name1.length() && Character.isDigit(name1.charAt(i)))
                    prefix1 += name1.charAt(i++);
            while (i < name1.length() && !Character.isDigit(name1.charAt(i)))
                prefix1 += name1.charAt(i++);
            assert prefix1.length() > 0;    /* should have collected something */

            String prefix2 = "";
            i = 0;
            if (Character.isDigit(name2.charAt(0)))
                while (i < name2.length() && Character.isDigit(name2.charAt(i)))
                    prefix2 += name2.charAt(i++);
            while (i < name2.length() && !Character.isDigit(name2.charAt(i)))
                prefix2 += name2.charAt(i++);
            assert prefix2.length() > 0;

            /* first criterion: compare the prefixes */
            int result = prefix1.compareTo(prefix2);
            if (result != 0)
                return result;

            /* so the prefixes are the same, remove these from the names and
               then compare values (but also check that the remaining names are
               longer than the prefixes */
            if (name1.length() == prefix1.length())
                return -1;
            else if (name2.length() == prefix2.length())
                return 1;
            name1 = name1.substring(prefix1.length());
            name2 = name2.substring(prefix2.length());
            assert Character.isDigit(name1.charAt(0));  /* there must be a digit */
            assert Character.isDigit(name2.charAt(0));  /* there must be a digit */
            prefix1 = "";
            for (i = 0; i < name1.length() && Character.isDigit(name1.charAt(i)); i++)
                prefix1 += name1.charAt(i);
            prefix2 = "";
            for (i = 0; i < name2.length() && Character.isDigit(name2.charAt(i)); i++)
                prefix2 += name2.charAt(i);
            long number1 = Long.parseLong(prefix1);
            long number2 = Long.parseLong(prefix2);
            if (number1 != number2)
                return (number1 < number2) ? -1 : 1;

            /* so the values following the numbers were the same too, end with
               a simple alpha comparison of the remainder */
            name1 = name1.substring(prefix1.length());
            name2 = name2.substring(prefix2.length());
            return name1.compareTo(name2);
        }
    };

    /* constructor */
    Package(){
        names = new String[1];
        names[0] = "";

        description = "";

        type = CharType.SMD;

        body = new Body();

        lead2lead = new Lead2Lead();

        nrOfPins = 0;

        pitch = 0.0;

        termination = TermType.ENDCAP;

        polarized = false;

        tapeOrient = Orientation.ZERO;

        specPacks = new ArrayList<>();

        footPrints = new ArrayList<>();
        footPrints.add(new Footprint());
        footPrints.get(0).ftprntType = FootprintType.NOMINAL;
        footPrints.get(0).span = footPrints.get(0).new Span();
        footPrints.get(0).outline = footPrints.get(0).new Outline();

        references = new ArrayList<>();
        relatedPackNames = new ArrayList<>();

        dateUpdate();
    }
}
