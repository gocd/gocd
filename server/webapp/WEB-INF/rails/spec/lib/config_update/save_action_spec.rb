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

describe ConfigUpdate::SaveAction do

  it "should create a valid object" do
    params = {:a => :b}
    security_service = Object.new
    save_action = ::ConfigUpdate::SaveAction.new(params, "loser", security_service)

    save_action.params.should == params
    save_action.instance_variable_get("@user").should == "loser"
    save_action.instance_variable_get("@security_service").should == security_service
  end

  it "should create ConfigUpdate command" do
    save_action = ::ConfigUpdate::SaveAction.new(nil, nil, nil)
    save_action.java_kind_of?(com.thoughtworks.go.config.update.UpdateConfigFromUI).should be_true
  end
end