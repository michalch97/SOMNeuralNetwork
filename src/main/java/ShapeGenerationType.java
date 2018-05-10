

public enum ShapeGenerationType {
    Rectangle, Ellipse, Polygon;
    
    public static String toString(ShapeGenerationType type) {
        switch (type) {
            case Rectangle:
                return "Prostokąt";
            case Ellipse:
                return "Elipsa";
            case Polygon:
                return "Wielokąt";
            default:
                return "";
        }
    }
}
