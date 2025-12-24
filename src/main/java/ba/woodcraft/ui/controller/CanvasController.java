package ba.woodcraft.ui.controller;

import ba.woodcraft.model.BezierCurveShape;
import ba.woodcraft.model.Drawable;
import ba.woodcraft.model.LineShape;
import ba.woodcraft.model.RectangleShape;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ba.woodcraft.ui.controller.SceneNavigator;

public class CanvasController {

    private enum Tool {
        LINE,
        RECTANGLE,
        BEZIER
    }

    @FXML private Pane drawingPane;
    @FXML private ToggleButton lineTool;
    @FXML private ToggleButton rectangleTool;
    @FXML private ToggleButton bezierTool;

    private Tool activeTool = Tool.LINE;
    private Drawable activeShape;

    @FXML
    public void initialize() {
        ToggleGroup toolGroup = new ToggleGroup();
        lineTool.setToggleGroup(toolGroup);
        rectangleTool.setToggleGroup(toolGroup);
        bezierTool.setToggleGroup(toolGroup);
        lineTool.setSelected(true);

        toolGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == lineTool) {
                activeTool = Tool.LINE;
            } else if (newToggle == rectangleTool) {
                activeTool = Tool.RECTANGLE;
            } else if (newToggle == bezierTool) {
                activeTool = Tool.BEZIER;
            }
        });
    }

    @FXML
    public void onClear() {
        drawingPane.getChildren().clear();
    }

    @FXML
    public void onLogout() {
        SceneNavigator.show("view/login.fxml");
    }

    @FXML
    public void onMousePressed(MouseEvent event) {
        activeShape = createShape(event.getX(), event.getY());
        if (activeShape != null) {
            drawingPane.getChildren().add(activeShape.getNode());
        }
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        if (activeShape != null) {
            activeShape.update(event.getX(), event.getY());
        }
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        if (activeShape != null) {
            activeShape.update(event.getX(), event.getY());
            activeShape = null;
        }
    }

    private Drawable createShape(double x, double y) {
        return switch (activeTool) {
            case LINE -> new LineShape(x, y);
            case RECTANGLE -> new RectangleShape(x, y);
            case BEZIER -> new BezierCurveShape(x, y);
        };
    }
}
