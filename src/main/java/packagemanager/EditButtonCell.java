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
import java.io.File;

/**
 *
 * @author Guido Wolff
 * @param <T>
 */
public class EditButtonCell<T extends Editable> extends TableCell<T, Boolean>{
    final Button cellButton = new Button();

    EditButtonCell(final TableView tblView){
        /* Set button graphic */
        String path = App.getResourcePath();
        path += "/b_edit.png";
        File file = new File(path);
        if(file.exists()){
            Image icon;
            try{
                icon = new Image(file.toURI().toString());
                cellButton.setGraphic(new ImageView(icon));
            } catch(Exception e){
                System.out.println("EditButtonCell Image failed to load!");
            }
        }
        cellButton.setStyle("-fx-padding:2px 8px 2px 8px;");
        /* Set button tooltip */
        Tooltip tooltip = new Tooltip("Edit polygon");
        cellButton.setTooltip(tooltip);

        /* Set button action */
        cellButton.setOnAction(new EventHandler<ActionEvent>(){
             @Override
            public void handle(ActionEvent t) {
                int selectedIndex = getTableRow().getIndex();
                T toEdit = (T) tblView.getItems().get(selectedIndex);
                toEdit.edit();
            }
        });
    }

    //Display button if the row is not empty
    @Override
    protected void updateItem(Boolean b, boolean empty) {
        super.updateItem(b, empty);
        if(!empty && b){
            setGraphic(cellButton);
        } else{
            setGraphic(null);
        }
    }
}