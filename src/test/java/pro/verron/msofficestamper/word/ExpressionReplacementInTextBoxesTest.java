package pro.verron.msofficestamper.word;

import org.docx4j.dml.wordprocessingDrawing.Anchor;
import org.junit.jupiter.api.Test;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import pro.verron.msofficestamper.utils.TestDocxStamper;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static pro.verron.msofficestamper.utils.ResourceUtils.docx;

class ExpressionReplacementInTextBoxesTest {
	@Test
    void expressionReplacementInTextBoxesTest() {
		var context = new Name("Bart Simpson");
		var template = docx(Path.of("ExpressionReplacementInTextBoxesTest" +
                                    ".docx"));
		var stamper = new TestDocxStamper<Name>(new DocxStamperConfiguration().setFailOnUnresolvedExpression(false));
		var actual = stamper.stampAndLoadAndExtract(template, context, Anchor.class);
		List<String> expected = List.of(
				"❬Bart Simpson❘color=auto❭",
				"❬${foo}❘color=auto❭"
		);
		assertIterableEquals(expected, actual);
	}

	public record Name(String name) {
	}
}
