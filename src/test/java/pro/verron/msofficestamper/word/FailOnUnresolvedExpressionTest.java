package pro.verron.msofficestamper.word;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.api.UnresolvedExpressionException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static pro.verron.msofficestamper.utils.ResourceUtils.docx;

class FailOnUnresolvedExpressionTest {
    @Test
    void fails() throws IOException {
        var context = new Name("Homer");
        try (var template = docx(Path.of("FailOnUnresolvedExpressionTest" +
                                         ".docx"))) {
            var stamper = new DocxStamper<Name>(new DocxStamperConfiguration());
            var outputStream = new ByteArrayOutputStream();
            assertThrows(UnresolvedExpressionException.class, () -> stamper.stamp(template, context, outputStream));
        }
    }

    @Test
    void doesNotFail() throws IOException {
        Name context = new Name("Homer");
        try (InputStream template = docx(Path.of(
                "FailOnUnresolvedExpressionTest.docx"))) {
            var config = new DocxStamperConfiguration()
                    .setFailOnUnresolvedExpression(false);
            var stamper = new DocxStamper<Name>(config);
            Assertions.assertDoesNotThrow(() -> stamper.stamp(template, context, new ByteArrayOutputStream()));
        }
    }

    public record Name(String name) {
    }

}
