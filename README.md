# LODCat

## How to use

* Place the input `.ttl` files in some directory `datadir`
* Start the required services: `docker-compose up -d db rabbitmq`
* Add the input files to the processing queue for the label extraction: `DATA_DIR=datadir make queue-all-files`
* Start the label extraction process: `make extract` (can run in parallel with the previous step)
