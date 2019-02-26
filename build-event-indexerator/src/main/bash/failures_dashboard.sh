#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"
START_DATE="2019-01-01"

bq query --location="US" --destination_table="build-analysis:reports.failures_dashboard" --time_partitioning_field="timestamp" --use_legacy_sql="false" --replace --batch "
SELECT
  buildId,
  rootProjectName AS project,
  buildTimestamp AS timestamp,
  wallClockDuration AS build_duration,
  STARTS_WITH(buildAgentId, 'tcagent') AS ci,
  failureData.category AS failure_category,
  failed_task,
  JSON_EXTRACT(env.value,
    '$.name') AS os
FROM
  \`gradle_builds.builds\` builds,
  UNNEST(failureData.taskPaths) AS failed_task
CROSS JOIN
  UNNEST(environmentParameters) AS env
WHERE
  rootProjectName IN (${MONITORED_PROJECTS})
  AND buildTimestamp > '${START_DATE}'
  AND BYTE_LENGTH(failureId) > 0
  AND env.key = 'Os'"
