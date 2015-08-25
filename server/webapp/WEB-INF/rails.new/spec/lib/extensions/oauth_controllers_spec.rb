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

describe Oauth2Provider::ClientsController, type: :controller do
  it "should set the tab_name" do
    controller.send(:set_tab_name)
    controller.instance_variable_get("@tab_name").should == "oauth-clients"
  end

  it "should use set tab as a before filter" do
    expect(controller.class._process_action_callbacks.map(&:filter)).to include(:set_tab_name)
  end

  it "should set the view_title" do
    controller.send(:set_view_title)
    controller.instance_variable_get("@view_title").should == "Administration"
  end

  it "should use set the view title as a before filter" do
    expect(controller.class._process_action_callbacks.map(&:filter)).to include(:set_view_title)
  end
end

describe Oauth2Provider::UserTokensController, type: :controller do
  it "should use set tab as a before filter" do
    expect(controller.class._process_action_callbacks.map(&:filter)).to include(:set_tab_name)
  end

  it "should set the tab_name" do
    controller.send(:set_tab_name)
    expect(controller.instance_variable_get("@current_tab_name")).to eq("preferences")
  end
end
