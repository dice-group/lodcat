# LODCat

## How to use

* Place the input `.ttl` files in some directory `your_data_dir`.
* Start the required services: `docker-compose up -d db rabbitmq`.
* Add the input files to the processing queue for the label extraction: `DATA_DIR=your_data_dir make queue-all-files`.
* Start the label extraction process: `make extract` (can run in parallel with the previous step). All extracted labels are added to the database.
* Generate the corpus file: `DATA_DIR=your_data_dir make generate-corpus`. File `corpus/corpus.xml` will be generated.
* Generate the object file: `make generate-object`. File `object/object.gz` will be generated.
* Generate the model file: `make generate-model`. File `model/model.gz` will be generated.
* Generate the labels: `make generate-labels`. Files with labels will be generated in `labels/`.

## Measure quality of topics

Run `make measure-quality`.

## Analysis of how number of topics influences the topic quality

```
./lodcat-quality-number <object file> <output directory> <number of repeats for the same parameters>
./lodcat-quality-number-report <output directory>
```
CSV and corresponding plot will be generated in the specified output directory.
