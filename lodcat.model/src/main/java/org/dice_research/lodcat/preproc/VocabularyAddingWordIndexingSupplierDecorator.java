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

import org.dice_research.topicmodeling.lang.Term;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VocabularyAddingWordIndexingSupplierDecorator extends WordIndexingSupplierDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyAddingWordIndexingSupplierDecorator.class);

    public VocabularyAddingWordIndexingSupplierDecorator(DocumentSupplier documentSource, Vocabulary vocabulary) {
        super(documentSource, vocabulary);
    }

    @Override
    protected int[] prepareWordIds(List<Term> terms) {
        int wordIds[] = new int[terms.size()];
        int wordId;
        Term term;
        for (int w = 0; w < terms.size(); ++w) {
            term = terms.get(w);
            wordId = vocabulary.getId(term.getLemma());
            if (wordId < 0) {
                vocabulary.add(term.getLemma());
                wordId = vocabulary.getId(term.getLemma());
            }
            wordIds[w] = wordId;
        }
        return wordIds;
    }

}
