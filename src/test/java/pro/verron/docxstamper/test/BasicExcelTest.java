package pro.verron.docxstamper.test;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.docx4j.openpackaging.packages.SpreadsheetMLPackage.load;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pro.verron.docxstamper.preset.OfficeStampers.xlsxStamper;
import static pro.verron.docxstamper.test.IOStreams.getInputStream;
import static pro.verron.docxstamper.test.IOStreams.getOutputStream;
import static pro.verron.docxstamper.test.Stringifier.stringifyExcel;

public class BasicExcelTest {
    @Test
    public void testStamper()
            throws IOException, Docx4JException {

        var stamper = xlsxStamper();
        var templatePath = Path.of("test", "sources", "excel-base.xlsx");
        var templateStream = Files.newInputStream(templatePath);

        var templatePackage = load(templateStream);
        var templateExpectedString = """
                A1: Hello
                B1: ${name}""";
        var templateActualString = stringifyExcel(templatePackage);
        assertEquals(templateExpectedString, templateActualString);
        var stampedOutputStream = getOutputStream();
        record Person(String name) {}
        var context = new Person("Bart");

        stamper.stamp(templatePackage, context, stampedOutputStream);

        var stampedReadStream = getInputStream(stampedOutputStream);
        var stampedPackage = load(stampedReadStream);
        var stampedExpectedString = """
                A1: Hello
                B1: Bart""";
        var stampedActualString = stringifyExcel(stampedPackage);
        assertEquals(stampedExpectedString, stampedActualString);
    }
}