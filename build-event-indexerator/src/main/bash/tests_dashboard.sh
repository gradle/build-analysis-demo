#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"

bq query --location="US" --destination_table="build-analysis:reports.tests_dashboard" --time_partitioning_field="date" --use_legacy_sql="false" --replace --batch "
SELECT
  DATE(buildTimestamp) AS date,
  rootProjectName as project,
  CONCAT(t.className, '.', t.name) AS test_name,
  t.taskId as task_path,
  exec.failed AS failed,
  STARTS_WITH(buildAgentId, 'tcagent') AS ci,
  SUM(exec.wallClockDuration) AS total_time_ms,
  AVG(exec.wallClockDuration) AS avg_duration,
  STDDEV(exec.wallClockDuration) stddev_duration
FROM
  \`gradle_builds.test_executions\`,
  UNNEST(tests) AS t,
  UNNEST(t.executions) AS exec
WHERE
  rootProjectName IN (${MONITORED_PROJECTS})
  AND buildTimestamp > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 60 DAY)
  AND t.suite = FALSE
GROUP BY
  1,
  2,
  3,
  4,
  5,
  6;"
