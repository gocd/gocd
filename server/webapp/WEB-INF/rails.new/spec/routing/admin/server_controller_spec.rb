##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

require 'rails_helper'

describe Admin::ServerController do
  describe "index" do
    it "should resolve route to server config" do
      expect({:get => "/admin/config/server"}).to route_to(:controller => "admin/server", :action => "index")
    end
  end

  describe "update" do
    it "should resolve route to server config" do
      expect({:post => "/admin/config/server/update"}).to route_to(:controller => "admin/server", :action => "update")
    end
  end

  describe "validate" do
    it "should resolve /admin/config/server/validate" do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect({:post => "/admin/config/server/validate"}).to route_to(:controller => "admin/server", :action => "validate")
    end
  end

  describe "test_email" do
    it "should resolve admin/config/server/test_email" do
      expect({:post => "/admin/config/server/test_email"}).to route_to(:controller => "admin/server", :action => "test_email")
    end
  end
end
