package domain.drawing;

import java.util.List;
import java.util.UUID;

public class Drawing {
    private final UUID id;
    private final List<Shape> shapes;
    private final String name; // ime tog zamišljenog namještaja

    public Drawing(UUID id, List<Shape> shapes, String name) {
        this.id = id;
        this.shapes = List.copyOf(shapes);
        this.name = name;
    }

    public UUID id() { return id; }
    public List<Shape> shapes() { return shapes; }
    public String name() { return name; }

    // Domain behavior
    public double totalLength() {
        return shapes.stream()
                .mapToDouble(Shape::totalLength)
                .sum();
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
    }

    public void removeShape(Shape shape) {
        shapes.remove(shape);
    }
}
