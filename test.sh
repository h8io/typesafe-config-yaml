#!/bin/bash

set -euxo pipefail

sbt scalafmtSbtCheck scalafmtCheckAll clean test jacoco
