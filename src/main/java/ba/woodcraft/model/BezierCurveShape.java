package ba.woodcraft.model;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

public class BezierCurveShape implements Drawable {
    private final double startX;
    private final double startY;
    private final CubicCurve curve;

    public BezierCurveShape(double startX, double startY) {
        this.startX = startX;
        this.startY = startY;
        this.curve = new CubicCurve();
        this.curve.setStartX(startX);
        this.curve.setStartY(startY);
        this.curve.setEndX(startX);
        this.curve.setEndY(startY);
        this.curve.setStroke(Color.DARKSLATEBLUE);
        this.curve.setStrokeWidth(2);
        this.curve.setFill(Color.TRANSPARENT);
    }

    @Override
    public Node getNode() {
        return curve;
    }

    @Override
    public void update(double x, double y) {
        curve.setStartX(startX);
        curve.setStartY(startY);
        curve.setEndX(x);
        curve.setEndY(y);

        double dx = x - startX;
        double dy = y - startY;
        curve.setControlX1(startX + dx * 0.33);
        curve.setControlY1(startY + dy * 0.1);
        curve.setControlX2(startX + dx * 0.66);
        curve.setControlY2(startY + dy * 0.9);
    }
}
