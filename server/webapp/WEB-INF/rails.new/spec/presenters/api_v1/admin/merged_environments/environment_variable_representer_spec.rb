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

describe ApiV1::Admin::MergedEnvironments::EnvironmentVariableRepresenter do
  it 'should render plain environment variable with hal representation' do
    presenter = ApiV1::Admin::MergedEnvironments::EnvironmentVariableRepresenter.new({env_var: get_plain_variable, environment: get_environment_config})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(get_plain_text_hash)
  end

  it 'should render secure environment variable with hal representation' do
    presenter = ApiV1::Admin::MergedEnvironments::EnvironmentVariableRepresenter.new({env_var: get_secure_variable, environment: get_environment_config})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(get_secure_hash_with_encrypted_value)
  end

  def get_secure_variable
    EnvironmentVariableConfig.new(GoCipher.new, 'secure', 'confidential', true)
  end

  def get_plain_variable
    EnvironmentVariableConfig.new(GoCipher.new, 'plain', 'plain', false)
  end

  def get_plain_text_hash
    {
      name: 'plain',
      value: 'plain',
      secure: false,
      origin: {
        type: 'local',
        file: {
          _links: {
            self: {
              href: 'http://test.host/admin/config_xml'
            },
            doc: {
              href: 'https://api.gocd.org/#get-configuration'
            }
          },
          name: 'cruise-config.xml'
        }
      }
    }
  end

  def get_secure_hash_with_encrypted_value
    {
      secure: true,
      name: 'secure',
      encrypted_value: GoCipher.new.encrypt('confidential'),
      origin: {
        type: 'local',
        file: {
          _links: {
            self: {
              href: 'http://test.host/admin/config_xml'
            },
            doc: {
              href: 'https://api.gocd.org/#get-configuration'
            }
          },
          name: 'cruise-config.xml'
        }
      }
    }
  end

  def get_environment_config
    env = EnvironmentConfigMother.environment('dev').tap {|c| c.addEnvironmentVariable('username', 'admin')}
    env.setOrigins(com.thoughtworks.go.config.remote.FileConfigOrigin.new)
    env
  end
end