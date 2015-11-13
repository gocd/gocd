##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe ApiV1::Config::StageAuthorizationRepresenter do
  it 'should render stage authorization with hal representation' do
    presenter = ApiV1::Config::StageAuthorizationRepresenter.new(get_admins_config)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(stage_authorization_hash)
  end

  it "should convert from document to AdminsConfig" do
    admins_config = com.thoughtworks.go.config.AdminsConfig.new

    ApiV1::Config::StageAuthorizationRepresenter.new(admins_config).from_hash(stage_authorization_hash)

    expected = get_admins_config
    expect(admins_config).to eq(expected)
  end

  def get_admins_config
    com.thoughtworks.go.config.AdminsConfig.new(
      com.thoughtworks.go.config.AdminRole.new(com.thoughtworks.go.config.CaseInsensitiveString.new('admin_role')),
      com.thoughtworks.go.config.AdminUser.new(com.thoughtworks.go.config.CaseInsensitiveString.new('admin_user')))
  end

  def stage_authorization_hash
    {
      roles: ['admin_role'],
      users: ['admin_user']
    }
  end
end