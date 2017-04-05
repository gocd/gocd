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

require 'spec_helper'

describe ApiV1::NotificationFiltersController do
  include ApiHeaderSetupTeardown, ApiV4::ApiVersionHelper

  before(:each) do
    login_as_user
    controller.stub(:user_service).and_return(@user_service = double('user-service'))
    @user_service.stub(:findUserByName).and_return(@user = double('user'))
    @user.stub(:id).and_return(1000)
    @user_service.stub(:load).with(@user.id).and_return(@user)
  end

  describe :index do
    it("returns a list of filters serialized to JSON") do
      @user.stub(:notification_filters).and_return([
        filter_for("pipeline1", "defaultStage", "Fails", true, 1),
        filter_for("[Any Pipeline]", "[Any Stage]", "Breaks", false, 2),
      ])

      get_with_api_header(:index)
      expected = [
        {"pipelineName" => "pipeline1", "stageName" => "defaultStage", "event" => "Fails", "id" => 1, "myCheckin" => true},
        {"pipelineName" => "[Any Pipeline]", "stageName" => "[Any Stage]", "event" => "Breaks", "id" => 2, "myCheckin" => false}
      ].sort_by {|h| h["id"]}

      assert_equal 200, response.status
      assert_equal expected, JSON.parse(response.body).sort_by {|h| h["id"]}
    end
  end

  describe :create do
    it("creates a filter to match any commit") do
      @user.stub(:notification_filters).and_return([]) # not verifying this
      @user_service.should_receive(:add_notification_filter).with(@user.id, filter_for("foo", "bar", "Breaks", false))

      post_with_api_header(:create, pipeline: "foo", stage: "bar", event: "Breaks")

      assert_equal 200, response.status
    end

    it("creates a filter to match a user's own commits") do
      @user.stub(:notification_filters).and_return([]) # not verifying this
      @user_service.should_receive(:add_notification_filter).with(@user.id, filter_for("foo", "bar", "Breaks", true))

      post_with_api_header(:create, pipeline: "foo", stage: "bar", event: "Breaks", myCheckin: "on")

      assert_equal 200, response.status
    end

    it("validates input") do
      @user.stub(:notification_filters).and_return([]) # not verifying this

      post_with_api_header(:create, pipeline: "foo", event: "Breaks", myCheckin: "on")

      assert_equal 400, response.status
      assert_equal "You must specify pipeline, stage, and event.", JSON.parse(response.body)["message"]
    end
  end

  describe :destroy do
    it("returns destroys filter") do
      @user.stub(:notification_filters).and_return([]) # really don't care
      @user_service.should_receive(:remove_notification_filter).with(@user.id, 5)

      delete_with_api_header(:destroy, id: "5")
    end
  end

  private

  def filter_for(pipeline, stage, event, own_commits, stubbed_id=nil)
    NotificationFilter.new(pipeline, stage, StageEvent.valueOf(event), own_commits).tap {|f| f.setId(stubbed_id) unless stubbed_id.nil?}
  end
end
