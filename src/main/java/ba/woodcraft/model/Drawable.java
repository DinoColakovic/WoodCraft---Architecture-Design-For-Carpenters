package ba.woodcraft.model;

import javafx.scene.Node;

public interface Drawable {
    Node getNode();
    void update(double x, double y);
}
