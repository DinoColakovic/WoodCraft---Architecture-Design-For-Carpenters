package ba.woodcraft.ui.controller;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
        dragStartBounds = target.getBoundsInParent();
        startScaleX = target.getScaleX();
        startScaleY = target.getScaleY();
        anchorX = dragStartBounds.getMinX() + dragStartBounds.getWidth() / 2.0;
        anchorY = dragStartBounds.getMinY() + dragStartBounds.getHeight() / 2.0;
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

        target.setScaleX(scaleX);
        target.setScaleY(scaleY);
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
        Bounds bounds = target.getBoundsInLocal();
        overlayGroup.getTransforms().setAll(new Affine(target.getLocalToParentTransform()));
        outline.setX(bounds.getMinX());
        outline.setY(bounds.getMinY());
        outline.setWidth(bounds.getWidth());
        outline.setHeight(bounds.getHeight());

        double left = bounds.getMinX() - HANDLE_SIZE / 2.0;
        double right = bounds.getMaxX() - HANDLE_SIZE / 2.0;
        double top = bounds.getMinY() - HANDLE_SIZE / 2.0;
        double bottom = bounds.getMaxY() - HANDLE_SIZE / 2.0;
        double centerX = bounds.getMinX() + bounds.getWidth() / 2.0 - HANDLE_SIZE / 2.0;
        double centerY = bounds.getMinY() + bounds.getHeight() / 2.0 - HANDLE_SIZE / 2.0;

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

        rotateHandle.setCenterX(bounds.getMinX() + bounds.getWidth() / 2.0);
        rotateHandle.setCenterY(bounds.getMinY() - ROTATION_HANDLE_OFFSET);
    }
}
