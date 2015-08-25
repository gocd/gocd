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

describe "admin/pipeline_groups/new.html.erb" do

  include ReflectiveUtil
  include GoUtil

  before(:each) do
    assign(:user, Username.new(CaseInsensitiveString.new("loser")))
    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    set(cruise_config, "md5", "abcd1234")
    view.stub(:pipeline_group_create_path).and_return("pipeline_group_create_path")
  end

  it "should display form to create a new template" do
    assign(:group, BasicPipelineConfigs.new)

    render

    Capybara.string(response.body).find("form[action='pipeline_group_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("label", :text => "Pipeline Group Name*")
      expect(form).to have_selector("input[name='config_md5'][value='abcd1234']")
      expect(form).to have_selector("input[name='group[group]']")
    end
  end
end
