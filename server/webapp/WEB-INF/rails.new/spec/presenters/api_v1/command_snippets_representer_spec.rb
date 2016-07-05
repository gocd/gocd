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

describe ApiV1::CommandSnippetsRepresenter do
  it 'renders multiple command snippets with hal representation' do
    snippet = com.thoughtworks.go.helper.CommandSnippetMother.validSnippet("scp")

    presenter   = ApiV1::CommandSnippetsRepresenter.new([snippet])
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new, prefix: 'sc')

    expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_internal_command_snippets_url(:prefix => 'sc'))
    actual_json.delete(:_links)

    actual_json.fetch(:_embedded).should == { :command_snippets => [ApiV1::CommandSnippetRepresenter.new(snippet).to_hash(url_builder: UrlBuilder.new)] }
  end
end