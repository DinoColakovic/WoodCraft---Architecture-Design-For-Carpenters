package domain.drawing;

import java.util.List;

public record BezierCurve(List<Point> controlPoints) {

    public Point evaluate(double t) {
        int n = controlPoints.size() - 1;
        double x = 0.0;
        double y = 0.0;

        for (int i = 0; i <= n; i++) {
            double binomial = binomialCoefficient(n, i);
            double basis = binomial * Math.pow(1 - t, n - i) * Math.pow(t, i);
            x += basis * controlPoints.get(i).x();
            y += basis * controlPoints.get(i).y();
        }

        return new Point(x, y);
    }

    private long binomialCoefficient(int n, int k) {
        long res = 1;
        for (int i = 0; i < k; i++) {
            res = res * (n - i) / (i + 1);
        }
        return res;
    }
}