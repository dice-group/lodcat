all: build test

build:
	mvn package
	docker build --tag=lodcat_extractor lodcat.extractor

test:
	docker-compose up -d
	sleep 1
	docker run --rm -i --network lodcat_default --env-file=.env lodcat_extractor <test.ttl
