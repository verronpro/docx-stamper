package org.wickedsource.docxstamper.preprocessor;

import org.docx4j.TraversalUtil;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.utils.TraversalUtilVisitor;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.wickedsource.docxstamper.api.preprocessor.PreProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MergeSameStyleRuns implements PreProcessor {
	private final List<List<R>> similarStyleConcurrentRuns = new ArrayList<>();
	private final TraversalUtilVisitor<R> visitor = new TraversalUtilVisitor<>() {
		@Override
		public void apply(R element, Object parent, List<Object> siblings) {
			RPr rPr = element.getRPr();
			int currentIndex = siblings.indexOf(element);
			int nextIndex = currentIndex + 1;
			List<R> similarStyleConcurrentRun = new ArrayList<>(List.of(element));
			while (siblings.size() > nextIndex
					&& siblings.get(nextIndex) instanceof R nextRun
					&& Objects.equals(nextRun.getRPr(), rPr)) {
				similarStyleConcurrentRun.add(nextRun);
				nextIndex++;
			}
			if (similarStyleConcurrentRun.size() > 1)
				similarStyleConcurrentRuns.add(similarStyleConcurrentRun);
		}
	};

	@Override
	public void process(WordprocessingMLPackage document) {
		var mainDocumentPart = document.getMainDocumentPart();
		TraversalUtil.visit(mainDocumentPart, visitor);
		for (List<R> similarStyleConcurrentRun : similarStyleConcurrentRuns) {
			R firstRun = similarStyleConcurrentRun.get(0);
			List<Object> firstRunContent = firstRun.getContent();
			ContentAccessor firstRunParent = (ContentAccessor) firstRun.getParent();
			for (int i = 1; i < similarStyleConcurrentRun.size(); i++) {
				R r = similarStyleConcurrentRun.get(i);
				firstRunContent.addAll(r.getContent());
				firstRunParent.getContent().remove(r);
			}
		}
	}
}
