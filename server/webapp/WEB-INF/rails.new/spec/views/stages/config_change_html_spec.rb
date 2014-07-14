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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")

describe 'stages/_config_change.erb' do

  it "should display config changes" do
    assigns[:changes] = "changes_string"
    render 'stages/config_change.html'
    response.body.should have_tag(".config-changes", "changes_string")
  end

  it "should tag each line of config changes with appropriate css class" do
    assigns[:changes] = "@@ -23,9 +23,10 @@\n       <stage name='up42_stage'>\n         <jobs>\n-          <job name='up42_job'>\n+          <job name='job'>"

    render 'stages/config_change.html'
    response.body.should have_tag(".config-changes") do
       with_tag("tr.line-info td pre", "@@ -23,9 +23,10 @@")
       with_tag("tr.context td pre",   "       &lt;stage name='up42_stage'&gt;")
       with_tag("tr.context td pre",   "         &lt;jobs&gt;")
       with_tag("tr.remove td pre",    "-          &lt;job name='up42_job'&gt;")
       with_tag("tr.add td pre",       "+          &lt;job name='job'&gt;")
    end
  end

  it "should display error message if available" do
    assigns[:config_change_error_message] = "Error message"
    render 'stages/config_change.html'
    response.body.should have_tag(".config-changes") do
        with_tag("tr.information td", "Error message")
    end
  end
end