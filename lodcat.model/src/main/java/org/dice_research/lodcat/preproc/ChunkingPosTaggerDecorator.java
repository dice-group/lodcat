/**
 * This file is part of topicmodeling.lang.
 *
 * topicmodeling.lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * topicmodeling.lang is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with topicmodeling.lang.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dice_research.lodcat.preproc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dice_research.topicmodeling.lang.Term;
import org.dice_research.topicmodeling.lang.postagging.AbstractPosTagger;
import org.dice_research.topicmodeling.lang.postagging.PosTagger;
import org.dice_research.topicmodeling.lang.postagging.PosTaggingTermFilter;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits the document text into chunks of a specified length,
 * applies the specified PosTagger to chunks
 * and collects the results to a single TermTokenizedText property.
 */
public class ChunkingPosTaggerDecorator extends AbstractPosTagger {

    private static final Logger logger = LoggerFactory.getLogger(ChunkingPosTaggerDecorator.class);
    private static int DEFAULT_CHUNK_SIZE = 1000;

    private PosTagger posTagger;
    private Pattern pattern;

    public ChunkingPosTaggerDecorator(PosTagger posTagger) {
        this(posTagger, DEFAULT_CHUNK_SIZE);
    }

    public ChunkingPosTaggerDecorator(PosTagger posTagger, int chunkSize) {
        this.posTagger = posTagger;
        pattern = Pattern.compile(".{" + chunkSize + "}\\S*\\s", Pattern.DOTALL);
    }

    private void tokenizeChunk(TermTokenizedText acc, String text) {
        acc.addTerms(posTagger.tokenize(text).getTermTokenizedText().toArray(Term[]::new));
    }

    @Override
    protected TermTokenizedText tokenizeText(String text) {
        TermTokenizedText termTokenizedText = new TermTokenizedText();

        Matcher matcher = pattern.matcher(text);
        int end = 0;
        while (matcher.find()) {
            tokenizeChunk(termTokenizedText, matcher.group());
            end = matcher.end();
        }
        tokenizeChunk(termTokenizedText, text.substring(end));

        return termTokenizedText;
    }

    @Override
    protected TermTokenizedText tokenizeText(String text, PosTaggingTermFilter filter) {
        throw new UnsupportedOperationException();
    }
}
