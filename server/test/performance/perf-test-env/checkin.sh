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

#!/usr/bin/env bash
FILE_DIR=`dirname $0`
CWD=`(cd $FILE_DIR/perf-hg-repo && pwd)`

if [ ! "$1" ]; then
	echo "Must supply number of checkins to run"
	exit 1
fi

for ((i=0; i < $1; i+=1)); do
	echo "x" >> $CWD/readme.txt
	COUNT=`cat $CWD/readme.txt|wc -l`
	hg ci --cwd $CWD -u "auto" -m "added to readme.txt $COUNT"
	curl -d "pipelineName=perf&stageName=run" http://localhost:8153/go/pipelineStatus.json
	sleep 14 
done
