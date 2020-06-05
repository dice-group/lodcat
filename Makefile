include .env

all: build test

build:
	mvn package
	docker build --tag=lodcat_extractor lodcat.extractor

test:
	docker-compose up -d
	sleep 1
	docker run --rm -i --network lodcat_default --env-file=.env lodcat_extractor <test.ttl

extract:
	find $$DATA_DIR -name '*.ttl.gz' ! -name 'http___w3id_org_squirrel_metadata.ttl.gz' -fprint /dev/stderr -exec sh -c 'zcat "{}" |docker run --rm -i --network lodcat_default --env-file=.env lodcat_extractor' \;

generate-corpus:
	DB_HOST=$(DB_HOST) DB_USER=$(DB_USER) DB_PASSWORD=$(DB_PASSWORD) DB_DB=$(DB_DB) java -Xmx8g -cp lodcat.model/target/lodcat.model.jar org.dice_research.lodcat.model.InitialCorpusGenerator data corpus/corpus.xml

generate-object:
	java -Xmx8g -cp lodcat.model/target/lodcat.model.jar org.dice_research.lodcat.model.CorpusObjectGenerator corpus/corpus.xml object/object.gz

generate-model:
	java -Xmx8g -cp lodcat.model/target/lodcat.model.jar org.dice_research.lodcat.model.ModelGenerator object/object.gz model/model.gz 5
