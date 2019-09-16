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

describe ApiV1::Config::PluginConfigurationPropertyRepresenter do
  describe 'serialize' do
    it 'non-secure configuration value with hal representation' do
      actual_json   = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(get_non_secure_property).to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(get_plain_text_hash)
    end

    it 'secure configuration value with hal representation' do
      actual_json   = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(get_secure_property).to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(secure_hash_with_encrypted_value)
    end
  end

  describe 'deserialize' do
    it 'from hash with encrypted value' do
      configuration_property = ConfigurationProperty.new
      presenter = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(configuration_property)
      presenter.from_hash(secure_hash_with_encrypted_value)
      expect(configuration_property).to eq(get_secure_property)
    end

    it 'from hash with clear text value' do
      configuration_property = ConfigurationProperty.new
      presenter = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(configuration_property)
      presenter.from_hash(get_plain_text_hash)
      expect(configuration_property).to eq(get_non_secure_property)
    end

    it 'should handle boolean as string' do
      configuration_property = ConfigurationProperty.new
      presenter = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(configuration_property)

      presenter.from_hash({key: 'boolean-prop', value: true})

      expect(configuration_property).to eq(property('boolean-prop', 'true'))
    end

    it 'should handle nil' do
      configuration_property = ConfigurationProperty.new
      presenter = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(configuration_property)

      presenter.from_hash({key: 'null-prop', value: nil})

      expect(configuration_property).to eq(property('null-prop', nil))
    end

    it 'should handle missing value' do
      configuration_property = ConfigurationProperty.new
      presenter = ApiV1::Config::PluginConfigurationPropertyRepresenter.new(configuration_property)

      presenter.from_hash({key: 'missing-value-prop'})

      expect(configuration_property).to eq(property('missing-value-prop', nil))
    end
  end

  def property(key, value)
    value_to_use = value.nil? ? nil : ConfigurationValue.new(value)
    ConfigurationProperty.new(ConfigurationKey.new(key), value_to_use)
  end

  def get_non_secure_property
    property('key', 'non-encrypted-value')
  end

  def get_secure_property
    ConfigurationProperty.new(ConfigurationKey.new('key'), EncryptedConfigurationValue.new(GoCipher.new.encrypt('confidential')))
  end

  def secure_hash_with_encrypted_value
    {
      key: "key",
      encrypted_value: GoCipher.new.encrypt('confidential')
    }
  end

  def get_plain_text_hash
    {
      key:   'key',
      value:  'non-encrypted-value'
    }
  end
end
