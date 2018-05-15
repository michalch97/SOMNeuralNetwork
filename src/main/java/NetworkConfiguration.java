import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static java.lang.Math.exp;
import static java.lang.Math.pow;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class NetworkConfiguration extends SwingWorker<Void, RealMatrix> {

    private String filePath;
    private int groups;
    private int epochLimit;
    private double startRadius;
    private double startLearningRate;
    private QuantizationTypeManager.QuantizationType quantizationType;
    private double currentRadius;
    private double currentLearningRate;
    private double lambda;
    private List<RealVector> inputs = new ArrayList<>();
    private RealMatrix weights;
    private PlotFrame frame;
    private PlotFrame endFrame;

    private ArrayList<ArrayList<RealVector>> imageInputs;
    private int pixelSquareSide = 3;
    private int originalImageHeight;
    private int originalImageWidth;
    private int originalImageType;
    private String imageOutputPath = "quantizedImage.png";

    private final int pointsCount = 1000;
    private final Rectangle shapeBounds = new Rectangle(-5, -5, 10, 10);
    private ShapeGenerationType shapeType;

    public NetworkConfiguration(String filePath, int groups, int epoch, double startRadius, double startLearningRate, QuantizationTypeManager.QuantizationType quantizationType, ShapeGenerationType shapeType) {
        this.filePath = filePath;
        this.groups = groups;
        this.epochLimit = epoch;
        this.quantizationType = quantizationType;
        this.shapeType = shapeType;
        this.startRadius = startRadius;
        this.currentRadius = startRadius;
        this.startLearningRate = startLearningRate;
        this.currentLearningRate = startLearningRate;
        this.lambda = epochLimit / startRadius;
    }

    public void start() {
        loadData();
        execute();
    }

    public void cancelLearning() {
        cancel(true);
        frame.dispose();

        if (endFrame != null) {
            endFrame.dispose();
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        switch (quantizationType) {
            case KMeansAlgorithm:
                kMeans();
                break;
            case ImageCompression:
                imagecCmpression();
                break;
            case KohonenAlgorithm:
                kohonenAlgorithm();
                break;
            case NeuralGasAlgorithm:
                neuralGasAlgorithm();
                break;
        }
        return null;
    }

    @Override
    protected void process(List<RealMatrix> listOfWeights) {
        for (RealMatrix weights : listOfWeights) {
            frame.addWeights(weights);
        }
    }

    @Override
    protected void done() {
        System.out.println(calculateError());

        if (quantizationType != QuantizationTypeManager.QuantizationType.ImageCompression) {
            plotByGroups();
        }
    }

    private double calculateError() {
        double sumError = 0;

        for (RealVector input : inputs) {
            double minDistance = weights.getRowVector(0).getDistance(input);
            for (int i = 1; i < weights.getRowDimension(); i++) {
                RealVector weight = weights.getRowVector(i);
                if (weight.getDistance(input) < minDistance) {
                    minDistance = weight.getDistance(input);
                }
            }
            sumError += minDistance;
        }

        if (inputs.size() > 0) {
            return sumError / inputs.size();
        } else {
            return 0;
        }
    }

    private void plotByGroups() {
        ArrayList<ArrayList<RealVector>> groupedPoints = new ArrayList<ArrayList<RealVector>>();
        for (int i = 0; i < weights.getRowDimension(); i++) {
            groupedPoints.add(new ArrayList<>());
        }

        for (RealVector input : inputs) {
            int closestNeuronIndex = 0;
            double minDistance = weights.getRowVector(0).getDistance(input);
            for (int i = 1; i < groupedPoints.size(); i++) {
                RealVector weight = weights.getRowVector(i);
                if (weight.getDistance(input) < minDistance) {
                    minDistance = weight.getDistance(input);
                    closestNeuronIndex = i;
                }
            }
            groupedPoints.get(closestNeuronIndex).add(input);
        }

        endFrame = new PlotFrame("Grupy punktów");
        endFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                cancelLearning();
            }
        });

        ArrayList<RealVector> neuronPositions = new ArrayList<RealVector>();
        for (int i = 0; i < weights.getRowDimension(); i++) {
            neuronPositions.add(weights.getRowVector(i));
        }
        endFrame.addNewColoredSeries(neuronPositions, Color.BLACK);

        Random rand = new Random();
        for (ArrayList<RealVector> groupOfPoints : groupedPoints) {
            float r = rand.nextFloat() / 1.2f + 0.1f;
            float g = rand.nextFloat() / 1.2f + 0.1f;
            float b = rand.nextFloat() / 1.2f + 0.1f;

            endFrame.addNewColoredSeries(groupOfPoints, new Color(r, g, b));
        }
    }

    private void loadData() {
        switch (quantizationType) {
            case KMeansAlgorithm:
            case KohonenAlgorithm:
            case NeuralGasAlgorithm:
                if (!filePath.isEmpty() && filePath.contains("txt")) {
                    try {
                        FileReader fileReader = new FileReader(filePath);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        String lineWithData;
                        String[] splitLineWithData;
                        RealVector input;
                        double[] xAndy = new double[2];
                        while ((lineWithData = bufferedReader.readLine()) != null) {
                            splitLineWithData = lineWithData.split(",");
                            xAndy[0] = Double.parseDouble(splitLineWithData[0]);
                            xAndy[1] = Double.parseDouble(splitLineWithData[1]);
                            input = new ArrayRealVector(xAndy);
                            inputs.add(input);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Shape generatedShape = null;

                    switch (shapeType) {
                        case Rectangle:
                        case Ellipse:
                            Random random = new Random();
                            Point2D.Double pointLeftTop = generatePointInArea(shapeBounds.getMinX(), shapeBounds.getCenterX(),
                                    shapeBounds.getMinY(), shapeBounds.getCenterY());
                            Point2D.Double pointRightBot = generatePointInArea(shapeBounds.getCenterX(), shapeBounds.getMaxX(),
                                    shapeBounds.getCenterY(), shapeBounds.getMaxY());

                            if (shapeType == ShapeGenerationType.Rectangle) {
                                generatedShape = new Rectangle2D.Double(pointLeftTop.x, pointLeftTop.y, pointRightBot.x - pointLeftTop.x, pointRightBot.y - pointLeftTop.y);
                            } else if (shapeType == ShapeGenerationType.Ellipse) {
                                generatedShape = new Ellipse2D.Double(pointLeftTop.x, pointLeftTop.y, pointRightBot.x - pointLeftTop.x, pointRightBot.y - pointLeftTop.y);
                            }
                            break;
                        case Polygon:
                            ArrayList<Point2D.Double> pointList = new ArrayList<Point2D.Double>();

                            double minY = shapeBounds.getMinY();
                            double cenY = shapeBounds.getCenterY();
                            double maxY = shapeBounds.getMaxY();

                            pointList.add(generatePointInArea(shapeBounds.getMinX(), shapeBounds.getCenterX(), // leftTop
                                    shapeBounds.getCenterY(), shapeBounds.getMaxY()));
                            pointList.add(generatePointInArea(shapeBounds.getCenterX() - (shapeBounds.getCenterX() - shapeBounds.getMinX()) * 0.4, // midTop
                                    shapeBounds.getCenterX() + (shapeBounds.getMaxX() - shapeBounds.getCenterX()) * 0.4,
                                    shapeBounds.getCenterY(),
                                    shapeBounds.getMaxY() - (shapeBounds.getMaxY() - shapeBounds.getCenterY()) * 0.4));
                            pointList.add(generatePointInArea(shapeBounds.getCenterX(), shapeBounds.getMaxX(), // rightTop
                                    shapeBounds.getMaxY(), shapeBounds.getCenterY()));
                            pointList.add(generatePointInArea(shapeBounds.getCenterX(), shapeBounds.getMaxX(), // rightBot
                                    shapeBounds.getMinY(), shapeBounds.getCenterY()));
                            pointList.add(generatePointInArea(shapeBounds.getCenterX() - (shapeBounds.getCenterX() - shapeBounds.getMinX()) * 0.4, // midBot
                                    shapeBounds.getCenterX() + (shapeBounds.getMaxX() - shapeBounds.getCenterX()) * 0.4,
                                    shapeBounds.getMinY(),
                                    shapeBounds.getCenterY()));
                            pointList.add(generatePointInArea(shapeBounds.getMinX(), shapeBounds.getCenterX(), // leftBot
                                    shapeBounds.getMinY(), shapeBounds.getCenterY()));

                            Path2D path = new Path2D.Double();
                            path.moveTo(pointList.get(0).getX(), pointList.get(0).getY());
                            for (int i = 1; i < 6; i++) {
                                path.lineTo(pointList.get(i).getX(), pointList.get(i).getY());
                            }
                            path.closePath();

                            generatedShape = path;

                            break;
                        default:
                            break;
                    }

                    if (generatedShape != null) {
                        double minX = generatedShape.getBounds2D().getMinX();
                        double maxX = generatedShape.getBounds2D().getMaxX();
                        double minY = generatedShape.getBounds2D().getMinY();
                        double maxY = generatedShape.getBounds2D().getMaxY();

                        for (int i = 0; i < pointsCount; i++) {
                            RealVector vector = new ArrayRealVector(2);
                            Point2D.Double generatedPoint = generatePointInArea(minX, maxX, minY, maxY);

                            if (generatedShape.contains(generatedPoint)) {
                                vector.setEntry(0, generatedPoint.getX());
                                vector.setEntry(1, generatedPoint.getY());
                                inputs.add(vector);
                            } else {
                                continue;
                            }
                        }
                    }
                }
                break;
            case ImageCompression:
                if (!filePath.isEmpty() && (filePath.contains("png") || filePath.contains("bmp") || filePath.contains("jpg"))) {
                    try {
                        File imgFile = new File(filePath);
                        BufferedImage image = ImageIO.read(imgFile);
                        ImageRgbExtractor extractor = new ImageRgbExtractor(image);
                        imageInputs = new ArrayList<ArrayList<RealVector>>();

                        originalImageHeight = image.getHeight();
                        originalImageWidth = image.getWidth();
                        originalImageType = image.getType();

                        for (int height = 0; height + pixelSquareSide < image.getHeight(); height += pixelSquareSide) {
                            for (int width = 0; width + pixelSquareSide < image.getWidth(); width += pixelSquareSide) {
                                imageInputs.add(new ArrayList<RealVector>());

                                for (int squareY = 0; squareY < pixelSquareSide; squareY++) {
                                    for (int squareX = 0; squareX < pixelSquareSide; squareX++) {
                                        int x = squareX + width;
                                        int y = squareY + height;
                                        imageInputs.get(imageInputs.size() - 1).add(extractor.getRGBVector(squareX + width, squareY + height));
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

    }

    private Point2D.Double generatePointInArea(double minX, double maxX, double minY, double maxY) {
        Random random = new Random();
        double x = random.nextDouble() * (maxX - minX) + minX;
        double y = random.nextDouble() * (maxY - minY) + minY;

        Point2D.Double generatedPoint = new Point2D.Double(x, y);
        return generatedPoint;
    }

    private void imagecCmpression() {
        if (imageInputs.size() == 0) {
            return;
        }
        if (imageInputs.get(0).size() == 0) {
            return;
        }

        double[][] arrayOfWeights = new double[groups * groups][imageInputs.get(0).get(0).getDimension()];
        Random random = new Random();
        for (int i = 0; i < groups * groups; i++) {
            for (int j = 0; j < arrayOfWeights[0].length; j++) {
                arrayOfWeights[i][j] = random.nextDouble();
            }
        }

        weights = new Array2DRowRealMatrix(arrayOfWeights);
        for (int currentEpoch = 0; (currentEpoch < epochLimit) && (!isCancelled()); currentEpoch++) {
            System.out.println(currentEpoch);

            if (currentEpoch != 0) {
                currentLearningRate = calculateCurrentLearningRate(currentEpoch);
                currentRadius = calculateCurrentRadius(currentEpoch);
            }

            for (ArrayList<RealVector> frameOfPixels : imageInputs) {
                double minDistance = getDistanceFromColorFrame(weights.getRowVector(0), frameOfPixels);
                int index = 0;
                for (int i = 1; i < groups * groups; i++) {
                    double currentDistance = getDistanceFromColorFrame(weights.getRowVector(i), frameOfPixels);
                    if (currentDistance < minDistance) {
                        minDistance = currentDistance;
                        index = i;
                    }
                }

                int BMUx = (index % groups);
                int BMUy = (index / groups);
                for (int i = 0; i < groups * groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    double distanceBetweenBMUAndNeighbourhood = weight.getDistance(weights.getRowVector(index));

                    int neuronX = (i % groups);
                    int neuronY = (i / groups);
                    double neighbourDistance = (BMUy - neuronY) * (BMUy - neuronY) + (BMUx - neuronX) * (BMUx - neuronX);
                    neighbourDistance = Math.sqrt(neighbourDistance);

                    if (neighbourDistance < currentRadius) {
                        weights.setRowVector(i, calculateUpdateForPixels(frameOfPixels, weight, distanceBetweenBMUAndNeighbourhood));
                    }
                }
            }
        }

        ArrayList<Color> framesColors = assignFramesToGroups(imageInputs, weights);
        //ArrayList<Color> framesColors = assignFramesToGroupsMockup(imageInputs, weights);
        saveQuantizedImage(framesColors);
    }

    private ArrayList<Color> assignFramesToGroupsMockup(ArrayList<ArrayList<RealVector>> imageFrames, RealMatrix neurons) {
        ArrayList<Color> framesColors = new ArrayList<Color>();

        for (ArrayList<RealVector> frameOfPixels : imageInputs) {
            Color frameColor = getColorFromVector(frameOfPixels.get(0));
            framesColors.add(frameColor);
        }

        return framesColors;
    }

    private ArrayList<Color> assignFramesToGroups(ArrayList<ArrayList<RealVector>> imageFrames, RealMatrix neurons) {
        ArrayList<Color> framesColors = new ArrayList<Color>();

        for (ArrayList<RealVector> frameOfPixels : imageInputs) {
            double minDistance = getDistanceFromColorFrame(neurons.getRowVector(0), frameOfPixels);
            int index = 0;
            for (int i = 1; i < groups * groups; i++) {
                double currentDistance = getDistanceFromColorFrame(neurons.getRowVector(i), frameOfPixels);
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    index = i;
                }
            }

            Color frameColor = getColorFromVector(neurons.getRowVector(index));
            framesColors.add(frameColor);
        }

        return framesColors;
    }

    private Color getColorFromVector(RealVector colorVector) {
        int position = -1;

        int alpha = 255;
        if (colorVector.getDimension() > 3 && colorVector.getEntry(++position) >= 0) {
            alpha = (int) colorVector.getEntry(position);
        }

        int blue = 0;
        if (colorVector.getEntry(++position) >= 0) {
            blue = (int) colorVector.getEntry(position);
        }

        int green = 0;
        if (colorVector.getEntry(++position) >= 0) {
            green = (int) colorVector.getEntry(position);
        }

        int red = 0;
        if (colorVector.getEntry(++position) >= 0) {
            red = (int) colorVector.getEntry(position);
        }

        return new Color(red, green, blue, alpha);
    }

    private void saveQuantizedImage(ArrayList<Color> frameColors) {
        BufferedImage outputImageBuffer = new BufferedImage((originalImageWidth / pixelSquareSide) * pixelSquareSide, (originalImageHeight / pixelSquareSide) * pixelSquareSide, originalImageType);

        for (int height = 0; height + pixelSquareSide < originalImageHeight; height += pixelSquareSide) {
            for (int width = 0; width + pixelSquareSide < originalImageWidth; width += pixelSquareSide) {
                int heightOffset = 0;
                if (originalImageWidth % pixelSquareSide != 0) {
                    heightOffset = (height / pixelSquareSide) * (originalImageWidth / pixelSquareSide);
                } else {
                    heightOffset = (height / pixelSquareSide) * (originalImageWidth / pixelSquareSide - 1);
                }
                int widthOffset = width / pixelSquareSide;

                Color frameColor = frameColors.get(heightOffset + widthOffset);

                for (int squareY = 0; squareY < pixelSquareSide; squareY++) {
                    for (int squareX = 0; squareX < pixelSquareSide; squareX++) {
                        int pixel = (frameColor.getAlpha() << 24) | (frameColor.getRed() << 16) | (frameColor.getGreen() << 8) | frameColor.getBlue();
                        int x = squareX + width;
                        int y = squareY + height;
                        outputImageBuffer.setRGB(squareX + width, squareY + height, pixel);
                    }
                }
            }
        }

        File outputFile = new File(imageOutputPath);
        try {
            ImageIO.write(outputImageBuffer, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getDistanceFromColorFrame(RealVector weight, ArrayList<RealVector> frameOfPixels) {
        double distance = 0;

        for (RealVector pixel : frameOfPixels) {
            distance += weight.getDistance(pixel);
        }

        return distance;
    }

    private void kMeans() {
        plotPoints();
        double[][] arrayOfWeights = new double[groups][2];

        Random random = new Random();
        for (int i = 0; i < groups; i++) {
            arrayOfWeights[i][0] = random.nextDouble() - 0.5;
            arrayOfWeights[i][1] = random.nextDouble() - 0.5;
        }

        weights = new Array2DRowRealMatrix(arrayOfWeights);
        for (int currentEpoch = 0; (currentEpoch < epochLimit) && (!isCancelled()); currentEpoch++) {
            System.out.println(currentEpoch);

            this.publish(weights);
            if (currentEpoch != 0) {
                currentLearningRate = calculateCurrentLearningRate(currentEpoch);
                currentRadius = calculateCurrentRadius(currentEpoch);
                Collections.shuffle(inputs);
            }

            List<Pair<Integer, RealVector>> groupAndInput = new ArrayList<>();
            for (int j = 0; j < inputs.size(); j++) {
                double minDistance = weights.getRowVector(0).getDistance(inputs.get(j));
                int index = 0;
                for (int i = 1; i < groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    if (weight.getDistance(inputs.get(j)) < minDistance) {
                        minDistance = weight.getDistance(inputs.get(j));
                        index = i;
                    }
                }
                groupAndInput.add(new Pair<Integer, RealVector>(index, inputs.get(j)));
            }

            RealVector[] sumOfInputsByGroup = new RealVector[groups];
            for (int i = 0; i < groups; i++) {
                sumOfInputsByGroup[i] = new ArrayRealVector(weights.getColumnDimension());
            }

            int[] inputCountByGroup = new int[groups];
            for (Pair<Integer, RealVector> pair : groupAndInput) {
                sumOfInputsByGroup[pair.getFirst()] = sumOfInputsByGroup[pair.getFirst()].add(pair.getSecond());
                inputCountByGroup[pair.getFirst()]++;
            }

            RealVector[] meanOfGroup = new RealVector[sumOfInputsByGroup.length];
            for (int i = 0; i < groups; i++) {
                meanOfGroup[i] = new ArrayRealVector(weights.getColumnDimension());
            }
            for (int groupId = 0; groupId < groups; groupId++) {
                if (inputCountByGroup[groupId] != 0) {
                    meanOfGroup[groupId] = sumOfInputsByGroup[groupId].mapDivide(inputCountByGroup[groupId]);
                }
            }

            for (int i = 0; i < groups; i++) {
                weights.setRowVector(i, meanOfGroup[i]);
            }
        }
    }

    private void kohonenAlgorithm() {
        plotPoints();
        double[][] arrayOfWeights = new double[groups * groups][2];
        Random random = new Random();
        for (int i = 0; i < groups * groups; i++) {
            arrayOfWeights[i][0] = random.nextDouble() - 0.5;
            arrayOfWeights[i][1] = random.nextDouble() - 0.5;
        }

        weights = new Array2DRowRealMatrix(arrayOfWeights);
        for (int currentEpoch = 0; (currentEpoch < epochLimit) && (!isCancelled()); currentEpoch++) {
            System.out.println(currentEpoch);
            this.publish(weights);

            if (currentEpoch != 0) {
                currentLearningRate = calculateCurrentLearningRate(currentEpoch);
                currentRadius = calculateCurrentRadius(currentEpoch);
                Collections.shuffle(inputs);
            }

            for (int j = 0; j < inputs.size(); j++) {
                double minDistance = weights.getRowVector(0).getDistance(inputs.get(j));
                int index = 0;
                for (int i = 1; i < groups * groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    if (weight.getDistance(inputs.get(j)) < minDistance) {
                        minDistance = weight.getDistance(inputs.get(j));
                        index = i;
                    }
                }

                int BMUx = (index % groups);
                int BMUy = (index / groups);
                for (int i = 0; i < groups * groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    double distanceBetweenBMUAndNeighbourhood = weight.getDistance(weights.getRowVector(index));

                    int neuronX = (i % groups);
                    int neuronY = (i / groups);
                    double distance = (BMUy - neuronY) * (BMUy - neuronY) + (BMUx - neuronX) * (BMUx - neuronX);
                    distance = Math.sqrt(distance);

                    if (distance < currentRadius) {
                        weights.setRowVector(i, calculateUpdate(inputs.get(j), weight, distanceBetweenBMUAndNeighbourhood));
                    }
                }

            }
        }
    }

    private void neuralGasAlgorithm() {
        plotPoints();
        double[][] arrayOfWeights = new double[groups][2];

        Random random = new Random();
        for (int i = 0; i < groups; i++) {
            arrayOfWeights[i][0] = random.nextDouble() - 0.5;
            arrayOfWeights[i][1] = random.nextDouble() - 0.5;
        }

        weights = new Array2DRowRealMatrix(arrayOfWeights);
        for (int currentEpoch = 0; (currentEpoch < epochLimit) && (!isCancelled()); currentEpoch++) {
            System.out.println(currentEpoch);

            this.publish(weights);
            if (currentEpoch != 0) {
                currentLearningRate = calculateCurrentLearningRate(currentEpoch);
                currentRadius = calculateCurrentRadius(currentEpoch);
                Collections.shuffle(inputs);
            }

            for (int j = 0; j < inputs.size(); j++) {
                double minDistance = weights.getRowVector(0).getDistance(inputs.get(j));
                int index = 0;
                for (int i = 1; i < groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    if (weight.getDistance(inputs.get(j)) < minDistance) {
                        minDistance = weight.getDistance(inputs.get(j));
                        index = i;
                    }
                }

                List<Pair<Integer, Double>> seriesOfDistance = new ArrayList<>();
                for (int i = 0; i < groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    seriesOfDistance.add(i, new Pair<>(i, weight.getDistance(weights.getRowVector(index))));
                }

                seriesOfDistance.sort(new Comparator<Pair<Integer, Double>>() {
                    @Override
                    public int compare(Pair<Integer, Double> p1, Pair<Integer, Double> p2) {
                        return p1.getSecond() < p2.getSecond() ? -1 : p1.getSecond() == p2.getSecond() ? 0 : 1;
                    }
                });

                for (int i = 0; i < groups; i++) {
                    RealVector weight = weights.getRowVector(i);
                    int positionInSeries = 0;
                    for (int k = 0; k < seriesOfDistance.size(); k++) {
                        if (seriesOfDistance.get(k).getFirst() == i) {
                            positionInSeries = k;
                        }
                    }

                    if (seriesOfDistance.get(positionInSeries).getSecond() < currentRadius) {
                        weights.setRowVector(i, calculateUpdate(inputs.get(j), weight, positionInSeries));
                    }
                }
            }
        }
    }

    private double calculateCurrentLearningRate(int currentEpoch) {
        switch (quantizationType) {
            case KohonenAlgorithm:
                return startLearningRate * exp((-1) * ((double) currentEpoch / epochLimit));
            case NeuralGasAlgorithm:
            case ImageCompression:
                double kDivideBykMax = ((double) currentEpoch) / ((double) epochLimit);
                return (startLearningRate) * pow(0.01 / startLearningRate, kDivideBykMax);
        }
        return 0;
    }

    private double calculateCurrentRadius(int currentEpoch) {
        switch (quantizationType) {
            case KohonenAlgorithm:
                return startRadius * exp(((-1) * (double) currentEpoch) / lambda);
            case NeuralGasAlgorithm:
            case ImageCompression:
                double lambdaMax = ((double) groups) / 2.0;
                double kDivideBykMax = ((double) currentEpoch) / ((double) epochLimit);
                return (lambdaMax) * pow(0.01 / lambdaMax, kDivideBykMax);
        }
        return 0;
    }

    private double gaussiannNeighborhoodFunction(double distanceOrPosition) {
        switch (quantizationType) {
            case KohonenAlgorithm:
                return exp((-1) * ((distanceOrPosition * distanceOrPosition) / (2 * this.currentRadius * this.currentRadius)));
            case NeuralGasAlgorithm:
            case ImageCompression:
                return exp((-1) * (distanceOrPosition / this.currentRadius));
        }
        return 0;
    }

    private RealVector calculateUpdate(RealVector input, RealVector weight, double distanceOrPosition) {
        RealVector newVector = weight.add(input.subtract(weight).mapMultiply(gaussiannNeighborhoodFunction(distanceOrPosition) * currentLearningRate));
        return newVector;
    }

    private RealVector calculateUpdateForPixels(List<RealVector> input, RealVector weight, double distanceOrPosition) {
        RealVector meanDifference = new ArrayRealVector(weight.getDimension());
        for (RealVector pixel : input) {
            meanDifference = meanDifference.add(pixel.subtract(weight));
        }
        meanDifference.mapDivideToSelf(input.size());

        RealVector newVector = weight.add(meanDifference.mapMultiply(gaussiannNeighborhoodFunction(distanceOrPosition) * currentLearningRate));
        return newVector;
    }

    private void plotPoints() {
        frame = new PlotFrame("Wykres");
        frame.plotFrame(inputs);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                cancelLearning();
            }
        });
    }
}
