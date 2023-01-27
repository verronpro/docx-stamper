package org.wickedsource.docxstamper.util;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.SectPr;

import java.util.List;

public class SectionUtil {
    private static final ObjectFactory factory = Context.getWmlObjectFactory();

    public static SectPr getPreviousSectionBreakIfPresent(Object firstObject, ContentAccessor parent) {
        int pIndex = parent.getContent().indexOf(firstObject);
        for (int i = pIndex - 1; i >= 0; i++) {
            Object prevObj = parent.getContent().get(i);
            if (prevObj instanceof P) {
                P prevParagraph = (P) prevObj;
                if (prevParagraph.getPPr() != null && prevParagraph.getPPr().getSectPr() != null) {
                    return prevParagraph.getPPr().getSectPr();
                }
                break;
            }
        }
        System.out.println("No previous section break found from : " + parent + ", first object index=" + pIndex);
        return null;
    }

    public static SectPr getWrappingSectionBreakIfPresent(P p) {
        if (p.getPPr() != null && p.getPPr().getSectPr() != null) {
            return p.getPPr().getSectPr();
        }
        return null;
    }

    public static boolean isOddNumberOfSectionBreaks(List<Object> objects) {
        int count = 0;
        for (Object obj : objects) {
            if (obj instanceof P) {
                P p = (P) obj;
                if (p.getPPr() != null && p.getPPr().getSectPr() != null) {
                    count++;
                }
            }
        }
        return (count & 1) != 0;
    }

    public static void applySectionBreakToParagraph(SectPr sectPr, P paragraph) {
        if (paragraph.getPPr() == null) {
            paragraph.setPPr(factory.createPPr());
        }
        paragraph.getPPr().setSectPr(XmlUtils.deepCopy(sectPr));
    }

}
