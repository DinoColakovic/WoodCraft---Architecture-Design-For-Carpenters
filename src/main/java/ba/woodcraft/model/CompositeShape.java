package ba.woodcraft.model;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public class CompositeShape implements Drawable {

    private final List<Drawable> shapes = new ArrayList<>();
    private final Group group = new Group();

    public CompositeShape(List<Drawable> shapes) {
        if (shapes != null) {
            shapes.forEach(this::add);
        }
    }

    public void add(Drawable shape) {
        shapes.add(shape);
        group.getChildren().add(shape.getNode());
    }

    @Override
    public Node getNode() {
        return group;
    }

    @Override
    public void update(double x, double y) {
        // Composite is static; individual shapes handle updates
    }
}
