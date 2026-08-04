#!/bin/bash

set -euxo pipefail

# -debug -v came from the workflow stub, which used to pass `debug: true` and `verbose: true` to shared CI. Those
# inputs are gone in gha v6, since the command they decorated no longer lives there; drop the flags here if they
# outlived the troubleshooting they were added for.
sbt -debug -v "cleanFull; +test; ci-release"
