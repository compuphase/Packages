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

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.application.Platform;
import javafx.event.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.*;
import javafx.scene.control.cell.CheckBoxTableCell;


/**
 *
 * @author Guido Daniel Wolff
 */

/* A custom TextFieldTableCell that edits on focus change */
public class CustomCell extends TableCell<Object, String> {

    private TextField textField;

    public CustomCell() {
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            if (textField == null) {
                createTextField();
            } else {
                textField.setText(getString());
            }

            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            // textField.selectAll();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    textField.requestFocus();
                    textField.selectAll();
                }
            });
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText((String) getItem());
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(getString());
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
        textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0,
                Boolean arg1, Boolean arg2) {
                    if (!arg2) {
                        commitEdit(textField.getText());
                    }
            }
        });
        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() != null) switch (t.getCode()) {
                    case ENTER:
                        commitEdit(textField.getText());
                        TableColumn nextColumn = getNextColumn(!t.isShiftDown());
                        if (nextColumn != null) {
                            getTableView().edit(getTableRow().getIndex(), nextColumn);
                        }
                        break;
                    case ESCAPE:
                        cancelEdit();
                        break;
                    case TAB:
                        commitEdit(textField.getText());
                        nextColumn = getNextColumn(!t.isShiftDown());
                        if (nextColumn != null) {
                            getTableView().edit(getTableRow().getIndex(), nextColumn);
                        }   break;
                    default:
                        break;
                }
            }
        });
    }

    private String getString() {
        return getItem() == null ? "" : getItem();
    }

    private TableColumn<Object, ?> getNextColumn(boolean forward) {
        List<TableColumn<Object, ?>> columns = new ArrayList<>();
        for (TableColumn<Object, ?> column : getTableView().getColumns()) {
            columns.addAll(getLeaves(column));
        }
        // There is no other column that supports editing.
        if (columns.size() < 2) {
            return null;
        }
        int currentIndex = columns.indexOf(getTableColumn());
        int nextIndex = currentIndex;
        if (forward) {
                nextIndex++;
                if (nextIndex > columns.size() - 1) {
                    nextIndex = 0;
                }
        } else {
            nextIndex--;
            if (nextIndex < 0) {
                nextIndex = columns.size() - 1;
            }
        }
        return columns.get(nextIndex);
    }

    private List<TableColumn<Object, ?>> getLeaves(TableColumn<Object, ?> root) {
        List<TableColumn<Object, ?>> columns = new ArrayList<>();
        if (root.getColumns().isEmpty()) {
            // We only want the leaves that are editable.
            if (root.isEditable()) {
                columns.add(root);
            }
            return columns;
        } else{
            for (TableColumn<Object, ?> column : root.getColumns()) {
                columns.addAll(getLeaves(column));
            }
            return columns;
        }
    }
}


class EditingDoubleCell extends TableCell<Object, Double> {

    private TextField textField;

    public EditingDoubleCell() {
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            if (textField == null) {
                createTextField();
            } else {
                textField.setText(getString());
            }

            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            // textField.selectAll();
            Platform.runLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText((String) Double.toString(getItem()));
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    public void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(getString());
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
        textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0,
                Boolean arg1, Boolean arg2) {
                    if (!arg2) {
                        commitEdit(Double.parseDouble(textField.getText()));
                    }
            }
        });
        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() != null) switch (t.getCode()) {
                    case ENTER:
                        commitEdit(Double.parseDouble(textField.getText()));
                        TableColumn nextColumn = getNextColumn(!t.isShiftDown());
                        if (nextColumn != null) {
                            getTableView().edit(getTableRow().getIndex(), nextColumn);
                        }
                        break;
                    case ESCAPE:
                        cancelEdit();
                        break;
                    case TAB:
                        commitEdit(Double.parseDouble(textField.getText()));
                        nextColumn = getNextColumn(!t.isShiftDown());
                        if (nextColumn != null) {
                            getTableView().edit(getTableRow().getIndex(), nextColumn);
                        }   break;
                    default:
                        break;
                }
            }
        });
    }

    private String getString() {
        if(getItem() != null){
            return Double.toString(getItem());
        } else {
            return "";
        }
    }

    private TableColumn<Object, ?> getNextColumn(boolean forward) {
        List<TableColumn<Object, ?>> columns = new ArrayList<>();
        for (TableColumn<Object, ?> column : getTableView().getColumns()) {
            columns.addAll(getLeaves(column));
        }
        // There is no other column that supports editing.
        if (columns.size() < 2) {
            return null;
        }
        int currentIndex = columns.indexOf(getTableColumn());
        int nextIndex = currentIndex;
        if (forward) {
                nextIndex++;
                if (nextIndex > columns.size() - 1) {
                    nextIndex = 0;
                }
        } else {
            nextIndex--;
            if (nextIndex < 0) {
                nextIndex = columns.size() - 1;
            }
        }
        return columns.get(nextIndex);
    }

    private List<TableColumn<Object, ?>> getLeaves(TableColumn<Object, ?> root) {
        List<TableColumn<Object, ?>> columns = new ArrayList<>();
        if (root.getColumns().isEmpty()) {
            // We only want the leaves that are editable.
            if (root.isEditable()) {
                columns.add(root);
            }
            return columns;
        } else{
            for (TableColumn<Object, ?> column : root.getColumns()) {
                columns.addAll(getLeaves(column));
            }
            return columns;
        }
    }
}

class EditingIntCell extends TableCell<Object, Integer> {

    private TextField textField;

    public EditingIntCell() {
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            if (textField == null) {
                createTextField();
            } else {
                textField.setText(getString());
            }

            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            Platform.runLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText((String) Integer.toString(getItem()));
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    public void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(getString());
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
        textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0,
                Boolean arg1, Boolean arg2) {
                    if (!arg2) {
                        commitEdit(Integer.parseInt(textField.getText()));
                    }
            }
        });
        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() != null) switch (t.getCode()) {
                    case ENTER:
                        commitEdit(Integer.parseInt(textField.getText()));
                        TableColumn nextColumn = getNextColumn(!t.isShiftDown());
                        if (nextColumn != null) {
                            getTableView().edit(getTableRow().getIndex(), nextColumn);
                        }
                        break;
                    case ESCAPE:
                        cancelEdit();
                        break;
                    case TAB:
                        commitEdit(Integer.parseInt(textField.getText()));
                        nextColumn = getNextColumn(!t.isShiftDown());
                        if (nextColumn != null) {
                            getTableView().edit(getTableRow().getIndex(), nextColumn);
                        }   break;
                    default:
                        break;
                }
            }
        });
    }

    private String getString() {
        if(getItem() != null){
            return Integer.toString(getItem());
        } else {
            return "";
        }
    }

    private TableColumn<Object, ?> getNextColumn(boolean forward) {
        List<TableColumn<Object, ?>> columns = new ArrayList<>();
        for (TableColumn<Object, ?> column : getTableView().getColumns()) {
            columns.addAll(getLeaves(column));
        }
        // There is no other column that supports editing.
        if (columns.size() < 2) {
            return null;
        }
        int currentIndex = columns.indexOf(getTableColumn());
        int nextIndex = currentIndex;
        if (forward) {
                nextIndex++;
                if (nextIndex > columns.size() - 1) {
                    nextIndex = 0;
                }
        } else {
            nextIndex--;
            if (nextIndex < 0) {
                nextIndex = columns.size() - 1;
            }
        }
        return columns.get(nextIndex);
    }

    private List<TableColumn<Object, ?>> getLeaves(TableColumn<Object, ?> root) {
        List<TableColumn<Object, ?>> columns = new ArrayList<>();
        if (root.getColumns().isEmpty()) {
            // We only want the leaves that are editable.
            if (root.isEditable()) {
                columns.add(root);
            }
            return columns;
        } else{
            for (TableColumn<Object, ?> column : root.getColumns()) {
                columns.addAll(getLeaves(column));
            }
            return columns;
        }
    }
}


class CustomCheckBoxCell extends CheckBoxTableCell {

    public CustomCheckBoxCell(){
        super();

        EventHandler<KeyEvent> keyEventHandler = new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event){
                if(event.getCode() == KeyCode.TAB){
                    TableColumn nextColumn = getNextColumn(!event.isShiftDown());
                    if (nextColumn != null){
                        getTableView().edit(getTableRow().getIndex(), nextColumn);
                    }
                }
            }
        };

        this.setEventHandler(KeyEvent.KEY_PRESSED,keyEventHandler);
    }


    private TableColumn getNextColumn(boolean forward) {
        List<TableColumn> columns = new ArrayList<>();
        for (var column : getTableView().getColumns()) {
            columns.addAll(getLeaves((TableColumn<Object, ?>) column));
        }
        // There is no other column that supports editing.
        if (columns.size() < 2) {
            return null;
        }
        int currentIndex = columns.indexOf(getTableColumn());
        int nextIndex = currentIndex;
        if (forward) {
            nextIndex++;
            if (nextIndex > columns.size() - 1) {
                nextIndex = 0;
            }
        } else {
            nextIndex--;
            if (nextIndex < 0) {
                nextIndex = columns.size() - 1;
            }
        }
        return columns.get(nextIndex);
    }

	private List<TableColumn<Object, ?>> getLeaves(TableColumn<Object, ?> root) {
            List<TableColumn<Object, ?>> columns = new ArrayList<>();
            if (root.getColumns().isEmpty()) {
                // We only want the leaves that are editable.
                if (root.isEditable()) {
                    columns.add(root);
                }
                return columns;
            } else{
                for (TableColumn<Object, ?> column : root.getColumns()) {
                    columns.addAll(getLeaves(column));
                }
                return columns;
            }
        }
}
