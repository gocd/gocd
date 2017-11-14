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

describe ApiV1::NotificationFiltersController do

  include ApiV4::ApiVersionHelper

  before(:each) do
    login_as_user
    allow(controller).to receive(:user_service).and_return(@user_service = double('user-service'))
    allow(@user_service).to receive(:findUserByName).and_return(@user = double('user'))
    allow(@user).to receive(:id).and_return(1000)
    allow(@user_service).to receive(:load).with(@user.id).and_return(@user)
  end

  describe "index" do
    it("returns a list of filters serialized to JSON") do
      allow(@user).to receive(:notificationFilters).and_return([
        filter_for("pipeline1", "defaultStage", "Fails", true, 1),
        filter_for("[Any Pipeline]", "[Any Stage]", "Breaks", false, 2),
      ])

      get_with_api_header(:index)
      expected = [
        {"pipeline" => "pipeline1", "stage" => "defaultStage", "event" => "Fails", "id" => 1, "match_commits" => true},
        {"pipeline" => "[Any Pipeline]", "stage" => "[Any Stage]", "event" => "Breaks", "id" => 2, "match_commits" => false}
      ].sort_by {|h| h["id"]}

      assert_equal 200, response.status
      assert_equal expected, JSON.parse(response.body)["filters"].sort_by {|h| h["id"]}
    end
  end

  describe "create" do
    it("creates a filter to match any commit") do
      allow(@user).to receive(:notificationFilters).and_return([]) # not verifying this
      expect(@user_service).to receive(:addNotificationFilter).with(@user.id, filter_for("foo", "bar", "Breaks", false))

      post_with_api_header(:create, params: {pipeline: "foo", stage: "bar", event: "Breaks"})

      assert_equal 200, response.status
    end

    it("creates a filter to match a user's own commits") do
      allow(@user).to receive(:notificationFilters).and_return([]) # not verifying this
      expect(@user_service).to receive(:addNotificationFilter).with(@user.id, filter_for("foo", "bar", "Breaks", true))

      post_with_api_header(:create, params: {pipeline: "foo", stage: "bar", event: "Breaks", match_commits: true})

      assert_equal 200, response.status
    end

    it("validates input") do
      allow(@user).to receive(:notificationFilters).and_return([]) # not verifying this

      post_with_api_header(:create, params: {pipeline: "foo", event: "Breaks", match_commits: true})

      assert_equal 400, response.status
      assert_equal "You must specify pipeline, stage, and event.", JSON.parse(response.body)["message"]
    end
  end

  describe "destroy" do
    it("destroys a filter") do
      allow(@user).to receive(:notificationFilters).and_return([]) # really don't care
      expect(@user_service).to receive(:removeNotificationFilter).with(@user.id, 5)

      delete_with_api_header(:destroy, params: {id: "5"})
      assert_equal 200, response.status
    end
  end

  private

  def filter_for(pipeline, stage, event, own_commits, stubbed_id=nil)
    NotificationFilter.new(pipeline, stage, StageEvent.valueOf(event), own_commits).tap {|f| f.setId(stubbed_id) unless stubbed_id.nil?}
  end
end
