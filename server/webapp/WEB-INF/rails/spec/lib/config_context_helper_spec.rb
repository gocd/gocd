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

require File.join(File.dirname(__FILE__), "..", "spec_helper")

def params
  {:pipeline_group => 'pipeline_gropu_stuff', :plugin_stuff => "other", :other_go_stuff => "also_incl"}
end

describe Admin::ConfigContextHelper do
  include Admin::ConfigContextHelper


  it "should create plugin params" do
    plugin_params = create_config_context(nil).getPluginAttributeMap
    plugin_params[:pipeline_group].should == nil
    plugin_params[:plugin_stuff].should == "other"
    plugin_params[:other_go_stuff].should == "also_incl"
    end

end