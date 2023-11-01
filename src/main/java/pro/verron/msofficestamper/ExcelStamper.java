package pro.verron.msofficestamper;

import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.wickedsource.docxstamper.api.DocxStamperException;

import java.io.OutputStream;

public class ExcelStamper
        implements OpcStamper<SpreadsheetMLPackage> {
    @Override
    public void stamp(
            SpreadsheetMLPackage template,
            Object context,
            OutputStream outputStream
    ) throws DocxStamperException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
