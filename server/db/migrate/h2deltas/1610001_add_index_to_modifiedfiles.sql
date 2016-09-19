--
-- Copyright 2016 ThoughtWorks, Inc.
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

-- H2 automatically creates indices on primary keys and foreign keys,
-- while postgres only creates indices on primary keys.
-- A migration was added to create the missing index on a foreign key in postgres.
-- To maintain sanity, and avoid mismatch of the migrations we have on h2 and postgres, this migration is added.

SELECT 1 + 1;

--//@UNDO

SELECT 1 + 1;
