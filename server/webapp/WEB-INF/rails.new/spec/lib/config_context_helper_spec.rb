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

require 'rails_helper'

def params
  {:pipeline_group => 'pipeline_group_stuff', :plugin_stuff => "other", :other_go_stuff => "also_incl"}
end

describe Admin::ConfigContextHelper do
  include Admin::ConfigContextHelper

  it "should create plugin params" do
    plugin_params = create_config_context(nil).getPluginAttributeMap
    expect(plugin_params[:pipeline_group]).to be_nil
    expect(plugin_params[:plugin_stuff]).to eq("other")
    expect(plugin_params[:other_go_stuff]).to eq("also_incl")
  end
end
