/**
 * This file is part of topicmodeling.preprocessing.
 *
 * topicmodeling.preprocessing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * topicmodeling.preprocessing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with topicmodeling.preprocessing.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dice_research.lodcat.preproc;

import java.util.List;

import org.dice_research.topicmodeling.algorithms.VocabularyContaining;
import org.dice_research.topicmodeling.lang.Term;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractDocumentSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentTextWordIds;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class WordIndexingSupplierDecorator extends
        AbstractDocumentSupplierDecorator implements VocabularyContaining {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordIndexingSupplierDecorator.class);

    protected Vocabulary vocabulary;

    public WordIndexingSupplierDecorator(DocumentSupplier documentSource, Vocabulary vocabulary) {
        super(documentSource);
        this.vocabulary = vocabulary;
    }

    @Override
    protected Document prepareDocument(Document document) {
        TermTokenizedText ttText = document.getProperty(TermTokenizedText.class);
        if (ttText != null) {
            List<Term> terms = ttText.getTermTokenizedText();
            int wordIds[] = prepareWordIds(terms);
            document.addProperty(new DocumentTextWordIds(wordIds));
        } else {
            LOGGER.error("Got a Document object without the needed TermTokenizedText property! Returning null.");
        }
        return document;
    }

    protected abstract int[] prepareWordIds(List<Term> terms);

    @Override
    public Vocabulary getVocabulary() {
        return vocabulary;
    }

}
