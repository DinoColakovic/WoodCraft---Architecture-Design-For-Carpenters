package ba.woodcraft.model;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RectangleShape implements Drawable {
    private final double startX;
    private final double startY;
    private final Rectangle rectangle;

    public RectangleShape(double startX, double startY) {
        this.startX = startX;
        this.startY = startY;
        this.rectangle = new Rectangle(startX, startY, 0, 0);
        this.rectangle.setFill(Color.TRANSPARENT);
        this.rectangle.setStroke(Color.FORESTGREEN);
        this.rectangle.setStrokeWidth(2);
    }

    @Override
    public Node getNode() {
        return rectangle;
    }

    @Override
    public void update(double x, double y) {
        double minX = Math.min(startX, x);
        double minY = Math.min(startY, y);
        double width = Math.abs(x - startX);
        double height = Math.abs(y - startY);
        rectangle.setX(minX);
        rectangle.setY(minY);
        rectangle.setWidth(width);
        rectangle.setHeight(height);
    }
}
