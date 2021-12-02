package org.dice_research.lodcat.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.dice_research.topicmodeling.io.CorpusWriter;
import org.dice_research.topicmodeling.io.gensim.VocabularyTSVReader;
import org.dice_research.topicmodeling.io.gensim.stream.StreamBasedMMDocumentSupplier;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusWriterDecorator;
import org.dice_research.topicmodeling.io.java.CorpusObjectWriter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.corpus.DocumentListCorpus;
import org.dice_research.topicmodeling.utils.corpus.properties.CorpusVocabulary;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;

public class CorpusTransformerGensim2JavaObject {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "CorpusTransformerGensim2JavaObject <input-matrix-file> <input-vocab-file> <output-corpus-file>");
            return;
        }
        File inputMatrixFile = new File(args[0]);
        File inputVocabFile = new File(args[1]);
        File outputCorpusFile = new File(args[2]);

        System.out.print("Reading vocabulary...");
        VocabularyTSVReader vocabReader = new VocabularyTSVReader(1, 0);
        // skip the first two lines
        vocabReader.setHeaderLines(1);
        Vocabulary vocabulary = vocabReader.readVocabulary(inputVocabFile);
        System.out.print("Found ");
        System.out.print(vocabulary.size());
        System.out.println(" words.");

        System.out.print("Reading corpus...");
        Corpus corpus = new DocumentListCorpus<List<Document>>(new ArrayList<>());
        try (Reader reader = new FileReader(inputMatrixFile)) {
            StreamBasedMMDocumentSupplier supplier = StreamBasedMMDocumentSupplier.createReader(reader, 0, 1, 2, false);
            supplier.setHeaderLines(2);
            Document document = supplier.getNextDocument();
            while (document != null) {
                corpus.addDocument(document);
                if ((corpus.getNumberOfDocuments() % 10000) == 0) {
                    System.out.print("Saw ");
                    System.out.print(corpus.getNumberOfDocuments());
                    System.out.println(" documents.");
                }
                document = supplier.getNextDocument();
            }
        }
        System.out.print("Found ");
        System.out.print(corpus.getNumberOfDocuments());
        System.out.println(" documents.");

        corpus.addProperty(new CorpusVocabulary(vocabulary));

        System.out.print("Writing corpus...");
        CorpusWriter writer = new GZipCorpusWriterDecorator(new CorpusObjectWriter());
        writer.writeCorpus(corpus, outputCorpusFile);
        System.out.print("Finished.");
    }
}
