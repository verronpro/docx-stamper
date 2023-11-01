package org.wickedsource.docxstamper.replace.typeresolver.image;

import org.wickedsource.docxstamper.api.DocxStamperException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an image that can be inserted into a document.
 * <p>
 * The image can be created from an {@link InputStream} or from a byte array.
 * If the image is created from an {@link InputStream}, the {@link InputStream} will be closed after the image
 * has been created.
 * If the image is created from a byte array, the byte array will be copied and the original byte array will not
 * be modified.
 * The image can be created with a maximum width. If the image is wider than the maximum width, it will be scaled
 * down to the maximum width while keeping the aspect ratio.
 * If the image is smaller than the maximum width, it will not be scaled up.
 * If the image is created without a maximum width, it will not be scaled at all.
 * The maximum width is measured in twips (1/20th of a point).
 * </p>
 */
public class Image {

    private final byte[] imageBytes;
    private Integer maxWidth;

    /**
     * Creates an image from the given {@link InputStream}.
     *
     * @param in the {@link InputStream} to read the image from.
     */
    public Image(InputStream in) {
        this(in, null);
    }

    /**
     * Creates an image from the given {@link InputStream} and scales it down to the given maximum width.
     *
     * @param in       the {@link InputStream} to read the image from.
     * @param maxWidth the maximum width of the image in twips (1/20th of a point).
     */
    public Image(InputStream in, Integer maxWidth) {
        this(allBytes(in), maxWidth);
    }

    public Image(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public Image(byte[] imageBytes, Integer maxWidth) {
        this.imageBytes = imageBytes;
        this.maxWidth = maxWidth;
    }

    private static byte[] allBytes(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new DocxStamperException("Failed to read stream", e);
        }
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }
}
