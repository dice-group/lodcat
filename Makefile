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
