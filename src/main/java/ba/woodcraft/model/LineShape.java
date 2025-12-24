package ba.woodcraft.model;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class LineShape implements Drawable {
    private final double startX;
    private final double startY;
    private final Line line;

    public LineShape(double startX, double startY) {
        this.startX = startX;
        this.startY = startY;
        this.line = new Line(startX, startY, startX, startY);
        this.line.setStroke(Color.DIMGRAY);
        this.line.setStrokeWidth(2);
    }

    @Override
    public Node getNode() {
        return line;
    }

    @Override
    public void update(double x, double y) {
        line.setStartX(startX);
        line.setStartY(startY);
        line.setEndX(x);
        line.setEndY(y);
    }
}
