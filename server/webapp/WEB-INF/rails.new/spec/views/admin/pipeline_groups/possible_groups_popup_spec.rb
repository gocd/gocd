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

describe "possible_groups_popup" do
  include FormUI

  it "should render all the groups as a list" do
    view.stub(:move_pipeline_to_group_path).and_return("foo")
    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    set(cruise_config, "md5", "abc")

    render :partial => "admin/pipeline_groups/possible_groups_popup", :locals => {:scope => {:possible_groups => ['group1','group2'], :pipeline_name => 'pipeline1', :md5_match => true}}

    Capybara.string(response.body).find('form').tap do |form|
      form.find("ul") do |ul|
        expect(ul).to have_selector('li', :text => 'group1')
        expect(ul).to have_selector('li', :text => 'group2')
      end
    end
  end

  it "should display conflict message on md5 mismatch" do
    view.stub(:move_pipeline_to_group_path).and_return("foo")
    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    set(cruise_config, "md5", "abc")

    render :partial => "admin/pipeline_groups/possible_groups_popup", :locals => {:scope => {:possible_groups => ['group1','group2'], :pipeline_name => 'pipeline1', :md5_match => false}}

    expect(response.body).to have_selector("p", :text => "Failed to load groups. Configuration file has been modified by someone else. Please reload the page")
  end
end
