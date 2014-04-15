#!/usr/bin/env bash
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

unzip ../cruise-server-1.0.zip
unzip ../cruise-agent-1.0.zip
cp cruise-config.xml cruise-server-1.0/
rm -Rf cruise-server-1.0/pipelines/
mkdir cruise-server-1.0/pipelines
rm -Rf cruise-agent-1.0/pipelines/
mkdir cruise-agent-1.0/pipelines
