# LODCat

<img style="height: auto; width: auto; max-width: 150px; max-height: 150px;" src="https://files.dice-research.org/projects/LODCat/LODCat.png">

## How to use

* Place the input `.ttl` files in some directory `your_data_dir`.
* Start the required services: `docker-compose up -d db rabbitmq`.
* Add the input files to the processing queue for the label extraction: `DATA_DIR=your_data_dir make queue-all-files`.
* Start the label extraction process: `make extract` (can run in parallel with the previous step). All extracted labels are added to the database.
* Generate the corpus file: `./lodcat-generate-corpus your_data_dir corpus/corpus.xml`. File `corpus/corpus.xml` will be generated.
* Generate the object file: `./lodcat-generate-object corpus/corpus.xml object/object.gz`. File `object/object.gz` will be generated.
* Generate the model file: `make generate-model`. File `model/model.gz` will be generated.

## Generate a list of redirect article titles from a Wikipedia dump

```
./list-wikidump-redirects <(bzcat *-pages-articles-multistream.xml.bz2) >redirects.txt
```

## Split each XML corpus file in a directory to multiple files

```
./lodcat-part-corpus <input directory> <output directory> <documents per file> <number of parallel jobs>
```

## Preprocess a splitted corpus

```
./lodcat-preproc-wiki-parts <input directory> <output directory> <number of parallel jobs>
```

## Create a corpus object from a splitted corpus

```
./lodcat-generate-wiki-object --input <input directory> --output <output file> [--output-type {object.gz|xml}] [--include-names <file>] [--exclude-names <file>]
```


## Measure quality of topics

Topic quality is measured with [Palmetto](https://github.com/dice-group/Palmetto).

```
./lodcat-measure-quality C_P <model directory>
```

Output: `quality.csv` with quality value for each topic.

## Generating labels

Labels are generated with [NETL](https://github.com/dice-group/NETL-Automatic-Topic-Labelling-).

```
./lodcat-generate-labels <model directory>
```

Output: `labels-supervised.csv`, `labels-unsupervised.csv` with label candidates for each topic.

## Analysis of how number of topics influences the topic quality

```
./lodcat-quality-number <object file> <output directory> <number of repeats for the same parameters> <number of jobs to run in parallel>
./lodcat-quality-number-report <output directory>
```

Output: `micro-quality.csv`, `micro-quality.png`.

CSV and a corresponding plot will be generated in the specified output directory.

# Classifying documents

First, build word count data from corpus files:
```
./lodcat-count-words <model directory> <directory with XML corpus files> <output directory> <number of jobs to run in parallel>
```

Then, run the classifier:
```
./lodcat-classify <model directory> <directory with word count files from the previous step> <output directory> <number of jobs to run in parallel>
```

# Generating reports

```
./lodcat-model-report <model directory>
./lodcat-classification-report <model directory> <classification file> <output directory>
```

# Finding occurrence of namespaces in documents

```
./lodcat-uri-counts corpus/corpus.xml occurrence_dir
./lodcat-uri-counts-report occurrence_dir/documents-per-namespace
```

CSVs and plots will be generated in the specified `occurrence_dir`.

# Analyzing HDT document sizes in triples

```
./lodcat-document-rdf-size <directory with HDT files> <output directory> <number of jobs>
```
