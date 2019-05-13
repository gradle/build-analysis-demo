#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"

bq query --location="US" --destination_table="build-analysis:reports.tasks_dashboard" --time_partitioning_field="date" --use_legacy_sql="false" --replace --batch "
SELECT
  DATE(buildTimestamp) AS date,
  rootProjectName AS project,
  CONCAT(tasks.buildPath, ' > ', tasks.path) AS absolute_task_path,
  tasks.className AS task_type,
  tasks.outcome,
  tasks.cacheable,
  CASE
    WHEN tasks.cacheable IS FALSE THEN 'NOT_CACHEABLE'
    WHEN tasks.cacheable IS TRUE
  AND tasks.outcome IN ('from_cache') THEN 'CACHE_HIT'
    WHEN tasks.cacheable IS TRUE AND tasks.outcome IN ('success', 'failed') THEN 'CACHE_MISS'
    WHEN tasks.cacheable IS TRUE
  AND tasks.outcome IN ('up_to_date',
    'skipped',
    'no_source') THEN 'UP_TO_DATE'
    ELSE 'UNKNOWN'
  END AS cache_use,
  STARTS_WITH(buildAgentId, 'tcagent') AS ci,
  SUM(tasks.wallClockDuration) AS total_time_ms,
  AVG(tasks.wallClockDuration) AS avg_duration_ms,
  STDDEV(tasks.wallClockDuration) AS stddev_duration_ms
FROM
  \`gradle_builds.task_executions\`,
  UNNEST(tasks) AS tasks
WHERE
  rootProjectName IN (${MONITORED_PROJECTS})
  AND buildTimestamp > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 60 DAY)
GROUP BY
  1,
  2,
  3,
  4,
  5,
  6,
  7,
  8;"
