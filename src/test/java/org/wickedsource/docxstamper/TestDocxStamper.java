package org.wickedsource.docxstamper;

import org.docx4j.TextUtils;
import org.docx4j.TraversalUtil;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.wickedsource.docxstamper.util.RunCollector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * Common methods to interact with docx documents.
 */
public final class TestDocxStamper<T> {

	private final DocxStamper<T> stamper;

	public TestDocxStamper() {
		this(new DocxStamperConfiguration()
					 .setFailOnUnresolvedExpression(false));
	}

	public TestDocxStamper(DocxStamperConfiguration config) {
		stamper = new DocxStamper<>(config);
	}

	/**
	 * Stamps the given template resolving the expressions within the template against the specified contextRoot.
	 * Returns the resulting document after it has been saved and loaded again to ensure that changes in the Docx4j
	 * object structure were really transported into the XML of the .docx file.
	 */
	public WordprocessingMLPackage stampAndLoad(InputStream template, T contextRoot) throws IOException, Docx4JException {
		OutputStream out = IOStreams.getOutputStream();
		stamper.stamp(template, contextRoot, out);
		InputStream in = IOStreams.getInputStream(out);
		return WordprocessingMLPackage.load(in);
	}

	public List<String> stampAndLoadAndExtract(InputStream template, T context) {
		return streamElements(template, context, P.class)
				.map(this::serialize)
				.toList();
	}

	private <C> Stream<C> streamElements(InputStream template, T context, Class<C> clazz) {
		Stream<C> elements;
		try {
			var config = new DocxStamperConfiguration().setFailOnUnresolvedExpression(false);
			var out = IOStreams.getOutputStream();
			var stamper = new DocxStamper<T>(config);
			stamper.stamp(template, context, out);
			var in = IOStreams.getInputStream(out);
			var document = WordprocessingMLPackage.load(in);
			var visitor = newCollector(clazz);
			var content = document.getMainDocumentPart().getContent();
			TraversalUtil.visit(content, visitor);
			elements = visitor.elements();

		} catch (Docx4JException | IOException e) {
			throw new RuntimeException(e);
		}
		return elements;
	}

	private String serialize(P p) {
		String runs = extractDocumentRuns(p);
		return Optional.ofNullable(p.getPPr())
					   .map(ppr -> "%s//%s".formatted(runs, serialize(ppr)))
					   .orElse(runs);
	}

	private <C> DocxCollector<C> newCollector(Class<C> type) {
		return new DocxCollector<>(type);
	}

	private static String extractDocumentRuns(Object p) {
		var runCollector = new RunCollector();
		TraversalUtil.visit(p, runCollector);
		return runCollector.runs()
						   .filter(r -> !r.getContent().isEmpty())
						   .filter(r -> !TextUtils.getText(r).isEmpty())
						   .map(TestDocxStamper::serialize)
						   .collect(joining());
	}

	private String serialize(PPr pPr) {
		var set = new TreeSet<String>();
		if (pPr.getJc() != null) set.add("jc=" + pPr.getJc().getVal().value());
		if (pPr.getInd() != null) set.add("ind=" + pPr.getInd().getLeft().intValue());
		if (pPr.getKeepLines() != null) set.add("keepLines=" + pPr.getKeepLines().isVal());
		if (pPr.getKeepNext() != null) set.add("keepNext=" + pPr.getKeepNext().isVal());
		if (pPr.getOutlineLvl() != null) set.add("outlineLvl=" + pPr.getOutlineLvl().getVal().intValue());
		if (pPr.getPageBreakBefore() != null) set.add("pageBreakBefore=" + pPr.getPageBreakBefore().isVal());
		if (pPr.getPBdr() != null) set.add("pBdr=xxx");
		if (pPr.getPPrChange() != null) set.add("pPrChange=xxx");
		if (pPr.getRPr() != null) set.add("rPr={" + serialize(pPr.getRPr()) + "}");
		if (pPr.getSectPr() != null) set.add("sectPr={" + serialize(pPr.getSectPr()) + "}");
		if (pPr.getShd() != null) set.add("shd=xxx");
		if (pPr.getSpacing() != null) set.add("spacing=xxx");
		if (pPr.getSuppressAutoHyphens() != null) set.add("suppressAutoHyphens=xxx");
		if (pPr.getSuppressLineNumbers() != null) set.add("suppressLineNumbers=xxx");
		if (pPr.getSuppressOverlap() != null) set.add("suppressOverlap=xxx");
		if (pPr.getTabs() != null) set.add("tabs=xxx");
		if (pPr.getTextAlignment() != null) set.add("textAlignment=xxx");
		if (pPr.getTextDirection() != null) set.add("textDirection=xxx");
		if (pPr.getTopLinePunct() != null) set.add("topLinePunct=xxx");
		if (pPr.getWidowControl() != null) set.add("widowControl=xxx");
		if (pPr.getWordWrap() != null) set.add("wordWrap=xxx");
		if (pPr.getFramePr() != null) set.add("framePr=xxx");
		if (pPr.getDivId() != null) set.add("divId=xxx");
		if (pPr.getCnfStyle() != null) set.add("cnfStyle=xxx");
		return String.join(",", set);
	}

	private static String serialize(R run) {
		var runPresentation = Optional.ofNullable(run.getRPr());
		var runText = TextUtils.getText(run);
		return runPresentation
				.map(TestDocxStamper::serialize)
				.map(s -> "|%s/%s|".formatted(runText, s))
				.orElse(runText);
	}

	private static String serialize(RPrAbstract rPr) {
		var set = new TreeSet<String>();
		if (rPr.getB() != null) set.add("b=" + rPr.getB().isVal());
		if (rPr.getBdr() != null) set.add("bdr=xxx");
		if (rPr.getCaps() != null) set.add("caps=" + rPr.getCaps().isVal());
		if (rPr.getColor() != null) set.add("color=" + rPr.getColor().getVal());
		if (rPr.getDstrike() != null) set.add("dstrike=" + rPr.getDstrike().isVal());
		if (rPr.getI() != null) set.add("i=" + rPr.getI().isVal());
		if (rPr.getKern() != null) set.add("kern=" + rPr.getKern().getVal().intValue());
		if (rPr.getLang() != null) set.add("lang=" + rPr.getLang().getVal());
		//if (rPr.getRFonts() != null) set.add("rFonts=xxx:" + rPr.getRFonts().getHint().value());
		if (rPr.getRPrChange() != null) set.add("rPrChange=xxx");
		if (rPr.getRStyle() != null) set.add("rStyle=" + rPr.getRStyle().getVal());
		if (rPr.getRtl() != null) set.add("rtl=" + rPr.getRtl().isVal());
		if (rPr.getShadow() != null) set.add("shadow=" + rPr.getShadow().isVal());
		if (rPr.getShd() != null) set.add("shd=" + rPr.getShd().getColor());
		if (rPr.getSmallCaps() != null) set.add("smallCaps=" + rPr.getSmallCaps().isVal());
		if (rPr.getVertAlign() != null) set.add("vertAlign=" + rPr.getVertAlign().getVal().value());
		if (rPr.getSpacing() != null) set.add("spacing=" + rPr.getSpacing().getVal().intValue());
		if (rPr.getStrike() != null) set.add("strike=" + rPr.getStrike().isVal());
		if (rPr.getOutline() != null) set.add("outline=" + rPr.getOutline().isVal());
		if (rPr.getEmboss() != null) set.add("emboss=" + rPr.getEmboss().isVal());
		if (rPr.getImprint() != null) set.add("imprint=" + rPr.getImprint().isVal());
		if (rPr.getNoProof() != null) set.add("noProof=" + rPr.getNoProof().isVal());
		if (rPr.getSpecVanish() != null) set.add("specVanish=" + rPr.getSpecVanish().isVal());
		if (rPr.getU() != null) set.add("u=" + rPr.getU().getVal().value());
		if (rPr.getVanish() != null) set.add("vanish=" + rPr.getVanish().isVal());
		if (rPr.getW() != null) set.add("w=" + rPr.getW().getVal());
		if (rPr.getWebHidden() != null) set.add("webHidden=" + rPr.getWebHidden().isVal());
		if (rPr.getHighlight() != null) set.add("highlight=" + rPr.getHighlight().getVal());
		if (rPr.getEffect() != null) set.add("effect=" + rPr.getEffect().getVal().value());
		return String.join(",", set);
	}

	private String serialize(SectPr sectPr) {
		var set = new TreeSet<String>();
		if (sectPr.getEGHdrFtrReferences() != null) set.add("eGHdrFtrReferences=xxx");
		if (sectPr.getPgSz() != null) set.add("pgSz={" + serialize(sectPr.getPgSz()) + "}");
		if (sectPr.getPgMar() != null) set.add("pgMar=xxx");
		if (sectPr.getPaperSrc() != null) set.add("paperSrc=xxx");
		if (sectPr.getBidi() != null) set.add("bidi=xxx");
		if (sectPr.getRtlGutter() != null) set.add("rtlGutter=xxx");
		if (sectPr.getDocGrid() != null) set.add("docGrid=xxx");
		if (sectPr.getFormProt() != null) set.add("formProt=xxx");
		if (sectPr.getVAlign() != null) set.add("vAlign=xxx");
		if (sectPr.getNoEndnote() != null) set.add("noEndnote=xxx");
		if (sectPr.getTitlePg() != null) set.add("titlePg=xxx");
		if (sectPr.getTextDirection() != null) set.add("textDirection=xxx");
		if (sectPr.getRtlGutter() != null) set.add("rtlGutter=xxx");
		return String.join(",", set);
	}

	private String serialize(SectPr.PgSz pgSz) {
		var set = new TreeSet<String>();
		if (pgSz.getOrient() != null) set.add("orient=" + pgSz.getOrient().value());
		if (pgSz.getW() != null) set.add("w=" + pgSz.getW().intValue());
		if (pgSz.getH() != null) set.add("h=" + pgSz.getH().intValue());
		if (pgSz.getCode() != null) set.add("code=" + pgSz.getCode().intValue());
		return String.join(",", set);
	}

	public <C> List<String> stampAndLoadAndExtract(InputStream template, T context, Class<C> clazz) {
		return streamElements(template, context, clazz)
				.map(TestDocxStamper::extractDocumentRuns)
				.toList();
	}
}
