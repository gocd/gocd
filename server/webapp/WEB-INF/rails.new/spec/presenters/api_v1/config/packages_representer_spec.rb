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

describe ApiV1::Config::PackagesRepresenter do

  it 'renders a list of packages' do
    pkg1 = PackageDefinition.new("uuid-1", "prettyjson", Configuration.new)
    pkg2 = PackageDefinition.new("uuid-2", "lodash", Configuration.new)
    packages = Packages.new(pkg1, pkg2)

    presenter = ApiV1::Config::PackagesRepresenter.new(packages)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :doc)
    actual_json.fetch(:_embedded).should == {
      :packages => [ApiV1::Config::PackageRepresenter.new({package: pkg1}).to_hash(url_builder: UrlBuilder.new),
                    ApiV1::Config::PackageRepresenter.new({package: pkg2}).to_hash(url_builder: UrlBuilder.new)]
    }

  end
end
