include .makerc

HDT_FILES := $(addprefix lodcat.extractor/src/test/resources/, \
simple.hdt \
lang.hdt \
lang2.hdt \
quotes.hdt \
blanknode.hdt \
) $(addprefix lodcat.model/src/test/resources/, \
counts.hdt \
)

build:
	mvn --batch-mode --quiet -DskipTests package
	#docker build --tag=lodcat_extractor lodcat.extractor

test: $(HDT_FILES)
	mvn --quiet test

test-extractor:
	docker-compose up -d
	sleep 1
	docker run --rm -i --network lodcat_default --env-file=.env lodcat_extractor <test.ttl
	docker-compose down --remove-orphans

queue-all-files:
	docker exec lodcat_rabbitmq rabbitmqadmin declare queue name=file durable=true
	find "$$DATA_DIR" -type f |docker exec -i lodcat_rabbitmq xargs -n 1 -I {} rabbitmqadmin publish routing_key=file payload="{}"

extract:
	DB_HOST=$(DB_HOST) DB_USER=$(DB_USER) DB_PASSWORD=$(DB_PASSWORD) DB_DB=$(DB_DB) ./extract_wrapper

generate-corpus: lodcat.model/target/lodcat.model.jar
	DB_HOST=$(DB_HOST) DB_USER=$(DB_USER) DB_PASSWORD=$(DB_PASSWORD) DB_DB=$(DB_DB) $(CORPUS_GENERATOR) "$$DATA_DIR" corpus/corpus.xml

generate-object:
	$(OBJECT_GENERATOR) corpus/corpus.xml object/object.gz

generate-enwiki-object: enwiki.xml.bz2
	$(OBJECT_GENERATOR) $< object/enwiki.gz

generate-simplewiki-object: simplewiki.xml.bz2
	$(OBJECT_GENERATOR) $< object/simplewiki.gz

generate-model:
	$(MODEL_GENERATOR) object/object.gz model/model.gz 5

generate-enwiki-model:
	$(MODEL_GENERATOR) object/enwiki.gz model/enwiki.gz 5

generate-simplewiki-model:
	$(MODEL_GENERATOR) object/simplewiki.gz model/simplewiki.gz 5

generate-labels:
	./topwords4labelling <model/top_words.csv >model/top_words.labelling
	docker run \
	-v `pwd`/model/top_words.labelling:/data/topics.csv \
	--name netl dicegroup/netl
	mkdir -p labels
	docker cp netl:/usr/src/app/model_run/output_unsupervised labels/unsupervised
	docker cp netl:/usr/src/app/model_run/output_supervised labels/supervised
	docker rm netl

measure-quality: palmetto
	./lodcat-measure-quality C_P model

palmetto: $(PALMETTO_JAR) palmetto-indexes

%/target/%.jar:
	mvn --projects $* package

$(PALMETTO_JAR):
	wget -q -O $@ https://hobbitdata.informatik.uni-leipzig.de/homes/mroeder/palmetto/palmetto-0.1.0-jar-with-dependencies.jar

palmetto-indexes:
	[ -e $$HOME/.local/share/palmetto/indexes/wikipedia_bd ] \
	|| (mkdir -p $$HOME/.local/share/palmetto/indexes \
	&& (wget -q -O - "https://hobbitdata.informatik.uni-leipzig.de/homes/mroeder/palmetto/Wikipedia_bd.zip" |busybox unzip - -d $$HOME/.local/share/palmetto/indexes))

%.hdt: %.ttl
	rdf2hdt -rdftype turtle $< $@

%wiki.xml.bz2:
	wget -O $@ https://dumps.wikimedia.org/$*wiki/20210101/$*wiki-20210101-pages-articles-multistream.xml.bz2
