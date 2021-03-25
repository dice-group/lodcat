# LODCat

## How to use

* Place the input `.ttl` files in some directory `your_data_dir`.
* Start the required services: `docker-compose up -d db rabbitmq`.
* Add the input files to the processing queue for the label extraction: `DATA_DIR=your_data_dir make queue-all-files`.
* Start the label extraction process: `make extract` (can run in parallel with the previous step). All extracted labels are added to the database.
* Generate the corpus file: `./lodcat-generate-corpus your_data_dir corpus/corpus.xml`. File `corpus/corpus.xml` will be generated.
* Generate the object file: `make generate-object`. File `object/object.gz` will be generated.
* Generate the model file: `make generate-model`. File `model/model.gz` will be generated.

## Measure quality of topics

Run `make measure-quality`.

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
CSV and corresponding plot will be generated in the specified output directory.

# Classifying documents

```
./lodcat-classify <model directory> <object file with documents to classify> <output classification file>
```

# Generating reports

```
./lodcat-model-report <model directory>
