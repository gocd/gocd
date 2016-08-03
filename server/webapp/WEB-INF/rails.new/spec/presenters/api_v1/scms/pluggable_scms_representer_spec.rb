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

describe ApiV1::Scms::PluggableScmsRepresenter do

  it 'should render all pluggable scm materials with hal representation' do
    scm = SCM.new('scm-id', PluginConfiguration.new('foo', '1'), nil)
    scm.setName('material')

    actual_json = ApiV1::Scms::PluggableScmsRepresenter.new([scm]).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_scms_url)
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#scms')
    actual_json.delete(:_links)

    actual_json.fetch(:_embedded).should == { :scms => [ApiV1::Scms::PluggableScmSummaryRepresenter.new(scm).to_hash(url_builder: UrlBuilder.new)] }
  end
end
