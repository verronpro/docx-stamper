package org.wickedsource.docxstamper;

import jakarta.xml.bind.JAXBElement;
import org.docx4j.dml.CTBlip;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.MapAccessor;
import org.wickedsource.docxstamper.replace.typeresolver.image.Image;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RepeatDocPartWithImageTest {
	@Test
	public void shouldImportImageDataInTheMainDocument() throws Docx4JException, IOException {
		var butterfly = new Image(getClass().getResourceAsStream("butterfly.png"));
		var worldMap = new Image(getClass().getResourceAsStream("map.jpg"));
		var context = Map.of(
				"units", Stream.of(butterfly, worldMap)
							   .map(image -> Map.of("coverImage", image))
							   .map(map -> Map.of("productionFacility", map))
							   .toList());

		var config = new DocxStamperConfiguration()
				.setEvaluationContextConfigurer(ctx -> ctx.addPropertyAccessor(new MapAccessor()));

		var template = getClass().getResourceAsStream("RepeatDocPartWithImageTest.docx");

		var stamper = new TestDocxStamper<Map<String, ?>>(config);
		var document = stamper.stampAndLoad(template, context);
		List<Object> content = document.getMainDocumentPart().getContent();
		CTBlip blip1 = ((Inline) ((Drawing) ((JAXBElement) ((R) ((P) content.get(1)).getContent().get(1)).getContent()
																										 .get(0)).getValue()).getAnchorOrInline()
																															 .get(0)).getGraphic()
																																	 .getGraphicData()
																																	 .getPic()
																																	 .getBlipFill()
																																	 .getBlip();
		assertEquals(blip1.getEmbed(), "rId11");
		CTBlip blip2 = ((Inline) ((Drawing) ((JAXBElement) ((R) ((P) content.get(2)).getContent().get(1)).getContent()
																										 .get(0)).getValue()).getAnchorOrInline()
																															 .get(0)).getGraphic()
																																	 .getGraphicData()
																																	 .getPic()
																																	 .getBlipFill()
																																	 .getBlip();
		assertEquals(blip2.getEmbed(), "rId12");
		CTBlip blip3 = ((Inline) ((Drawing) ((JAXBElement) ((R) ((P) content.get(7)).getContent().get(1)).getContent()
																										 .get(0)).getValue()).getAnchorOrInline()
																															 .get(0)).getGraphic()
																																	 .getGraphicData()
																																	 .getPic()
																																	 .getBlipFill()
																																	 .getBlip();
		assertEquals(blip3.getEmbed(), "rId13");
		var partStore = document.getSourcePartStore();
		var units = context.get("units");
		var img0 = units.get(0).get("productionFacility").get("coverImage").getImageBytes().length;
		var img1 = units.get(1).get("productionFacility").get("coverImage").getImageBytes().length;
		assertEquals(img0, partStore.getPartSize("word/media/document_image_rId11.png"));
		assertEquals(img0, partStore.getPartSize("word/media/document_image_rId13.png"));
		assertEquals(img1, partStore.getPartSize("word/media/document_image_rId12.jpeg"));
	}
}
