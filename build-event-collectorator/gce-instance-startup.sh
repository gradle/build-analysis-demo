#!/bin/sh
APP_NAME="build-event-collectorator"
APP_VERSION="0.4.1"
export GRADLE_ENTERPRISE_HOSTNAME="e.grdev.net"
export GRADLE_ENTERPRISE_USERNAME="eric-export"
export GRADLE_ENTERPRISE_PASSWORD="HexuNZyvUvvWUKV[T2FqRt3F;Bq6fq"
export BACKFILL_DAYS=30
export LAST_BUILD_ID="2aucincywa5oo"
export GCS_RAW_BUCKET_NAME="build-events-raw"
gsutil cp "gs://gradle-build-analysis-apps/maven2/org/gradle/buildeng/analysis/${APP_NAME}/${APP_VERSION}/${APP_NAME}-${APP_VERSION}.zip" .
apt-get update
apt-get -y --force-yes install openjdk-8-jdk
update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
apt-get -y --force-yes install unzip
echo "Running ${APP_NAME}-${APP_VERSION}..."
unzip "${APP_NAME}-${APP_VERSION}.zip"
sh "${APP_NAME}-${APP_VERSION}/bin/${APP_NAME}"
echo "Application exited"

