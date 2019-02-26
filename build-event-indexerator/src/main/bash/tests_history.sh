#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"

bq query --location="US" --destination_table="build-analysis:reports.tests_history" --time_partitioning_field="timestamp" --use_legacy_sql="false" --replace --batch "
SELECT
  buildId as build_id,
  rootProjectName AS project,
  STARTS_WITH(buildAgentId, 'tcagent') AS ci,
  CONCAT(t.className, '.', t.name) AS test_name,
  CASE
    WHEN exec.skipped = TRUE THEN 'SKIPPED'
    WHEN exec.failed = TRUE THEN 'FAILED'
    ELSE 'SUCCEEDED'
  END AS outcome,
  exec.startTimestamp AS timestamp,
  exec.wallClockDuration AS duration_ms
FROM
  \`gradle_builds.test_executions\`,
  UNNEST(tests) AS t,
  UNNEST(t.executions) AS exec
WHERE
  exec.startTimestamp > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
  AND rootProjectName IN (${MONITORED_PROJECTS})
  AND t.suite = FALSE;"
