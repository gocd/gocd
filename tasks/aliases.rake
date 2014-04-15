##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

task "clean" => ["cruise:misc:maven_clean"]
task "dist" => ["no-test", "cruise:pkg:unzip"]
task "cist" => ["clean", "dist"]
task "db" => ["no-test", "cruise:server:db:refresh"]
task "spec" => [ "no-test", "cruise:rails:spec"]
task "spec_server" => ["no-test", "cruise:rails:spec_server"]

task :prepare => ["db", "no-test", "cruise:agent-launcher:prepare", "cruise:test-agent:prepare", "cruise:misc:pull_latest_sass"]

task "no-test" do
  ENV["test"]="no"
end

