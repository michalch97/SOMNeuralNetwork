public class QuantizationTypeManager {
    public enum QuantizationType {
        KohonenAlgorithm, NeuralGasAlgorithm, KMeansAlgorithm, ImageCompression
    }
    public static String toString(QuantizationType type) {
        switch (type) {
            case KohonenAlgorithm:
                return "Algorytm Kohonena";
            case NeuralGasAlgorithm:
                return "Algorytm gazu neuronowego";
            case KMeansAlgorithm:
                return "Algorytm k-średnich";
            case ImageCompression:
                return "Kompresja obrazu";
            default:
                return "";
        }
    }
}
