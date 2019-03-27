.PHONY: help # List of targets with descriptions
help:
	@grep '^.PHONY: .* #' Makefile | sed 's/\.PHONY: \(.*\) # \(.*\)/\1\t\2/' | expand -t20

.PHONY: run # Run the container
run:
	rlwrap sh target/blended.demo.*-*-SNAPSHOT/bin/blended.sh

.PHONY: build # Clean build the project
build:
	mvn clean install

.PHONY: show-log # Open the container logfile
show-log:
	$${PAGER} -i target/blended.demo.*-*-SNAPSHOT/log/blended.log

.PHONY: unpack-sbt # Unpack the container which was build with sbt
unpack-sbt:
	tar xzf target/blended.demo.*-*-SNAPSHOT-full-nojre.tar.gz -C target/

.PHONY: clean-container # Remove an older instance of the unpacked container
clean-container:
	-rm -r target/blended.demo.*-*-SNAPSHOT/

