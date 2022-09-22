package org.wickedsource.docxstamper.util;

import org.docx4j.dml.Graphic;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.R;
import org.wickedsource.docxstamper.replace.typeresolver.image.ImageResolver;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentUtil {
    public static List<Object> prepareDocumentForInsert(WordprocessingMLPackage sourceDocument, WordprocessingMLPackage destDocument) throws Exception {
        return walkObjects(sourceDocument.getMainDocumentPart(), sourceDocument, destDocument, destDocument.getMainDocumentPart());
    }

    private static List<Object> walkObjects(ContentAccessor sourceContainer, WordprocessingMLPackage sourceDocument, WordprocessingMLPackage destDocument, ContentAccessor destContainer) throws Exception {
        List<Object> result = new ArrayList<>();
        for (Object obj : sourceContainer.getContent()) {
            if (obj instanceof R && isImageRun((R) obj)) {
                byte[] imageData = getRunDrawingData((R) obj, sourceDocument);
                // TODO : retrieve filename, altText and width from source document
                result.add(ImageResolver.createRunWithImage(destDocument, imageData, null, null, null));
            } else if (obj instanceof ContentAccessor) {
                result.addAll(walkObjects((ContentAccessor) obj, sourceDocument, destDocument, (ContentAccessor) obj));
            } else {
                result.add(obj);
            }
        }
        return result;
    }

    private static byte[] getRunDrawingData(R obj, WordprocessingMLPackage document) throws Docx4JException, IOException {
        for (Object runElement : obj.getContent()) {
            if (runElement instanceof JAXBElement && ((JAXBElement) runElement).getValue() instanceof Drawing) {
                Drawing drawing = (Drawing) ((JAXBElement) runElement).getValue();
                byte[] imageData = getImageData(document, drawing);
                return imageData;
            }
        }
        throw new RuntimeException("Run drawing not found !");
    }

    private static byte[] getImageData(WordprocessingMLPackage document, Drawing drawing) throws IOException, Docx4JException {
        String imageRelId = getImageRelationshipId(drawing);
        Part imageRelPart = document.getMainDocumentPart().getRelationshipsPart().getPart(imageRelId);
        // TODO : find a better way to find image rel part name in source part store
        String imageRelPartName = imageRelPart.getPartName().getName().substring(1);
        byte[] imageData = streamToByteArray(
                document.getSourcePartStore().getPartSize(imageRelPartName),
                document.getSourcePartStore().loadPart(imageRelPartName)
        );
        return imageData;
    }

    private static boolean isImageRun(R obj) {
        for (Object runElement : obj.getContent()) {
            if (runElement instanceof JAXBElement && ((JAXBElement) runElement).getValue() instanceof Drawing) {
                return true;
            }
        }
        return false;
    }

    private static String getImageRelationshipId(Drawing drawing) {
        Graphic graphic = getInlineGraphic(drawing);
        return graphic.getGraphicData().getPic().getBlipFill().getBlip().getEmbed();
    }

    private static Graphic getInlineGraphic(Drawing drawing) {
        if (drawing.getAnchorOrInline().isEmpty()) {
            throw new RuntimeException("Anchor or Inline is empty !");
        }
        Object anchorOrInline = drawing.getAnchorOrInline().get(0);
        if (anchorOrInline instanceof Inline) {
            Inline inline = ((Inline) anchorOrInline);
            return inline.getGraphic();
        } else {
            throw new RuntimeException("Don't know how to process anchor !");
        }
    }

    private static byte[] streamToByteArray(long size, InputStream is) throws IOException {
        if (size > Integer.MAX_VALUE) {
            throw new RuntimeException("Image size exceeds maximum allowed (2GB)");
        }
        int intSize = (int) size;
        byte[] data = new byte[intSize];
        int offset = 0;
        int numRead;
        while ((numRead = is.read(data, offset, intSize - offset)) > 0) {
            offset += numRead;
        }
        is.close();
        return data;
    }
}
