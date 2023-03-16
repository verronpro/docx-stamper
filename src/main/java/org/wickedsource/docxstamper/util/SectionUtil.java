package org.wickedsource.docxstamper.util;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.*;

import java.util.List;

public class SectionUtil {
	private static final ObjectFactory factory = Context.getWmlObjectFactory();

	public static SectPr getPreviousSectionBreakIfPresent(Object firstObject, ContentAccessor parent) {
		int pIndex = parent.getContent().indexOf(firstObject);
		for (int i = pIndex - 1; i >= 0; i++) {
			Object prevObj = parent.getContent().get(i);
			if (prevObj instanceof P prevParagraph) {
				// the first P preceding the object is the one potentially carrying a section break
				if (prevParagraph.getPPr() != null && prevParagraph.getPPr().getSectPr() != null) {
					return prevParagraph.getPPr().getSectPr();
				}
				break;
			}
		}
		System.out.println("No previous section break found from : " + parent + ", first object index=" + pIndex);
		return null;
	}

	public static SectPr getParagraphSectionBreak(P p) {
		return p.getPPr() != null && p.getPPr().getSectPr() != null
				? p.getPPr().getSectPr()
				: null;
	}

	public static boolean isOddNumberOfSectionBreaks(List<Object> objects) {
		long count = objects.stream()
							.filter(obj -> obj instanceof P)
							.map(obj -> (P) obj)
							.filter(p -> p.getPPr() != null && p.getPPr().getSectPr() != null)
							.count();
		return count % 2 != 0;
	}

	public static void applySectionBreakToParagraph(SectPr sectPr, P paragraph) {
		PPr currentPPr = paragraph.getPPr();
		PPr nextPPr = currentPPr != null ? currentPPr : factory.createPPr();
		paragraph.setPPr(nextPPr);
		nextPPr.setSectPr(XmlUtils.deepCopy(sectPr));
	}

}
