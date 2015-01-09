#*************************GO-LICENSE-START********************************
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*************************GO-LICENSE-END**********************************

#!/bin/sh
source ./job_version_file

if [ "x$GO_RERUN_OF_STAGE_COUNTER" = 'x' ]; then
    unset GO_RERUN_OF_STAGE_COUNTER
fi #because if we do not override it, it gets priority over stage-counter when TLB resolves job-version

echo *****************************
echo "Dumping environment variables for reference: "
env
echo *****************************

./bn cruise:misc:assert_all_partitions_executed $*
