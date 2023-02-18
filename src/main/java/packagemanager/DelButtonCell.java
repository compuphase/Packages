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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import java.io.File;

/**
 *
 * @author Guido Wolff
 * @param <T>
 */
public class DelButtonCell<T extends Removable> extends TableCell<T, Boolean>{
    final Button cellButton = new Button();

    DelButtonCell(final TableView tblView){
        /* Set button graphic */
        String path = App.getResourcePath();
        path += "/b_drop.png";
        File file = new File(path);
        if(file.exists()){
            Image icon;
            try{
                icon = new Image(file.toURI().toString());
                cellButton.setGraphic(new ImageView(icon));
            } catch(Exception e){
                System.out.println("DelButtonCell Image failed to load!");
            }
        }

        /* set styles (for hover/click) */
        final String styleIdle = "-fx-padding:2px 8px 2px 8px; -fx-background-color:transparent; -fx-border-radius:4px; -fx-border-width:0px;";
        final String styleHover = "-fx-padding:2px 8px 2px 8px; -fx-background-color:lightblue; -fx-border-radius:4px; -fx-border-width:0px;";
        final String styleClick =  "-fx-padding:2px 8px 2px 8px; -fx-background-color:#9DC8D6; -fx-border-radius:4px; -fx-border-width:0px;";
        cellButton.setStyle(styleIdle);
        cellButton.setOnMouseEntered((MouseEvent arg0) -> {
            cellButton.setStyle(styleHover);
        });
        cellButton.setOnMouseExited((MouseEvent arg0) -> {
            cellButton.setStyle(styleIdle);
        });
        cellButton.setOnMousePressed((MouseEvent arg0) -> {
            cellButton.setStyle(styleClick);
        });
        cellButton.setOnMouseReleased((MouseEvent arg0) -> {
            cellButton.setStyle(styleHover);
        });

        /* Set button tooltip */
        Tooltip tooltip = new Tooltip("Delete this entry");
        cellButton.setTooltip(tooltip);

        /* Set button action */
        cellButton.setOnAction(new EventHandler<ActionEvent>(){
             @Override
            public void handle(ActionEvent t) {
                int selectedIndex = getTableRow().getIndex();
                T toRemove = (T) tblView.getItems().get(selectedIndex);
                toRemove.remove();
                tblView.getItems().remove(toRemove);
            }
        });
    }

    //Display button if the row is not empty
    @Override
    protected void updateItem(Boolean t, boolean empty) {
        super.updateItem(t, empty);
        if(!empty){
            setGraphic(cellButton);
        } else{
            setGraphic(null);
        }
    }
}