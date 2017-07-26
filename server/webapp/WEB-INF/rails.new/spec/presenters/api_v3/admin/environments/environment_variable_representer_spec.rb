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

describe ApiV3::Admin::Environments::EnvironmentVariableRepresenter do
  it 'should render plain environment variable with hal representation' do
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new({env_var: get_plain_variable, environment: get_environment_config})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(get_plain_text_hash)
  end

  it 'should render secure environment variable with hal representation' do
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new({env_var: get_secure_variable, environment: get_environment_config})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(get_secure_hash_with_encrypted_value)
  end

  it 'should convert from secure hash with encrypted value to EnvironmentVariableConfig' do
    config = EnvironmentVariableConfig.new
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new(config)
    presenter.from_hash(get_secure_hash_with_encrypted_value)
    expect(config).to eq(get_secure_variable)
  end

  it 'should convert from secure hash with clear text value to EnvironmentVariableConfig' do
    config = EnvironmentVariableConfig.new
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new(config)
    presenter.from_hash(secure_hash_with_clear_text_value)
    expect(config).to eq(get_secure_variable)
  end

  it 'should deserialize an ambiguous encrypted variable (with both value and encrypted_value) to a variable with errors' do
    config = EnvironmentVariableConfig.new
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new(config)
    config = presenter.from_hash(name: 'PASSWORD', secure: true, value: 'plainText', encrypted_value: 'c!ph3rt3xt')
    expect(config.errors.getAllOn('value').to_a).to eq(['You may only specify `value` or `encrypted_value`, not both!'])
    expect(config.errors.getAllOn('encryptedValue').to_a).to eq(['You may only specify `value` or `encrypted_value`, not both!'])
  end

  it 'should deserialize an unambiguous encrypted variable (with either value or encrypted_value) to a variable without errors' do
    config = EnvironmentVariableConfig.new
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new(config)
    config = presenter.from_hash(name: 'PASSWORD', secure: true, value: 'plainText')
    expect(config.errors).to be_empty

    config = EnvironmentVariableConfig.new
    presenter = ApiV3::Admin::Environments::EnvironmentVariableRepresenter.new(config)
    config = presenter.from_hash(name: 'PASSWORD', secure: true, encrypted_value: 'c!ph3rt3xt')
    expect(config.errors).to be_empty
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

  def secure_hash_with_clear_text_value
    {
      secure: true,
      name: 'secure',
      value: 'confidential'
    }
  end

  def get_environment_config
    env = EnvironmentConfigMother.environment('dev').tap {|c| c.addEnvironmentVariable('username', 'admin')}
    env.setOrigins(com.thoughtworks.go.config.remote.FileConfigOrigin.new)
    env
  end
end
