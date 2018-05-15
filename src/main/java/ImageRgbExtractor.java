import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

public class ImageRgbExtractor
{
    private int width;
    private int height;
    private boolean hasAlphaChannel;
    private int pixelLength;
    private byte[] pixels;

    ImageRgbExtractor(BufferedImage image)
    {
        pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        width = image.getWidth();
        height = image.getHeight();
        hasAlphaChannel = image.getAlphaRaster() != null;
        if (image.getType() == 	TYPE_BYTE_GRAY) {
            pixelLength = 1;
        } else {
            pixelLength = 3;
            if (hasAlphaChannel) {
                pixelLength = 4;
            }
        }
    }

    RealVector getRGBVector(int x, int y)
    {
        RealVector colorVector = new ArrayRealVector(4);
        int pos = (y * pixelLength * width) + (x * pixelLength);

        if (pixelLength > 1) {
            try {
                if (hasAlphaChannel) {
                    colorVector.setEntry(0, pixels[pos++]); // alpha
                } else {
                    colorVector.setEntry(0, 255);
                }

                colorVector.setEntry(1, pixels[pos++] & 0xFF); // blue
                colorVector.setEntry(2, pixels[pos++] & 0xFF); // green
                colorVector.setEntry(3, pixels[pos++] & 0xFF); // red
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        } else {
            colorVector.setEntry(0, 255);

            colorVector.setEntry(1, pixels[pos] & 0xFF); // blue
            colorVector.setEntry(2, pixels[pos] & 0xFF); // green
            colorVector.setEntry(3, pixels[pos] & 0xFF); // red
        }

        return colorVector;
    }
}