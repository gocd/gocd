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

describe Api::ConfigurationController do
  include APIModelMother

  before(:each) do
    allow(controller).to receive(:security_service).and_return(@security_service = double("security_service"))
    allow(controller).to receive(:config_repository).and_return(@config_repository = double("config_repository"))
  end

  describe "config_revisions" do
    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@security_service).to receive(:isUserAdmin).with(loser).and_return(true)
      expect(@config_repository).to receive(:getCommits).with(10, 0).and_return([create_config_revision_model])

      get :config_revisions, params: { :no_layout => true }

      expect(response.body).to eq([ConfigRevisionAPIModel.new(create_config_revision_model)].to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@security_service).to receive(:isUserAdmin).with(loser).and_return(false)

      get :config_revisions, params: { :no_layout => true }

      expect(response.status).to eq(401)
      expect(response.body).to eq("Unauthorized to access this API.\n")
    end
  end

  describe "diff" do
    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@security_service).to receive(:isUserAdmin).with(loser).and_return(true)
      expect(@config_repository).to receive(:configChangesForCommits).with('a', 'b').and_return('text')

      get :config_diff, params: { :from_revision => 'a', :to_revision => 'b', :no_layout => true }

      expect(response.body).to eq('text')
      expect(response.content_type).to eq('text/plain');
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@security_service).to receive(:isUserAdmin).with(loser).and_return(false)

      get :config_diff, params: { :from_revision => 'a', :to_revision => 'b', :no_layout => true }

      expect(response.status).to eq(401)
      expect(response.body).to eq("Unauthorized to access this API.\n")
    end
  end
end
