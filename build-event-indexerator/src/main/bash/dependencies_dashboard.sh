#!/usr/bin/env bash

MONITORED_PROJECTS="'gradle','dotcom','dotcom-docs','gradle-kotlin-dsl','ci-health','build-analysis','gradle-profiler','gradle-site-plugin','gradlehub'"

bq query --location="US" --destination_table="build-analysis:reports.dependencies_dashboard" --use_legacy_sql="false" --replace --batch "
SELECT
  DISTINCT(CONCAT(md.group, ':', md.module)) AS group_and_module,
  rootProjectName AS project_name,
  md.version,
  COUNT(buildId) build_count
FROM
  `gradle_builds.dependencies` AS d,
  UNNEST(moduleDependencies) AS md
WHERE
  rootProjectName IN (${MONITORED_PROJECTS})
  AND buildTimestamp > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 60 DAY)
GROUP BY
  1,
  2,
  3;"
