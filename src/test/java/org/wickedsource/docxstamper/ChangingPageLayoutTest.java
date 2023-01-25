package org.wickedsource.docxstamper;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Test;
import org.springframework.context.expression.MapAccessor;
import org.wickedsource.docxstamper.context.NameContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangingPageLayoutTest extends AbstractDocx4jTest {
    @Test
    public void shouldKeepPageLayoutInformationAfterStamping() throws IOException, Docx4JException {
        Map<String, Object> context = new HashMap<>();

        NameContext name1 = new NameContext();
        name1.setName("Homer");

        NameContext name2 = new NameContext();
        name2.setName("Marge");

        List repeatValues = new ArrayList();
        repeatValues.add(name1);
        repeatValues.add(name2);

        context.put("repeatValues", repeatValues);

        InputStream template = getClass().getResourceAsStream("ChangingPageLayoutTest.docx");
        DocxStamperConfiguration config = new DocxStamperConfiguration()
                .setEvaluationContextConfigurer(ctx -> ctx.addPropertyAccessor(new MapAccessor()));

        WordprocessingMLPackage result = stampAndLoad(template, context, config);

        result.save(new File("RESULTAAT.docx"));
    }
}
