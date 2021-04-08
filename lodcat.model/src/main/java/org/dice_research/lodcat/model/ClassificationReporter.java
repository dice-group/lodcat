package org.dice_research.lodcat.model;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds formatted reports from the classification results.
 */
public class ClassificationReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationReporter.class);

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tClassificationReporter <model directory> <classification csv file> <output directory>");
        }
        File modelDirectory = new File(args[0]);
        if (!modelDirectory.exists() || !modelDirectory.isDirectory()) {
            throw new IllegalArgumentException("The given model directory does not exist.");
        }
        File classificationFile = new File(args[1]);
        if (!classificationFile.exists()) {
            throw new IllegalArgumentException("The given file does not exist.");
        }
        File outputDirectory = new File(args[2]);
        outputDirectory.mkdirs();
        if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("The given output directory does not exist.");
        }

        new ClassificationReporter().run(modelDirectory, classificationFile, outputDirectory);
    }

    public void run(File modelDirectory, File classificationFile, File outputDirectory) throws Exception {
        ClassificationReport report = new ClassificationReport();

        for (CSVRecord record : CSVFormat.DEFAULT.parse(new FileReader(classificationFile))) {
            report.datasets.add(new DatasetInfo(record));
        }

        File topicLabels = new File(modelDirectory, "labels-unsupervised.csv");
        if (topicLabels.exists()) {
            for (CSVRecord record : CSVFormat.DEFAULT.parse(new FileReader(topicLabels))) {
                report.topics.add(new TopicInfo(record));
            }
        }

        XmlMapper xmlMapper = new XmlMapper();
        File xmlOutput = new File(outputDirectory, "classification.xml");
        File htmlOutput = new File(outputDirectory, "classification.html");
        xmlMapper.writer().withRootName("report").writeValue(xmlOutput, report);
        TransformerFactory.newInstance().newTransformer(new StreamSource(getClass().getResourceAsStream("/classification.xsl"))).transform(new StreamSource(xmlOutput), new StreamResult(htmlOutput));
    }

    private class ClassificationReport {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "topic")
        public List<TopicInfo> topics = new ArrayList<>();

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "dataset")
        public List<DatasetInfo> datasets = new ArrayList<>();
    }

    private class TopicInfo {
        @JacksonXmlProperty(isAttribute = true)
        public long id;

        public String label;

        public TopicInfo(CSVRecord record) {
            id = Long.parseLong(record.get(0));
            label = record.get(1);
        }
    }

    private class DatasetInfo {
        @JacksonXmlProperty(isAttribute = true)
        public String id;

        public String url;

        public Integer topicId;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "topicProbability")
        public double[] topicProbabilities;

        public DatasetInfo(CSVRecord record) {
            id = record.get(0);
            url = record.get(1);
            topicId = Integer.parseInt(record.get(2));
            int topicAmount = record.size() - 3;
            topicProbabilities = new double[topicAmount];
            for (int i = 0; i < topicAmount; i++) {
                topicProbabilities[i] = Double.parseDouble(record.get(3 + i));
            }
        }
    }
}
