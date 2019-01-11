#!/usr/bin/env bash

DEPLOY_LOCATION="gs://gradle-build-analysis-apps/"
APPS_VERSION="0.1.0"

# *-erator projects are runnable applications by convention
for project in $(find . -type d -name \*rator | grep -v "\.idea"); do
    gsutil cp ${project}/build/distributions/${project}-${APPS_VERSION}.zip ${DEPLOY_LOCATION};
done

