#!/bin/bash

set -euxo pipefail

sbt "scalafmtSbtCheck; scalafmtCheckAll; clean; javafmtCheckAll; doc; jacoco"
