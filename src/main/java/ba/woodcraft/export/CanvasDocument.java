package ba.woodcraft.export;

import ba.woodcraft.ui.controller.SelectionOverlay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class CanvasDocument {

    private final Pane drawingPane;
    private final Node snapIndicator;
    private final SelectionOverlay selectionOverlay;

    public CanvasDocument(Pane drawingPane, Node snapIndicator, SelectionOverlay selectionOverlay) {
        this.drawingPane = Objects.requireNonNull(drawingPane, "drawingPane");
        this.snapIndicator = Objects.requireNonNull(snapIndicator, "snapIndicator");
        this.selectionOverlay = selectionOverlay;
    }

    public double getWidth() {
        return drawingPane.getPrefWidth();
    }

    public double getHeight() {
        return drawingPane.getPrefHeight();
    }

    public List<Node> getExportableNodes() {
        List<Node> nodes = new ArrayList<>();
        for (Node node : drawingPane.getChildren()) {
            if (!node.isVisible()) {
                continue;
            }
            if (node == snapIndicator) {
                continue;
            }
            if (selectionOverlay != null && selectionOverlay.isOverlayNode(node)) {
                continue;
            }
            nodes.add(node);
        }
        return nodes;
    }
}
