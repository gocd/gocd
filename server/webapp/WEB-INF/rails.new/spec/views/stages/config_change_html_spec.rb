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

require 'spec_helper'

describe 'stages/config_change.html.erb' do

  it "should display config changes" do
    assign :changes, "changes_string"
    render
    expect(response).to have_selector(".config-changes", :text => "changes_string")
  end

  it "should tag each line of config changes with appropriate css class" do
    assign(:changes, "@@ -23,9 +23,10 @@\n       <stage name='up42_stage'>\n         <jobs>\n-          <job name='up42_job'>\n+          <job name='job'>")

    render
    Capybara.string(response.body).find(".config-changes").tap do |f|
      expect(f.find("tr.line-info td pre").text).to include "@@ -23,9 +23,10 @@"
      f.all("tr.context td pre").tap do |pre|
        expect(pre[0].text).to include "<stage name='up42_stage'>"
        expect(pre[1].text).to include "<jobs>"
      end
      expect(f.find("tr.remove td pre").text).to include "<job name='up42_job'>"
      expect(f.find("tr.add td pre").text).to include "<job name='job'>"
    end
  end

  it "should display error message if available" do
    assign(:config_change_error_message, "Error message")
    render
    Capybara.string(response.body).find(".config-changes").tap do |f|
      expect(f).to have_selector("tr.information td", :text => "Error message")
    end
  end
end
