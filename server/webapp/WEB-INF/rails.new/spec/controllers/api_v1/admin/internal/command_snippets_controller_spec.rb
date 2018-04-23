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

require 'rails_helper'

describe ApiV1::Admin::Internal::CommandSnippetsController do

  include ApiV1::ApiVersionHelper

  before :each do
    allow(controller).to receive(:command_repository_service).and_return(@command_repository_service = double('command_repository_service'))
  end

  describe "index" do
    describe "authorization" do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end
    end

    describe 'as admin' do
      it 'should fetch all command snippets filtered by prefix' do
        enable_security
        login_as_admin
        snippet = com.thoughtworks.go.helper.CommandSnippetMother.validSnippet("scp")
        presenter   = ApiV1::CommandSnippetsRepresenter.new([snippet])
        snippet_hash = presenter.to_hash(url_builder: controller, prefix: 'rake')

        expect(@command_repository_service).to receive(:lookupCommand).with('rake').and_return([snippet])

        get_with_api_header :index, params: { prefix: 'rake' }

        expect(response).to be_ok
        expect(actual_response).to eq(JSON.parse(snippet_hash.to_json).deep_symbolize_keys)
      end
    end
  end
end