package domain.drawing;

import java.util.List;

public record BezierCurve(List<Point> controlPoints) implements Segment {

    // Paskalov trokut, peti stepen
    private static final int[][] PASCAL = {
            {1},                // n=0
            {1, 1},             // n=1
            {1, 2, 1},          // n=2
            {1, 3, 3, 1},       // n=3
            {1, 4, 6, 4, 1},    // n=4
            {1, 5, 10, 10, 5, 1} // n=5
    };

    public Point evaluate(double t) {
        int n = controlPoints.size() - 1;
        double x = 0.0;
        double y = 0.0;

        for (int i = 0; i <= n; i++) {
            int binomial = PASCAL[n][i]; // lookup instead of computing
            double basis = binomial * Math.pow(1 - t, n - i) * Math.pow(t, i);
            x += basis * controlPoints.get(i).x();
            y += basis * controlPoints.get(i).y();
        }

        return new Point(x, y);
    }
    @Override
    public double length() {
        int samples = 50;
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
