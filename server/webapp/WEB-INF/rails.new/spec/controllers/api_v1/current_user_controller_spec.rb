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

describe ApiV1::CurrentUserController do
  include ApiHeaderSetupTeardown, ApiV4::ApiVersionHelper

  before(:each) do
    login_as_user
    @user_obj = User.new(@user.username.to_s, 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

    controller.stub(:user_service).and_return(@user_service = double('user-service'))
    @user_service.stub(:findUserByName).and_return(@user_obj)
  end

  describe :show do
    it("returns a JSON representation of the user") do
      get_with_api_header :show
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(@user_obj, ApiV1::UserRepresenter))
    end
  end

  describe :update do
    it("allows updating user and returns a JSON representation of the user") do
      @user_service.should_receive(:save).with(@user_obj, TriState.TRUE, TriState.FALSE, 'foo@example.com', 'foo, bar', an_instance_of(HttpLocalizedOperationResult)).and_return(@user_obj)

      patch_with_api_header :update, login_name: @user_obj.name, enabled: true, email_me: false, email: 'foo@example.com', checkin_aliases: 'foo, bar'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(@user_obj, ApiV1::UserRepresenter))
    end
  end

end
