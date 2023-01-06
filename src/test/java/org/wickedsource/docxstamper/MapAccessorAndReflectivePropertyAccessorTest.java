package org.wickedsource.docxstamper;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Test;
import org.springframework.context.expression.MapAccessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAccessorAndReflectivePropertyAccessorTest extends AbstractDocx4jTest {
    class Container {
        public String value;

        public Container(String value) {
            this.value = value;
        }
    }

    @Test
    public void shouldResolveMapAndPropertyPlaceholders() throws Docx4JException, IOException {
        List<Container> listProp = new ArrayList<>();
        listProp.add(new Container("first value"));
        listProp.add(new Container("second value"));

        Map<String, Object> context = new HashMap<>();
        context.put("FLAT_STRINGs", "Flat string has been resolved");
        context.put("OBJECT_LIST_PROP", listProp);

        DocxStamperConfiguration config = new DocxStamperConfiguration()
                .setFailOnUnresolvedExpression(false)
                .setLineBreakPlaceholder("\n")
                .replaceNullValues(true)
                .nullValuesDefault("N/C")
                .replaceUnresolvedExpressions(true)
                .unresolvedExpressionsDefaultValue("N/C")
                .setEvaluationContextConfigurer(ctx -> ctx.addPropertyAccessor(new MapAccessor()));

        InputStream template = getClass().getResourceAsStream("MapAccessorAndReflectivePropertyAccessorTest.docx");
        WordprocessingMLPackage result = stampAndLoad(template, context, config);
        result.save(new File("RESULTAAAAA.docx"));
    }
}
