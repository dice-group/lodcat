include .env

all: build test

build:
	mvn --batch-mode package
	docker build --tag=lodcat_extractor lodcat.extractor

test: test-extractor
	mvn test

test-extractor:
	docker-compose up -d
	sleep 1
	docker run --rm -i --network lodcat_default --env-file=.env lodcat_extractor <test.ttl
	docker-compose down --remove-orphans

queue-all-files:
	docker exec lodcat_rabbitmq rabbitmqadmin declare queue name=file durable=true
	find "$$DATA_DIR" -name '*.ttl' |docker exec -i lodcat_rabbitmq xargs -n 1 -I {} rabbitmqadmin publish routing_key=file payload="{}"

extract:
	DB_HOST=$(DB_HOST) DB_USER=$(DB_USER) DB_PASSWORD=$(DB_PASSWORD) DB_DB=$(DB_DB) ./extract_wrapper

generate-corpus:
	DB_HOST=$(DB_HOST) DB_USER=$(DB_USER) DB_PASSWORD=$(DB_PASSWORD) DB_DB=$(DB_DB) java -Xmx8g -cp lodcat.model/target/lodcat.model.jar org.dice_research.lodcat.model.InitialCorpusGenerator "$$DATA_DIR" corpus/corpus.xml

generate-object:
	java -Xmx8g -cp lodcat.model/target/lodcat.model.jar org.dice_research.lodcat.model.CorpusObjectGenerator corpus/corpus.xml object/object.gz

generate-model:
	java -Xmx8g -cp lodcat.model/target/lodcat.model.jar org.dice_research.lodcat.model.ModelGenerator object/object.gz model/model.gz 5

generate-labels:
	./topwords4labelling <model/top_words.csv >model/top_words.labelling
	docker run \
	-v `pwd`/model/top_words.labelling:/data/topics.csv \
	--name netl dicegroup/netl
	mkdir -p labels
	docker cp netl:/usr/src/app/model_run/output_unsupervised labels/unsupervised
	docker cp netl:/usr/src/app/model_run/output_supervised labels/supervised
	docker rm netl

measure-quality: palmetto-0.1.0.jar
	./topwords4palmetto <model/top_words.csv >model/top_words.palmetto
	java -jar palmetto-0.1.0.jar $$HOME/.local/share/palmetto/indexes/wikipedia_bd C_P model/top_words.palmetto

palmetto-0.1.0.jar:
	wget -O $@ https://hobbitdata.informatik.uni-leipzig.de/homes/mroeder/palmetto/palmetto-0.1.0-jar-with-dependencies.jar
