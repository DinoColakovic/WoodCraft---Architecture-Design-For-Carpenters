package domain.drawing;

public record BezierCurve(Point p0, Point p1, Point p2, Point p3) {


    public Point evaluate(double t) {
        double x = Math.pow(1 - t, 3) * p0.x()
                + 3 * Math.pow(1 - t, 2) * t * p1.x()
                + 3 * (1 - t) * Math.pow(t, 2) * p2.x()
                + Math.pow(t, 3) * p3.x();

        double y = Math.pow(1 - t, 3) * p0.y()
                + 3 * Math.pow(1 - t, 2) * t * p1.y()
                + 3 * (1 - t) * Math.pow(t, 2) * p2.y()
                + Math.pow(t, 3) * p3.y();

        return new Point(x, y);
    }


    public double approximateLength(int samples) {
        double length = 0.0;
        Point prev = evaluate(0.0);
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            Point curr = evaluate(t);
            length += Math.sqrt(Math.pow(curr.x() - prev.x(), 2) +
                    Math.pow(curr.y() - prev.y(), 2));
            prev = curr;
        }
        return length;
    }
}
