#!/bin/bash

set -euxo pipefail

sbt scalafmtSbtCheck scalafmtCheckAll javafmtCheckAll clean doc jacoco
