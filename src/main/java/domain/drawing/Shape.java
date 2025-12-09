package domain.drawing;

import java.util.UUID;

public class Shape {
    private final UUID id;
    private final java.util.List<Segment> segments;

    public Shape(UUID id, java.util.List<Segment> segments) {
        this.id = id;
        this.segments = java.util.List.copyOf(segments);
    }

    public UUID id() { return id; }
    public java.util.List<Segment> segments() { return segments; }

    public double totalLength() {
        return segments.stream().mapToDouble(Segment::length).sum();
    }
}

