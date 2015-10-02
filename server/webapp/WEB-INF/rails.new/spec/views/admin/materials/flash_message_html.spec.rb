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

describe "admin/materials/hg/new.html.erb" do

  include GoUtil

  before :each do
    assign(:material, @material = HgMaterial.new("url", nil))
    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    ReflectionUtil.setField(@cruise_config, "md5", "abc")
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assign(:config_file_conflict, true)
    in_params(:pipeline_name => "pipeline_name")

    render

    expect(response.body).to have_selector("#config_save_actions button.reload_config#reload_config", :text => "Reload")
    expect(response.body).to have_selector("#config_save_actions label", :text => "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    in_params(:pipeline_name => "pipeline_name")

    render

    expect(response.body).to have_selector("#config_save_actions")
  end
end
