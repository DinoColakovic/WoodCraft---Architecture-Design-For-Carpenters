package ba.woodcraft.model;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class FreehandShape implements Drawable {

    private final Path path = new Path();

    public FreehandShape(double x, double y) {
        path.getElements().add(new MoveTo(x, y));
        path.setStroke(Color.BLACK);
        path.setStrokeWidth(2.0);
        path.setFill(null);
        path.setSmooth(true);
    }

    @Override
    public void update(double x, double y) {
        path.getElements().add(new LineTo(x, y));
    }

    @Override
    public Node getNode() {
        return path;
    }
}
