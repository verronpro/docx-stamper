package org.wickedsource.docxstamper;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Test;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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

    class StampTable {
        public List<String> headers;
        public List<List<String>> records;

        public StampTable(
                List<String> headers,
                List<List<String>> records
        ) {
            this.headers = headers;
            this.records = records;
        }
    }

    @Test
    public void shouldResolveMapAndPropertyPlaceholders() throws Docx4JException, IOException {
        List<Container> listProp = new ArrayList<>();
        listProp.add(new Container("first value"));
        listProp.add(new Container("second value"));

        List<String> headers = new ArrayList<>();
        headers.add("Values");

        List<List<String>> records = new ArrayList<>();
        ArrayList<String> record = new ArrayList<>();
        record.add("stamp value");
        records.add(record);

        StampTable stampProp = new StampTable(headers, records);

        Map<String, Object> context = new HashMap<>();
        context.put("FLAT_STRING", "Flat string has been resolved");
        context.put("OBJECT_LIST_PROP", listProp);
        context.put("OBJECT_STAMP_PROP", stampProp);

        DocxStamperConfiguration config = new DocxStamperConfiguration()
                .setEvaluationContextConfigurer((StandardEvaluationContext ctx) -> ctx.addPropertyAccessor(new MapAccessor()));

        InputStream template = getClass().getResourceAsStream("MapAccessorAndReflectivePropertyAccessorTest.docx");
        WordprocessingMLPackage result = stampAndLoad(template, context, config);
        result.save(new File("RESULTAAAAA.docx"));
    }
}
