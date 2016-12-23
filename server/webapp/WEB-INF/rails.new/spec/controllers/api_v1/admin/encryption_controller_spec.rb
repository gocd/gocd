##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApiV1::Admin::EncryptionController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  describe :security do
    it 'should allow anyone, with security disabled' do
      disable_security
      expect(controller).to allow_action(:post, :encrypt_value)
    end

    it 'should disallow anonymous users, with security enabled' do
      enable_security
      login_as_anonymous
      expect(controller).to disallow_action(:post, :encrypt_value).with(401, 'You are not authorized to perform this action.')
    end

    it 'should disallow normal users, with security enabled' do
      login_as_user
      expect(controller).to disallow_action(:post, :encrypt_value).with(401, 'You are not authorized to perform this action.')
    end

    it 'should allow admin users, with security enabled' do
      login_as_admin
      expect(controller).to allow_action(:post, :encrypt_value)
    end

    it 'should allow pipeline group admin users, with security enabled' do
      login_as_group_admin
      expect(controller).to allow_action(:post, :encrypt_value)
    end

    it 'should allow template admin users, with security enabled' do
      login_as_template_admin
      expect(controller).to allow_action(:post, :encrypt_value)
    end
  end

  describe :route do
    describe :with_header do
      it 'should route to encrypt_value action of encryption controller' do
        expect(:post => 'api/admin/encrypt').to route_to(action: 'encrypt_value', controller: 'api_v1/admin/encryption')
      end

    end
    describe :without_header do
      before :each do
        teardown_header
      end
      it 'should not route to index action of packages controller without header' do
        expect(:post => 'api/admin/encrypt').to_not route_to(action: 'encrypt_value', controller: 'api_v1/admin/encryption')
        expect(:post => 'api/admin/encrypt').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/encrypt')
      end
    end
  end

  describe :encryption do
    it 'should return the encrypted value of the plain text passed' do
      login_as_admin
      post_with_api_header :encrypt_value, {value: 'foo'}

      expect(response).to be_ok
      expected_response(GoCipher.new.encrypt('foo'), ApiV1::EncryptedValueRepresenter)
    end
  end
end