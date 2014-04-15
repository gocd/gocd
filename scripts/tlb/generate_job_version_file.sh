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

echo "
export GO_PIPELINE_COUNTER='$GO_PIPELINE_COUNTER'
export GO_RERUN_OF_STAGE_COUNTER='$GO_RERUN_OF_STAGE_COUNTER'
export GO_STAGE_COUNTER='$GO_STAGE_COUNTER'
export GO_STAGE_COUNTER='$GO_STAGE_COUNTER'
export TLB_TMP_DIR='$TLB_TMP_DIR'
export TLB_TOTAL_PARTITIONS='$TLB_TOTAL_PARTITIONS'
export TLB_JOB_VERSION='$TLB_JOB_VERSION'
export TLB_BASE_URL='$TLB_BASE_URL'
export TLB_JOB_NAME='$TLB_JOB_NAME'
" > ./job_version_file
