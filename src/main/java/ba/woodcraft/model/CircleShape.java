package ba.woodcraft.model;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class CircleShape implements Drawable {
    private final double startX;
    private final double startY;
    private final Circle circle;

    public CircleShape(double startX, double startY) {
        this.startX = startX;
        this.startY = startY;

        this.circle = new Circle(startX, startY, 0);
        this.circle.setFill(Color.TRANSPARENT);
        this.circle.setStroke(Color.FORESTGREEN);
        this.circle.setStrokeWidth(2);
    }

    @Override
    public Node getNode() {
        return circle;
    }

    @Override
    public void update(double x, double y) {
        double dx = x - startX;
        double dy = y - startY;

        // Keep it a true circle: radius based on the larger drag distance
        double radius = Math.max(Math.abs(dx), Math.abs(dy)) / 2.0;

        // Center the circle between start point and current point
        double centerX = (startX + x) / 2.0;
        double centerY = (startY + y) / 2.0;

        circle.setCenterX(centerX);
        circle.setCenterY(centerY);
        circle.setRadius(radius);
    }
}
