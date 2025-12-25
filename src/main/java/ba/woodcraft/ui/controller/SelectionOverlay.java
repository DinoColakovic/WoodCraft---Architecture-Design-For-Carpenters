package ba.woodcraft.ui.controller;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;

public class SelectionOverlay {

    private static final double HANDLE_SIZE = 8.0;
    private static final double ROTATION_HANDLE_OFFSET = 20.0;

    private final Group overlayGroup = new Group();
    private final Rectangle outline = new Rectangle();
    private final Circle rotateHandle = new Circle(HANDLE_SIZE / 2.0);
    private final Rectangle[] handles = new Rectangle[8];

    private Node target;
    private Pane host;
    private boolean active;
    private DragMode dragMode = DragMode.NONE;
    private Point2D dragStartParent;
    private Bounds dragStartBounds;
    private double startLayoutX;
    private double startLayoutY;
    private double startTranslateX;
    private double startTranslateY;
    private double startScaleX;
    private double startScaleY;
    private double startRotate;
    private double anchorX;
    private double anchorY;
    private Point2D lineStartParent;
    private Point2D lineEndParent;
    private Point2D quadStartParent;
    private Point2D quadEndParent;
    private Point2D quadControlParent;

    private enum DragMode {
        NONE,
        MOVE,
        ROTATE,
        RESIZE_NW,
        RESIZE_N,
        RESIZE_NE,
        RESIZE_E,
        RESIZE_SE,
        RESIZE_S,
        RESIZE_SW,
        RESIZE_W
    }

    public SelectionOverlay() {
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.web("#3b82f6"));
        outline.setStrokeWidth(1.5);
        outline.setCursor(javafx.scene.Cursor.MOVE);

        rotateHandle.setFill(Color.WHITE);
        rotateHandle.setStroke(Color.web("#3b82f6"));
        rotateHandle.setStrokeWidth(1.5);
        rotateHandle.setCursor(javafx.scene.Cursor.CROSSHAIR);

        for (int i = 0; i < handles.length; i++) {
            Rectangle handle = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
            handle.setFill(Color.WHITE);
            handle.setStroke(Color.web("#3b82f6"));
            handle.setStrokeWidth(1.2);
            handles[i] = handle;
        }

        overlayGroup.getChildren().add(outline);
        overlayGroup.getChildren().add(rotateHandle);
        overlayGroup.getChildren().addAll(handles);

        registerHandlers();
        setVisible(false);
    }

    public void attachTo(Pane pane) {
        host = pane;
        if (!pane.getChildren().contains(overlayGroup)) {
            pane.getChildren().add(overlayGroup);
        }
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            clear();
        }
    }

    public void setTarget(Node target) {
        this.target = target;
        if (target == null || !active) {
            clear();
            return;
        }
        setVisible(true);
        update();
    }

    public void clear() {
        target = null;
        dragMode = DragMode.NONE;
        overlayGroup.getTransforms().clear();
        setVisible(false);
    }

    public boolean isOverlayNode(Node node) {
        return node == overlayGroup || overlayGroup.getChildren().contains(node);
    }

    private void setVisible(boolean visible) {
        overlayGroup.setVisible(visible);
        overlayGroup.setManaged(visible);
    }

    private void registerHandlers() {
        rotateHandle.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> beginRotate(event));
        rotateHandle.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateRotate(event));
        rotateHandle.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> endDrag());

        outline.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> beginMove(event));
        outline.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateMove(event));
        outline.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> endDrag());

        DragMode[] modes = {
                DragMode.RESIZE_NW,
                DragMode.RESIZE_N,
                DragMode.RESIZE_NE,
                DragMode.RESIZE_E,
                DragMode.RESIZE_SE,
                DragMode.RESIZE_S,
                DragMode.RESIZE_SW,
                DragMode.RESIZE_W
        };

        for (int i = 0; i < handles.length; i++) {
            Rectangle handle = handles[i];
            DragMode mode = modes[i];
            handle.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> beginResize(event, mode));
            handle.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateResize(event));
            handle.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> endDrag());
        }
    }

    private void beginMove(MouseEvent event) {
        if (target == null) return;
        dragMode = DragMode.MOVE;
        dragStartParent = toParentPoint(event);
        startLayoutX = target.getLayoutX();
        startLayoutY = target.getLayoutY();
        startTranslateX = target.getTranslateX();
        startTranslateY = target.getTranslateY();
        event.consume();
    }

    private void updateMove(MouseEvent event) {
        if (target == null || dragMode != DragMode.MOVE) return;
        Point2D current = toParentPoint(event);
        Point2D delta = current.subtract(dragStartParent);
        target.setLayoutX(startLayoutX);
        target.setLayoutY(startLayoutY);
        target.setTranslateX(startTranslateX + delta.getX());
        target.setTranslateY(startTranslateY + delta.getY());
        update();
        event.consume();
    }

    private void beginResize(MouseEvent event, DragMode mode) {
        if (target == null) return;
        dragMode = mode;
        dragStartParent = toParentPoint(event);
        normalizeScaleForPointGeometry();
        dragStartBounds = target.getBoundsInParent();
        startScaleX = target.getScaleX();
        startScaleY = target.getScaleY();
        anchorX = dragStartBounds.getMinX() + dragStartBounds.getWidth() / 2.0;
        anchorY = dragStartBounds.getMinY() + dragStartBounds.getHeight() / 2.0;
        cachePointGeometry();
        event.consume();
    }

    private void updateResize(MouseEvent event) {
        if (target == null || !dragMode.name().startsWith("RESIZE")) return;
        Point2D current = toParentPoint(event);
        Point2D delta = current.subtract(dragStartParent);
        double width = Math.max(1.0, dragStartBounds.getWidth());
        double height = Math.max(1.0, dragStartBounds.getHeight());
        double scaleX = startScaleX;
        double scaleY = startScaleY;

        switch (dragMode) {
            case RESIZE_E -> scaleX = startScaleX * (width + delta.getX()) / width;
            case RESIZE_W -> scaleX = startScaleX * (width - delta.getX()) / width;
            case RESIZE_S -> scaleY = startScaleY * (height + delta.getY()) / height;
            case RESIZE_N -> scaleY = startScaleY * (height - delta.getY()) / height;
            case RESIZE_NE -> {
                scaleX = startScaleX * (width + delta.getX()) / width;
                scaleY = startScaleY * (height - delta.getY()) / height;
            }
            case RESIZE_NW -> {
                scaleX = startScaleX * (width - delta.getX()) / width;
                scaleY = startScaleY * (height - delta.getY()) / height;
            }
            case RESIZE_SE -> {
                scaleX = startScaleX * (width + delta.getX()) / width;
                scaleY = startScaleY * (height + delta.getY()) / height;
            }
            case RESIZE_SW -> {
                scaleX = startScaleX * (width - delta.getX()) / width;
                scaleY = startScaleY * (height + delta.getY()) / height;
            }
            default -> {
            }
        }

        if (target instanceof Line line) {
            applyScaledLine(line, scaleX, scaleY);
        } else if (target instanceof QuadCurve quadCurve) {
            applyScaledQuadCurve(quadCurve, scaleX, scaleY);
        } else {
            target.setScaleX(scaleX);
            target.setScaleY(scaleY);
        }
        update();
        event.consume();
    }

    private void beginRotate(MouseEvent event) {
        if (target == null) return;
        dragMode = DragMode.ROTATE;
        dragStartParent = toParentPoint(event);
        dragStartBounds = target.getBoundsInParent();
        startRotate = target.getRotate();
        anchorX = dragStartBounds.getMinX() + dragStartBounds.getWidth() / 2.0;
        anchorY = dragStartBounds.getMinY() + dragStartBounds.getHeight() / 2.0;
        event.consume();
    }

    private void updateRotate(MouseEvent event) {
        if (target == null || dragMode != DragMode.ROTATE) return;
        Point2D current = toParentPoint(event);
        double startAngle = Math.toDegrees(Math.atan2(
                dragStartParent.getY() - anchorY,
                dragStartParent.getX() - anchorX
        ));
        double currentAngle = Math.toDegrees(Math.atan2(
                current.getY() - anchorY,
                current.getX() - anchorX
        ));
        target.setRotate(startRotate + (currentAngle - startAngle));
        update();
        event.consume();
    }

    private void normalizeScaleForPointGeometry() {
        if (!(target instanceof Line || target instanceof QuadCurve)) {
            return;
        }
        if (target.getScaleX() == 1.0 && target.getScaleY() == 1.0) {
            return;
        }
        if (target instanceof Line line) {
            Point2D startParent = line.localToParent(line.getStartX(), line.getStartY());
            Point2D endParent = line.localToParent(line.getEndX(), line.getEndY());
            line.setScaleX(1.0);
            line.setScaleY(1.0);
            Point2D startLocal = line.parentToLocal(startParent);
            Point2D endLocal = line.parentToLocal(endParent);
            line.setStartX(startLocal.getX());
            line.setStartY(startLocal.getY());
            line.setEndX(endLocal.getX());
            line.setEndY(endLocal.getY());
        } else if (target instanceof QuadCurve quadCurve) {
            Point2D startParent = quadCurve.localToParent(quadCurve.getStartX(), quadCurve.getStartY());
            Point2D endParent = quadCurve.localToParent(quadCurve.getEndX(), quadCurve.getEndY());
            Point2D controlParent = quadCurve.localToParent(quadCurve.getControlX(), quadCurve.getControlY());
            quadCurve.setScaleX(1.0);
            quadCurve.setScaleY(1.0);
            Point2D startLocal = quadCurve.parentToLocal(startParent);
            Point2D endLocal = quadCurve.parentToLocal(endParent);
            Point2D controlLocal = quadCurve.parentToLocal(controlParent);
            quadCurve.setStartX(startLocal.getX());
            quadCurve.setStartY(startLocal.getY());
            quadCurve.setEndX(endLocal.getX());
            quadCurve.setEndY(endLocal.getY());
            quadCurve.setControlX(controlLocal.getX());
            quadCurve.setControlY(controlLocal.getY());
        }
    }

    private void cachePointGeometry() {
        lineStartParent = null;
        lineEndParent = null;
        quadStartParent = null;
        quadEndParent = null;
        quadControlParent = null;
        if (target instanceof Line line) {
            lineStartParent = line.localToParent(line.getStartX(), line.getStartY());
            lineEndParent = line.localToParent(line.getEndX(), line.getEndY());
        } else if (target instanceof QuadCurve quadCurve) {
            quadStartParent = quadCurve.localToParent(quadCurve.getStartX(), quadCurve.getStartY());
            quadEndParent = quadCurve.localToParent(quadCurve.getEndX(), quadCurve.getEndY());
            quadControlParent = quadCurve.localToParent(quadCurve.getControlX(), quadCurve.getControlY());
        }
    }

    private void applyScaledLine(Line line, double scaleX, double scaleY) {
        if (lineStartParent == null || lineEndParent == null) {
            return;
        }
        Point2D startParent = scalePoint(lineStartParent, scaleX, scaleY);
        Point2D endParent = scalePoint(lineEndParent, scaleX, scaleY);
        Point2D startLocal = line.parentToLocal(startParent);
        Point2D endLocal = line.parentToLocal(endParent);
        line.setStartX(startLocal.getX());
        line.setStartY(startLocal.getY());
        line.setEndX(endLocal.getX());
        line.setEndY(endLocal.getY());
    }

    private void applyScaledQuadCurve(QuadCurve quadCurve, double scaleX, double scaleY) {
        if (quadStartParent == null || quadEndParent == null || quadControlParent == null) {
            return;
        }
        Point2D startParent = scalePoint(quadStartParent, scaleX, scaleY);
        Point2D endParent = scalePoint(quadEndParent, scaleX, scaleY);
        Point2D controlParent = scalePoint(quadControlParent, scaleX, scaleY);
        Point2D startLocal = quadCurve.parentToLocal(startParent);
        Point2D endLocal = quadCurve.parentToLocal(endParent);
        Point2D controlLocal = quadCurve.parentToLocal(controlParent);
        quadCurve.setStartX(startLocal.getX());
        quadCurve.setStartY(startLocal.getY());
        quadCurve.setEndX(endLocal.getX());
        quadCurve.setEndY(endLocal.getY());
        quadCurve.setControlX(controlLocal.getX());
        quadCurve.setControlY(controlLocal.getY());
    }

    private Point2D scalePoint(Point2D point, double scaleX, double scaleY) {
        double x = anchorX + (point.getX() - anchorX) * scaleX;
        double y = anchorY + (point.getY() - anchorY) * scaleY;
        return new Point2D(x, y);
    }

    private Point2D toParentPoint(MouseEvent event) {
        if (host == null) {
            return new Point2D(event.getSceneX(), event.getSceneY());
        }
        return host.sceneToLocal(event.getSceneX(), event.getSceneY());
    }

    private void endDrag() {
        dragMode = DragMode.NONE;
    }

    public void update() {
        if (target == null || host == null) return;
        overlayGroup.toFront();
        Bounds localBounds = target.getBoundsInLocal();
        Affine transform = new Affine(target.getLocalToParentTransform());
        double scaleX = Math.hypot(transform.getMxx(), transform.getMyx());
        double scaleY = Math.hypot(transform.getMxy(), transform.getMyy());
        double rotation = Math.toDegrees(Math.atan2(transform.getMyx(), transform.getMxx()));

        Point2D center = target.localToParent(
                localBounds.getMinX() + localBounds.getWidth() / 2.0,
                localBounds.getMinY() + localBounds.getHeight() / 2.0
        );
        double width = localBounds.getWidth() * scaleX;
        double height = localBounds.getHeight() * scaleY;

        overlayGroup.getTransforms().setAll(new javafx.scene.transform.Rotate(rotation, center.getX(), center.getY()));
        outline.setStrokeWidth(1.5);
        outline.setX(center.getX() - width / 2.0);
        outline.setY(center.getY() - height / 2.0);
        outline.setWidth(width);
        outline.setHeight(height);

        double left = outline.getX() - HANDLE_SIZE / 2.0;
        double right = outline.getX() + outline.getWidth() - HANDLE_SIZE / 2.0;
        double top = outline.getY() - HANDLE_SIZE / 2.0;
        double bottom = outline.getY() + outline.getHeight() - HANDLE_SIZE / 2.0;
        double centerX = outline.getX() + outline.getWidth() / 2.0 - HANDLE_SIZE / 2.0;
        double centerY = outline.getY() + outline.getHeight() / 2.0 - HANDLE_SIZE / 2.0;

        handles[0].setX(left);
        handles[0].setY(top);
        handles[1].setX(centerX);
        handles[1].setY(top);
        handles[2].setX(right);
        handles[2].setY(top);
        handles[3].setX(right);
        handles[3].setY(centerY);
        handles[4].setX(right);
        handles[4].setY(bottom);
        handles[5].setX(centerX);
        handles[5].setY(bottom);
        handles[6].setX(left);
        handles[6].setY(bottom);
        handles[7].setX(left);
        handles[7].setY(centerY);

        rotateHandle.setCenterX(outline.getX() + outline.getWidth() / 2.0);
        rotateHandle.setCenterY(outline.getY() - ROTATION_HANDLE_OFFSET);
    }
}
