package org.wickedsource.docxstamper;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Test;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.wickedsource.docxstamper.replace.typeresolver.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepeatDocPartWithImageTest extends AbstractDocx4jTest {
    @Test
    public void test() throws Docx4JException, IOException {
        List<Image> images = new ArrayList<>();
        images.add(new Image(getClass().getResourceAsStream("butterfly.png")));
        images.add(new Image(getClass().getResourceAsStream("map.jpg")));

        Map<String, Object> context = new HashMap<>();
        ArrayList<Object> units = new ArrayList<>();

        images.forEach(image -> {
            Map<String, Object> unit = new HashMap<>();
            Map<String, Object> productionFacility = new HashMap<>();
            unit.put("productionFacility", productionFacility);
            productionFacility.put("coverImage", image);
            units.add(unit);
        });

        context.put("units", units);

        DocxStamperConfiguration config = new DocxStamperConfiguration()
                .setEvaluationContextConfigurer((StandardEvaluationContext ctx) -> ctx.addPropertyAccessor(new MapAccessor()));

        InputStream template = getClass().getResourceAsStream("RepeatDocPartWithImageTest.docx");
        WordprocessingMLPackage document = stampAndLoad(template, context, config);

        document.save(new File("output.docx"));

        // List<Object> documentContent = document.getMainDocumentPart().getContent();
    }

}