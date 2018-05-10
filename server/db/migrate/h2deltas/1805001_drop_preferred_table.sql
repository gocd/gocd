--
-- Copyright 2018 ThoughtWorks, Inc.
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

-- See `230006_make_user_name_case_insensitive.sql`
-- This was a temp table created to make users' login-name case-insensitive
DROP TABLE preffered;

--//@UNDO

CREATE TABLE preffered AS SELECT MIN(id) id, in_name FROM users WHERE preffered_id IS NULL GROUP BY in_name;
