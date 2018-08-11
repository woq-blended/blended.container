.PHONY: help # List of targets with descriptions
help:
	@grep '^\.PHONY: .* #' Makefile | sed 's/\.PHONY: \(.*\) # \(.*\)/\1\t\2/' | expand -t20

.PHONY: container # Build all development project (not itest and docker)
container:
	mvn install

.PHONY: clean # Run mvn clean
clean:
	mvn -Pitest,docker --fail-at-end clean

.PHONY: pom-xml # Generate pom.xml files
pom-xml:
	mvn -Pitest,gen-pom-xml initialize

.PHONY: eclipse # Generate Eclipse project files
eclipse: pom-xml
	mvn -Peclipse,build,itest initialize de.tototec:de.tobiasroeser.eclipse-maven-plugin:0.1.1:eclipse

.PHONY: full # A full build including docker tests
full: docker-clean
	mvn -Pitest,docker install

.PHONY: docker-clean # Cleanup old images from docker registry
docker-clean:
	for vm in $$(docker ps -aq); do \
		docker rm -f $$vm; \
	done
	for image in $$(docker images | grep none | awk '{print $$3;}'); \
		do docker rmi -f $$image; \
	done

