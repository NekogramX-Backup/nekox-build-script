#!/usr/bin/env bash

cd /github/nekox-build-script || exit

export JAVA_HOME=$_JAVA_HOME

mvn exec:java -Dexec.mainClass="nekox.BuildScript"