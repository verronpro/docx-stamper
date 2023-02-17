package org.wickedsource.docxstamper;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.expression.MapAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepeatDocPartAndCommentProcessorsIsolationTest extends AbstractDocx4jTest {

    static class TableValue {
        public String value;

        TableValue(String value) {
            this.value = value;
        }
    }

    @Test
    public void repeatDocPartShouldNotUseSameCommentProcessorInstancesForSubtemplate() throws Docx4JException, IOException {
        Map<String, Object> context = new HashMap<>();

        List<TableValue> firstTable = new ArrayList<>();
        firstTable.add(new TableValue("firstTable value1"));
        firstTable.add(new TableValue("firstTable value2"));

        List<TableValue> secondTable = new ArrayList<>();
        secondTable.add(new TableValue("repeatDocPart value1"));
        secondTable.add(new TableValue("repeatDocPart value2"));
        secondTable.add(new TableValue("repeatDocPart value3"));

        List<TableValue> thirdTable = new ArrayList<>();
        thirdTable.add(new TableValue("secondTable value1"));
        thirdTable.add(new TableValue("secondTable value2"));
        thirdTable.add(new TableValue("secondTable value3"));
        thirdTable.add(new TableValue("secondTable value4"));

        context.put("firstTable", firstTable);
        context.put("secondTable", secondTable);
        context.put("thirdTable", thirdTable);

        InputStream template = getClass().getResourceAsStream("RepeatDocPartAndCommentProcessorsIsolationTest.docx");
        DocxStamperConfiguration config = new DocxStamperConfiguration();
        config.setEvaluationContextConfigurer((ctx) -> ctx.addPropertyAccessor(new MapAccessor()));
        WordprocessingMLPackage document = stampAndLoad(template, context, config);

        List<Object> documentContent = document.getMainDocumentPart().getContent();

        Assert.assertEquals(19, documentContent.size());

        Assert.assertEquals("This will stay untouched.", documentContent.get(0).toString());
        Assert.assertEquals("This will also stay untouched.", documentContent.get(4).toString());
        Assert.assertEquals("This will stay untouched too.", documentContent.get(18).toString());

        // checking table before repeating paragraph
        Tbl table1 = (Tbl) XmlUtils.unwrap(documentContent.get(2));
        checkTableAgainstContextValues(firstTable, table1);

        // checking repeating paragraph
        Assert.assertEquals("Repeating paragraph :", documentContent.get(6).toString());
        Assert.assertEquals("Repeating paragraph :", documentContent.get(9).toString());
        Assert.assertEquals("Repeating paragraph :", documentContent.get(12).toString());

        Assert.assertEquals("repeatDocPart value1", documentContent.get(8).toString());
        Assert.assertEquals("repeatDocPart value2", documentContent.get(11).toString());
        Assert.assertEquals("repeatDocPart value3", documentContent.get(14).toString());

        // checking table after repeating paragraph
        Tbl table2 = (Tbl) XmlUtils.unwrap(documentContent.get(16));
        checkTableAgainstContextValues(thirdTable, table2);
    }

    private static void checkTableAgainstContextValues(List<TableValue> tableValues, Tbl docxTable) {
        Assert.assertEquals(tableValues.size(), docxTable.getContent().size());
        for (int i = 0; i < tableValues.size(); i++) {
            Tr row = (Tr) docxTable.getContent().get(i);
            Assert.assertEquals(1, row.getContent().size());

            Tc cell = (Tc) XmlUtils.unwrap(row.getContent().get(0));
            String expected = tableValues.get(i).value;
            Assert.assertEquals(1, cell.getContent().size());
            Assert.assertEquals(expected, cell.getContent().get(0).toString());
        }
    }
}