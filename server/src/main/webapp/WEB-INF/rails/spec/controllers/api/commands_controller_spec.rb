#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe Api::CommandsController do
  before :each do
    @command_repository_service = stub_service(:command_repository_service)
  end

  describe "routes" do
    it "should generate path for command snippet cache reload " do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect(:post => '/api/admin/command-repo-cache/reload').to route_to(:no_layout => true, :controller => "api/commands", :action => "reload_cache")
      expect(admin_command_cache_reload_path).to eq("/api/admin/command-repo-cache/reload")
    end
  end

  describe "actions" do
    describe "reload_cache" do
      it "should reload the cache" do
        expect(@command_repository_service).to receive(:reloadCache)

        post :reload_cache, params:{:no_layout => true}

        expect(response.status).to eq(200)
        expect(response.code).to eq("200")
      end
    end
  end
end
