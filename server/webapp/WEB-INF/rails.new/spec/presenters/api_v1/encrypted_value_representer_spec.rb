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

describe ApiV1::EncryptedValueRepresenter do
  it 'should render encrypted value with hal representation' do
    actual_json = ApiV1::EncryptedValueRepresenter.new("encrypted_string").to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_encrypt_url)
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#encryption')
    actual_json.delete(:_links)

    expect(actual_json).to eq({encrypted_value: "encrypted_string"})
  end
end