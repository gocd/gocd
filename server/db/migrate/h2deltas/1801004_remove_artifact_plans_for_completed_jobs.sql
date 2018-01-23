--
-- Copyright 2017 ThoughtWorks, Inc.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- This query turned out to be time consuming on bigger db instances. Commenting it out as we plan to rethink about how we want to remove data from these tables.

-- DELETE FROM artifactPlans
-- WHERE artifactPlans.id IN (
--   SELECT artifactPlans.id
--   FROM artifactPlans
--   INNER JOIN builds
--     ON builds.id = artifactPlans.buildId
--   WHERE
--       artifactPlans.artifactType = 'file'
--     AND
--       builds.state = 'Completed'
-- );


--//@UNDO

