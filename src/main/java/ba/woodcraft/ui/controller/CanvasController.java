package ba.woodcraft.ui.controller;

import ba.woodcraft.model.BezierCurveShape;
import ba.woodcraft.model.CircleShape;
import ba.woodcraft.model.Drawable;
import ba.woodcraft.model.FreehandShape;
import ba.woodcraft.model.LineShape;
import ba.woodcraft.model.RectangleShape;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class CanvasController {

    private enum Tool {
        FREEHAND,
        LINE,
        RECTANGLE,
        CIRCLE,
        BEZIER
    }

    @FXML private StackPane canvasHost;
    @FXML private Group zoomGroup;
    @FXML private Pane drawingPane;

    @FXML private ToggleButton lineTool;
    @FXML private ToggleButton rectangleTool;
    @FXML private ToggleButton circleTool;
    @FXML private ToggleButton bezierTool;

    private Tool activeTool = Tool.FREEHAND;
    private Drawable activeShape;

    private double zoom = 1.0;
    private static final double ZOOM_STEP = 1.1;
    private static final double ZOOM_MIN = 0.3;
    private static final double ZOOM_MAX = 4.0;

    @FXML
    public void initialize() {
        ToggleGroup toolGroup = new ToggleGroup();
        lineTool.setToggleGroup(toolGroup);
        rectangleTool.setToggleGroup(toolGroup);
        circleTool.setToggleGroup(toolGroup);
        bezierTool.setToggleGroup(toolGroup);

        // Default: no tool selected => FREEHAND
        toolGroup.selectToggle(null);
        activeTool = Tool.FREEHAND;

        // Allow "click again to return to freehand"
        allowDeselectToFreehand(lineTool, toolGroup);
        allowDeselectToFreehand(rectangleTool, toolGroup);
        allowDeselectToFreehand(circleTool, toolGroup);
        allowDeselectToFreehand(bezierTool, toolGroup);

        toolGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) activeTool = Tool.FREEHAND;
            else if (newToggle == lineTool) activeTool = Tool.LINE;
            else if (newToggle == rectangleTool) activeTool = Tool.RECTANGLE;
            else if (newToggle == circleTool) activeTool = Tool.CIRCLE;
            else if (newToggle == bezierTool) activeTool = Tool.BEZIER;
        });

        zoomGroup.setPickOnBounds(false);
        drawingPane.setPickOnBounds(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasHost.widthProperty());
        clip.heightProperty().bind(canvasHost.heightProperty());
        canvasHost.setClip(clip);

        applyZoom();
    }

    private void allowDeselectToFreehand(ToggleButton btn, ToggleGroup group) {
        btn.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (btn.isSelected()) {
                // prevent default behavior (which would keep it selected)
                e.consume();
                group.selectToggle(null); // => FREEHAND
            }
        });
    }

    @FXML
    public void onZoomIn(ActionEvent event) {
        zoom = Math.min(ZOOM_MAX, zoom * ZOOM_STEP);
        applyZoom();
    }

    @FXML
    public void onZoomOut(ActionEvent event) {
        zoom = Math.max(ZOOM_MIN, zoom / ZOOM_STEP);
        applyZoom();
    }

    @FXML
    public void onZoomReset(ActionEvent event) {
        zoom = 1.0;
        applyZoom();
    }

    private void applyZoom() {
        zoomGroup.setScaleX(zoom);
        zoomGroup.setScaleY(zoom);
    }

    @FXML
    public void onClear() {
        drawingPane.getChildren().clear();
    }

    @FXML
    public void onLogout() {
        SceneNavigator.show("view/login.fxml");
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(v, max));
    }

    private Point2D getCanvasPoint(MouseEvent event) {
        Point2D local = drawingPane.sceneToLocal(event.getSceneX(), event.getSceneY());
        double w = drawingPane.getPrefWidth();
        double h = drawingPane.getPrefHeight();
        return new Point2D(
                clamp(local.getX(), 0, w),
                clamp(local.getY(), 0, h)
        );
    }

    @FXML
    public void onMousePressed(MouseEvent event) {
        Point2D p = getCanvasPoint(event);
        activeShape = createShape(p.getX(), p.getY());
        if (activeShape != null) drawingPane.getChildren().add(activeShape.getNode());
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        if (activeShape != null) {
            Point2D p = getCanvasPoint(event);
            activeShape.update(p.getX(), p.getY());
        }
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        if (activeShape != null) {
            Point2D p = getCanvasPoint(event);
            activeShape.update(p.getX(), p.getY());
            activeShape = null;
        }
    }

    private Drawable createShape(double x, double y) {
        return switch (activeTool) {
            case FREEHAND -> new FreehandShape(x, y);
            case LINE -> new LineShape(x, y);
            case RECTANGLE -> new RectangleShape(x, y);
            case CIRCLE -> new CircleShape(x, y);
            case BEZIER -> new BezierCurveShape(x, y);
        };
    }
}
