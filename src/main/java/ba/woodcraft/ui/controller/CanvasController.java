package ba.woodcraft.ui.controller;

import ba.woodcraft.export.CanvasDocument;
import ba.woodcraft.export.ExportFormat;
import ba.woodcraft.export.ExportServiceRegistry;
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
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;

public class CanvasController {

    private enum Tool {
        SELECT,
        FREEHAND,
        LINE,
        RECTANGLE,
        CIRCLE,
        BEZIER
    }

    private enum BezierStage {
        NONE,
        END,
        CONTROL
    }

    @FXML private StackPane canvasHost;
    @FXML private Group zoomGroup;
    @FXML private Pane drawingPane;
    @FXML private Canvas topRuler;
    @FXML private Canvas leftRuler;

    @FXML private ToggleButton lineTool;
    @FXML private ToggleButton rectangleTool;
    @FXML private ToggleButton circleTool;
    @FXML private ToggleButton bezierTool;
    @FXML private ToggleButton selectTool;

    private Tool activeTool = Tool.FREEHAND;
    private Drawable activeShape;
    private BezierCurveShape activeBezier;
    private BezierStage bezierStage = BezierStage.NONE;
    private Node selectedNode;
    private SelectionOverlay selectionOverlay;
    private Circle snapIndicator;
    private Point2D snapPoint;
    private final ExportServiceRegistry exportServiceRegistry = new ExportServiceRegistry();

    private static final double SNAP_RADIUS = 10.0;
    private static final double SNAP_INDICATOR_RADIUS = 4.0;
    private static final double RULER_SIZE = 24.0;
    private static final double RULER_MAJOR_TICK = 50.0;
    private static final double RULER_MINOR_TICK = 10.0;

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
        selectTool.setToggleGroup(toolGroup);

        // Default: no tool selected => FREEHAND
        toolGroup.selectToggle(null);
        activeTool = Tool.FREEHAND;

        // Allow "click again to return to freehand"
        allowDeselectToFreehand(lineTool, toolGroup);
        allowDeselectToFreehand(rectangleTool, toolGroup);
        allowDeselectToFreehand(circleTool, toolGroup);
        allowDeselectToFreehand(bezierTool, toolGroup);
        allowDeselectToFreehand(selectTool, toolGroup);

        toolGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                activeTool = Tool.FREEHAND;
                resetBezierState();
                clearSelection();
                selectionOverlay.setActive(false);
                hideSnapIndicator();
            } else if (newToggle == selectTool) {
                activeTool = Tool.SELECT;
                resetBezierState();
                selectionOverlay.setActive(true);
                hideSnapIndicator();
            } else {
                clearSelection();
                selectionOverlay.setActive(false);
                hideSnapIndicator();
                if (newToggle == lineTool) activeTool = Tool.LINE;
                else if (newToggle == rectangleTool) activeTool = Tool.RECTANGLE;
                else if (newToggle == circleTool) activeTool = Tool.CIRCLE;
                else if (newToggle == bezierTool) activeTool = Tool.BEZIER;
                if (activeTool != Tool.BEZIER) {
                    resetBezierState();
                }
            }
        });

        zoomGroup.setPickOnBounds(false);
        drawingPane.setPickOnBounds(true);
        canvasHost.setAlignment(Pos.CENTER);
        StackPane.setAlignment(zoomGroup, Pos.CENTER);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasHost.widthProperty());
        clip.heightProperty().bind(canvasHost.heightProperty());
        canvasHost.setClip(clip);

        selectionOverlay = new SelectionOverlay();
        selectionOverlay.attachTo(drawingPane);
        selectionOverlay.setActive(false);

        snapIndicator = new Circle(SNAP_INDICATOR_RADIUS);
        snapIndicator.setFill(Color.WHITE);
        snapIndicator.setStroke(Color.web("#3b82f6"));
        snapIndicator.setStrokeWidth(1.2);
        snapIndicator.setManaged(false);
        snapIndicator.setMouseTransparent(true);
        snapIndicator.setVisible(false);
        drawingPane.getChildren().add(snapIndicator);

        topRuler.setHeight(RULER_SIZE);
        leftRuler.setWidth(RULER_SIZE);
        topRuler.widthProperty().bind(canvasHost.widthProperty());
        leftRuler.heightProperty().bind(canvasHost.heightProperty());
        topRuler.heightProperty().addListener((obs, oldValue, newValue) -> drawRulers());
        topRuler.widthProperty().addListener((obs, oldValue, newValue) -> drawRulers());
        leftRuler.heightProperty().addListener((obs, oldValue, newValue) -> drawRulers());
        leftRuler.widthProperty().addListener((obs, oldValue, newValue) -> drawRulers());

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
        drawRulers();
    }

    @FXML
    public void onClear() {
        drawingPane.getChildren().clear();
        selectionOverlay.attachTo(drawingPane);
        drawingPane.getChildren().add(snapIndicator);
        clearSelection();
        selectionOverlay.setActive(activeTool == Tool.SELECT);
        hideSnapIndicator();
        resetBezierState();
    }

    @FXML
    public void onLogout() {
        SceneNavigator.show("view/login.fxml");
    }

    @FXML
    public void onExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                ExportFormat.PDF.getDescription(),
                ExportFormat.PDF.getExtensionPattern()
        ));
        File file = chooser.showSaveDialog(canvasHost.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            CanvasDocument document = new CanvasDocument(drawingPane, snapIndicator, selectionOverlay);
            exportServiceRegistry.export(ExportFormat.PDF, document, file);
        } catch (IOException | IllegalStateException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export PDF: " + ex.getMessage(), ButtonType.OK);
            alert.setHeaderText("Export failed");
            alert.showAndWait();
        }
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
        if (activeTool == Tool.SELECT) {
            Node picked = findSelectableNode(event);
            if (picked == null) clearSelection();
            else setSelectedNode(picked);
            return;
        }

        clearSelection();
        Point2D p = snapPoint != null ? snapPoint : getCanvasPoint(event);
        if (activeTool == Tool.BEZIER) {
            handleBezierPress(p);
        } else {
            activeShape = createShape(p.getX(), p.getY());
            if (activeShape != null) drawingPane.getChildren().add(activeShape.getNode());
        }
        hideSnapIndicator();
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        if (activeTool == Tool.SELECT) {
            return;
        }
        if (activeTool == Tool.BEZIER) {
            updateBezierPreview(getCanvasPoint(event));
            return;
        }
        if (activeShape != null) {
            Point2D p = getCanvasPoint(event);
            activeShape.update(p.getX(), p.getY());
        }
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        if (activeTool == Tool.SELECT) {
            return;
        }
        if (activeTool == Tool.BEZIER) {
            return;
        }
        if (activeShape != null) {
            Point2D p = getCanvasPoint(event);
            activeShape.update(p.getX(), p.getY());
            activeShape = null;
        }
        hideSnapIndicator();
    }

    @FXML
    public void onMouseMoved(MouseEvent event) {
        if (!isSnapToolActive() || activeShape != null || (activeTool == Tool.BEZIER && bezierStage != BezierStage.NONE)) {
            hideSnapIndicator();
            return;
        }
        Point2D cursor = getCanvasPoint(event);
        Point2D point = findSnapPoint(cursor);
        if (point != null) {
            showSnapIndicator(point);
        } else {
            hideSnapIndicator();
        }
    }

    @FXML
    public void onMouseExited(MouseEvent event) {
        hideSnapIndicator();
    }

    private void setSelectedNode(Node node) {
        selectedNode = node;
        selectionOverlay.setTarget(node);
    }

    private void clearSelection() {
        selectedNode = null;
        selectionOverlay.clear();
    }

    private boolean isSnapToolActive() {
        return activeTool != Tool.SELECT && activeTool != Tool.FREEHAND;
    }

    private Node findSelectableNode(MouseEvent event) {
        Point2D scenePoint = new Point2D(event.getSceneX(), event.getSceneY());
        for (int i = drawingPane.getChildren().size() - 1; i >= 0; i--) {
            Node node = drawingPane.getChildren().get(i);
            if (!node.isVisible() || selectionOverlay.isOverlayNode(node) || node == snapIndicator) {
                continue;
            }
            Point2D localPoint = node.sceneToLocal(scenePoint);
            if (node.contains(localPoint)) {
                return node;
            }
        }
        return null;
    }

    private Drawable createShape(double x, double y) {
        return switch (activeTool) {
            case FREEHAND -> new FreehandShape(x, y);
            case LINE -> new LineShape(x, y);
            case RECTANGLE -> new RectangleShape(x, y);
            case CIRCLE -> new CircleShape(x, y);
            case BEZIER -> new BezierCurveShape(x, y);
            case SELECT -> null;
        };
    }

    private void handleBezierPress(Point2D point) {
        if (bezierStage == BezierStage.NONE) {
            activeBezier = new BezierCurveShape(point.getX(), point.getY());
            drawingPane.getChildren().add(activeBezier.getNode());
            bezierStage = BezierStage.END;
            return;
        }
        if (bezierStage == BezierStage.END && activeBezier != null) {
            activeBezier.setEnd(point.getX(), point.getY());
            bezierStage = BezierStage.CONTROL;
            return;
        }
        if (bezierStage == BezierStage.CONTROL && activeBezier != null) {
            activeBezier.setControlPoints(point.getX(), point.getY());
            activeBezier = null;
            bezierStage = BezierStage.NONE;
        }
    }

    private void updateBezierPreview(Point2D point) {
        if (activeBezier == null) {
            return;
        }
        if (bezierStage == BezierStage.END) {
            activeBezier.setEnd(point.getX(), point.getY());
        } else if (bezierStage == BezierStage.CONTROL) {
            activeBezier.setControlPoints(point.getX(), point.getY());
        }
    }

    private void resetBezierState() {
        activeBezier = null;
        bezierStage = BezierStage.NONE;
    }

    private void showSnapIndicator(Point2D point) {
        snapPoint = point;
        snapIndicator.setCenterX(point.getX());
        snapIndicator.setCenterY(point.getY());
        snapIndicator.setVisible(true);
        snapIndicator.toFront();
    }

    private void hideSnapIndicator() {
        snapPoint = null;
        snapIndicator.setVisible(false);
    }

    private Point2D findSnapPoint(Point2D cursor) {
        double bestDistance = SNAP_RADIUS;
        Point2D best = null;
        for (Node node : drawingPane.getChildren()) {
            if (!node.isVisible() || node == snapIndicator || selectionOverlay.isOverlayNode(node)) {
                continue;
            }

            if (node instanceof Line line) {
                Point2D start = line.localToParent(line.getStartX(), line.getStartY());
                Point2D end = line.localToParent(line.getEndX(), line.getEndY());
                best = updateBestPoint(cursor, start, best, bestDistance);
                bestDistance = best == null ? bestDistance : cursor.distance(best);
                best = updateBestPoint(cursor, end, best, bestDistance);
                bestDistance = best == null ? bestDistance : cursor.distance(best);
                Point2D nearest = nearestPointOnSegment(start, end, cursor);
                best = updateBestPoint(cursor, nearest, best, bestDistance);
                bestDistance = best == null ? bestDistance : cursor.distance(best);
            } else if (node instanceof CubicCurve curve) {
                Point2D start = curve.localToParent(curve.getStartX(), curve.getStartY());
                Point2D end = curve.localToParent(curve.getEndX(), curve.getEndY());
                best = updateBestPoint(cursor, start, best, bestDistance);
                bestDistance = best == null ? bestDistance : cursor.distance(best);
                best = updateBestPoint(cursor, end, best, bestDistance);
                bestDistance = best == null ? bestDistance : cursor.distance(best);
            }

            Point2D boundsPoint = nearestPointOnBounds(node.getBoundsInParent(), cursor);
            best = updateBestPoint(cursor, boundsPoint, best, bestDistance);
            bestDistance = best == null ? bestDistance : cursor.distance(best);
        }
        return best;
    }

    private Point2D updateBestPoint(Point2D cursor, Point2D candidate, Point2D currentBest, double currentDistance) {
        if (candidate == null) {
            return currentBest;
        }
        double distance = cursor.distance(candidate);
        if (distance <= currentDistance) {
            return candidate;
        }
        return currentBest;
    }

    private Point2D nearestPointOnSegment(Point2D start, Point2D end, Point2D point) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return start;
        }
        double t = ((point.getX() - start.getX()) * dx + (point.getY() - start.getY()) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return new Point2D(start.getX() + t * dx, start.getY() + t * dy);
    }

    private Point2D nearestPointOnBounds(javafx.geometry.Bounds bounds, Point2D point) {
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        double x = clamp(point.getX(), minX, maxX);
        double y = clamp(point.getY(), minY, maxY);

        boolean insideX = point.getX() >= minX && point.getX() <= maxX;
        boolean insideY = point.getY() >= minY && point.getY() <= maxY;
        if (insideX && insideY) {
            double leftDist = point.getX() - minX;
            double rightDist = maxX - point.getX();
            double topDist = point.getY() - minY;
            double bottomDist = maxY - point.getY();
            double minDist = Math.min(Math.min(leftDist, rightDist), Math.min(topDist, bottomDist));
            if (minDist == leftDist) {
                x = minX;
                y = point.getY();
            } else if (minDist == rightDist) {
                x = maxX;
                y = point.getY();
            } else if (minDist == topDist) {
                x = point.getX();
                y = minY;
            } else {
                x = point.getX();
                y = maxY;
            }
        }
        return new Point2D(x, y);
    }

    private void drawRulers() {
        drawTopRuler();
        drawLeftRuler();
    }

    private void drawTopRuler() {
        if (topRuler == null) {
            return;
        }
        GraphicsContext gc = topRuler.getGraphicsContext2D();
        double width = topRuler.getWidth();
        double height = topRuler.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#f3f4f6"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.web("#6b7280"));
        gc.setLineWidth(1.0);
        gc.setFill(Color.web("#374151"));

        double maxUnits = width / zoom;
        for (double unit = 0; unit <= maxUnits; unit += RULER_MINOR_TICK) {
            double x = unit * zoom;
            if (x > width) {
                break;
            }
            boolean major = unit % RULER_MAJOR_TICK == 0;
            double tickHeight = major ? height : height * 0.6;
            gc.strokeLine(x + 0.5, height, x + 0.5, height - tickHeight);
            if (major) {
                gc.fillText(String.valueOf((int) unit), x + 2, height - tickHeight - 2);
            }
        }
    }

    private void drawLeftRuler() {
        if (leftRuler == null) {
            return;
        }
        GraphicsContext gc = leftRuler.getGraphicsContext2D();
        double width = leftRuler.getWidth();
        double height = leftRuler.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#f3f4f6"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.web("#6b7280"));
        gc.setLineWidth(1.0);
        gc.setFill(Color.web("#374151"));

        double maxUnits = height / zoom;
        for (double unit = 0; unit <= maxUnits; unit += RULER_MINOR_TICK) {
            double y = unit * zoom;
            if (y > height) {
                break;
            }
            boolean major = unit % RULER_MAJOR_TICK == 0;
            double tickWidth = major ? width : width * 0.6;
            gc.strokeLine(width, y + 0.5, width - tickWidth, y + 0.5);
            if (major) {
                gc.fillText(String.valueOf((int) unit), 2, y - 2);
            }
        }
    }

}
