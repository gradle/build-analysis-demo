#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"

bq query --location="US" --destination_table="build-analysis:reports.builds_dashboard" --time_partitioning_field="date" --use_legacy_sql="false" --replace --batch "
SELECT
  DATE(buildTimestamp) AS date,
  rootProjectName AS project,
  buildId as build_id,
  STARTS_WITH(buildAgentId, 'tcagent') AS ci,
  SUM(wallClockDuration) AS total_build_time_ms
FROM
  \`gradle_builds.builds\`
WHERE
  rootProjectName IN (${MONITORED_PROJECTS})
  AND buildTimestamp > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 60 DAY)
GROUP BY
  1,
  2,
  3,
  4;"
