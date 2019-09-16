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

describe ApiV1::Admin::Authorization::AuthorizationConfigRepresenter do

  before :each do
    @authorization = com.thoughtworks.go.config.Authorization.new(ViewConfig.new(AdminUser.new(CaseInsensitiveString.new('jez'))), OperationConfig.new(AdminUser.new(CaseInsensitiveString.new('tez'))), AdminsConfig.new(AdminRole.new(CaseInsensitiveString.new('foo'))))
  end

  it 'should render authorization with hal reprsentation' do
    presenter = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(@authorization)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(get_authorization_hash)
  end

  it 'should deserialize json to authorization' do
    deserialized_authorization = com.thoughtworks.go.config.Authorization.new
    ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(deserialized_authorization).from_hash(get_authorization_hash)
    expect(deserialized_authorization).to eq(@authorization)
  end

  private
  def get_authorization_hash
    {
      all_group_admins_are_view_users: true,
      admin: {
        roles: ['foo'],
        users: []
      },
      view: {
        roles: [],
        users: ['jez']
      },
      operate: {
        roles: [],
        users: ['tez']
      }
    }
  end
end
