package org.wickedsource.docxstamper.processor.repeat;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.CommentsPart;
import org.docx4j.wml.*;
import org.jvnet.jaxb2_commons.ppp.Child;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import org.wickedsource.docxstamper.processor.BaseCommentProcessor;
import org.wickedsource.docxstamper.util.CommentWrapper;
import org.wickedsource.docxstamper.util.DocumentUtil;
import org.wickedsource.docxstamper.util.ParagraphUtil;
import org.wickedsource.docxstamper.util.walk.BaseDocumentWalker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class RepeatDocPartProcessor extends BaseCommentProcessor implements IRepeatDocPartProcessor {

    private final DocxStamperConfiguration config;

    private Map<CommentWrapper, List<Object>> subContexts = new HashMap<>();
    private Map<CommentWrapper, ContentAccessor> repeatElementsMap = new HashMap<>();
    private Map<CommentWrapper, WordprocessingMLPackage> subTemplates = new HashMap<>();
    private Map<CommentWrapper, ContentAccessor> gcpMap = new HashMap<>();

    private static ObjectFactory objectFactory = null;

    public RepeatDocPartProcessor(DocxStamperConfiguration config) {
        this.config = config;
    }

    @Override
    public void repeatDocPart(List<Object> contexts) {
        if (contexts == null) {
            contexts = Collections.emptyList();
        }

        CommentWrapper currentCommentWrapper = getCurrentCommentWrapper();
        ContentAccessor gcp = findGreatestCommonParent(
                currentCommentWrapper.getCommentRangeEnd().getParent(),
                (ContentAccessor) currentCommentWrapper.getCommentRangeStart().getParent()
        );
        ContentAccessor repeatElements = getRepeatElements(currentCommentWrapper, gcp);

        if (!repeatElements.getContent().isEmpty()) {
            try {
                subContexts.put(currentCommentWrapper, contexts);
                subTemplates.put(currentCommentWrapper, extractSubTemplate(currentCommentWrapper, repeatElements, getOrCreateObjectFactory()));
                gcpMap.put(currentCommentWrapper, gcp);
                repeatElementsMap.put(currentCommentWrapper, repeatElements);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static ObjectFactory getOrCreateObjectFactory() {
        if (objectFactory == null) {
            objectFactory = Context.getWmlObjectFactory();
        }
        return objectFactory;
    }

    private WordprocessingMLPackage copyTemplate(WordprocessingMLPackage doc) throws Docx4JException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return WordprocessingMLPackage.load(new ByteArrayInputStream(baos.toByteArray()));
    }

    @Override
    public void commitChanges(WordprocessingMLPackage document) {
        for (CommentWrapper commentWrapper : subContexts.keySet()) {
            List<Object> expressionContexts = subContexts.get(commentWrapper);

            // index changes after each replacement so we need to get the insert index at the right moment.
            ContentAccessor insertParentContentAccessor = gcpMap.get(commentWrapper);
            Integer index = insertParentContentAccessor.getContent().indexOf(repeatElementsMap.get(commentWrapper).getContent().get(0));

            if (expressionContexts != null) {
                for (Object subContext : expressionContexts) {
                    try {
                        WordprocessingMLPackage subTemplate = copyTemplate(subTemplates.get(commentWrapper));
                        DocxStamper<Object> stamper = new DocxStamper<>(config);
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        stamper.stamp(subTemplate, subContext, output);
                        WordprocessingMLPackage subDocument = WordprocessingMLPackage.load(new ByteArrayInputStream(output.toByteArray()));
                        try {
                            List<Object> changes = DocumentUtil.prepareDocumentForInsert(subDocument, document);
                            insertParentContentAccessor.getContent().addAll(index, changes);
                            index += changes.size();
                        } catch (Exception e) {
                            throw new RuntimeException("Unexpected error occured ! Skipping this comment", e);
                        }
                    } catch (Docx4JException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (config.isReplaceNullValues() && config.getNullValuesDefault() != null) {
                insertParentContentAccessor.getContent().add(index, ParagraphUtil.create(config.getNullValuesDefault()));
            }

            insertParentContentAccessor.getContent().removeAll(repeatElementsMap.get(commentWrapper).getContent());
        }
    }

    @Override
    public void reset() {
        subContexts = new HashMap<>();
        subTemplates = new HashMap<>();
        gcpMap = new HashMap<>();
        repeatElementsMap = new HashMap<>();
    }

    private WordprocessingMLPackage extractSubTemplate(CommentWrapper commentWrapper, ContentAccessor repeatElements, ObjectFactory objectFactory) throws Exception {
        WordprocessingMLPackage document = getDocument();
        WordprocessingMLPackage subDocument = WordprocessingMLPackage.createPackage();

        CommentsPart commentsPart = new CommentsPart();
        subDocument.getMainDocumentPart().addTargetPart(commentsPart);

        // copy the elements to repeat without comment range anchors
        List<Object> finalRepeatElements = XmlUtils.deepCopy(repeatElements.getContent());
        removeCommentAnchorsFromFinalElements(commentWrapper, finalRepeatElements);
        subDocument.getMainDocumentPart().getContent().addAll(finalRepeatElements);

        // copy the images from parent document using the original repeat elements
        DocumentUtil.walkObjectsAndImportImages(repeatElements, document, subDocument);

        Comments comments = objectFactory.createComments();
        commentWrapper.getChildren().forEach(comment -> comments.getComment().add(comment.getComment()));
        commentsPart.setContents(comments);

        return subDocument;
    }

    private static void removeCommentAnchorsFromFinalElements(CommentWrapper commentWrapper, List<Object> finalRepeatElements) {
        List<Object> commentsToRemove = new ArrayList<>();
        new BaseDocumentWalker(() -> finalRepeatElements) {
            @Override
            protected void onCommentRangeStart(CommentRangeStart commentRangeStart) {
                if (commentRangeStart.getId().equals(commentWrapper.getComment().getId())) {
                    commentsToRemove.add(commentRangeStart);
                }
            }

            @Override
            protected void onCommentRangeEnd(CommentRangeEnd commentRangeEnd) {
                if (commentRangeEnd.getId().equals(commentWrapper.getComment().getId())) {
                    commentsToRemove.add(commentRangeEnd);
                }
            }
        };

        for (Object commentAnchorToRemove : commentsToRemove) {
            if (commentAnchorToRemove instanceof CommentRangeStart) {
                ContentAccessor parent = ((ContentAccessor) ((CommentRangeStart) commentAnchorToRemove).getParent());
                parent.getContent().remove(commentAnchorToRemove);
            } else if (commentAnchorToRemove instanceof CommentRangeEnd) {
                ContentAccessor parent = ((ContentAccessor) ((CommentRangeEnd) commentAnchorToRemove).getParent());
                parent.getContent().remove(commentAnchorToRemove);
            } else {
                throw new RuntimeException("Unknown comment anchor type given to remove !");
            }
        }
    }

    private static ContentAccessor getRepeatElements(CommentWrapper commentWrapper, ContentAccessor greatestCommonParent) {
        ContentAccessor repeatElements = getOrCreateObjectFactory().createP();
        boolean startFound = false;
        for (Object element : greatestCommonParent.getContent()) {
            if (!startFound
                    && depthElementSearch(commentWrapper.getCommentRangeStart(), element)) {
                startFound = true;
            }
            if (startFound) {
                repeatElements.getContent().add(element);
                if (depthElementSearch(commentWrapper.getCommentRangeEnd(), element)) {
                    break;
                }
            }
        }
        return repeatElements;
    }

    private static ContentAccessor findGreatestCommonParent(Object targetSearch, ContentAccessor searchFrom) {
        if (depthElementSearch(targetSearch, searchFrom)) {
            return findInsertableParent(searchFrom);
        }
        return findGreatestCommonParent(targetSearch, (ContentAccessor) ((Child) searchFrom).getParent());
    }

    private static ContentAccessor findInsertableParent(ContentAccessor searchFrom) {
        if (!(searchFrom instanceof Tc || searchFrom instanceof Body)) {
            return findInsertableParent((ContentAccessor) ((Child) searchFrom).getParent());
        }
        return searchFrom;
    }

    private static boolean depthElementSearch(Object searchTarget, Object content) {
        content = XmlUtils.unwrap(content);
        if (searchTarget.equals(content)) {
            return true;
        } else if (content instanceof ContentAccessor) {
            for (Object object : ((ContentAccessor) content).getContent()) {
                Object unwrappedObject = XmlUtils.unwrap(object);
                if (searchTarget.equals(unwrappedObject)
                        || depthElementSearch(searchTarget, unwrappedObject)) {
                    return true;
                }
            }
        }
        return false;
    }
}
