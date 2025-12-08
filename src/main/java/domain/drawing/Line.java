package domain.drawing;



public record Line(Point start, Point end) {

    public double distance(){
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        return Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2));
    }

}
