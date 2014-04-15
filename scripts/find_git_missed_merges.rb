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

branch_name = ENV['BRANCH_NAME']
git_cherry_output = `git cherry -v origin/master origin/$BRANCH_NAME`
missed_merges = git_cherry_output.split("/\n/")
if missed_merges.length > 0
  puts "The following revisions were not merged from branch '#{branch_name}' to trunk"
  puts missed_merges
  exit 2
else
  puts "No missed merges found !!"
end

