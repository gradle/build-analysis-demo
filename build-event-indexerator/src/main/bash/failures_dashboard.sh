#!/usr/bin/env bash

START_DATE="2019-01-01"

bq query --location="US" --destination_table="build-analysis:reports.failures_dashboard" --time_partitioning_field="timestamp" --use_legacy_sql="false" --replace --batch "
SELECT
  buildId,
  rootProjectName AS project,
  buildTimestamp AS timestamp,
  ARRAY_TO_STRING(failureIds, ' ') AS failureIds,
  failureData.category,
  JSON_EXTRACT(env.value,
    '$.name') AS os,
  (
  SELECT
    failedTaskGoalName
  FROM
    UNNEST(failureData.causes)
  WHERE
    BYTE_LENGTH(failedTaskGoalName) > 0
  LIMIT
    1) AS failedTaskGoalName,
  (
  SELECT
    failedTaskTypeOrMojoClassName
  FROM
    UNNEST(failureData.causes)
  WHERE
    BYTE_LENGTH(failedTaskTypeOrMojoClassName) > 0
  LIMIT
    1) AS failedTaskTypeOrMojoClassName
FROM
  \`gradle_builds.builds\` builds
CROSS JOIN
  UNNEST(environmentParameters) AS env
WHERE
  buildTool = 'Gradle'
  AND buildTimestamp > '${START_DATE}'
  AND BYTE_LENGTH(failureData.category) > 0
  AND ARRAY_LENGTH(failureIds) > 0
  AND env.key = 'Os'"

