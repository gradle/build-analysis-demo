#!/bin/sh
APP_NAME="build-event-indexerator"
APP_VERSION="0.3.4"
export GCS_RAW_BUCKET_NAME="build-events-raw"
export GCS_TRANSFORMED_BUCKET_NAME="gradle-task-test-cache-events"
export BIGQUERY_DATASET_NAME="gradle_builds"
export BIGQUERY_TABLE_NAME="builds_20190115"
gsutil cp "gs://gradle-build-analysis-apps/maven2/org/gradle/buildeng/analysis/${APP_NAME}/${APP_VERSION}/${APP_NAME}-${APP_VERSION}.zip" .
apt-get update && apt-get -y --force-yes install openjdk-8-jdk unzip
update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
echo "Running ${APP_NAME}-${APP_VERSION}..."
unzip "${APP_NAME}-${APP_VERSION}.zip"
sh "${APP_NAME}-${APP_VERSION}/bin/${APP_NAME}"
echo "Application exited"
