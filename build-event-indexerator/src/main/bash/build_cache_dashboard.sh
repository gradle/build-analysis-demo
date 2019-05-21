#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"
START_DATE="2019-01-01"

bq query --location="US" --destination_table="build-analysis:reports.build_cache_dashboard" --time_partitioning_field="timestamp" --use_legacy_sql="false" --replace --batch "
SELECT
  DATE(bc.startTimestamp) AS date,
  bc.buildId,
  bc.type,
  COUNT(bc.cacheKey) AS num_failures
FROM
  \`gradle_builds.build_cache_interactions\` bc
WHERE
  BYTE_LENGTH(bc.failureId) > 0
  AND bc.startTimestamp > '${START_DATE}'
GROUP BY
  1,
  2,
  3
ORDER BY
  1 DESC;"
