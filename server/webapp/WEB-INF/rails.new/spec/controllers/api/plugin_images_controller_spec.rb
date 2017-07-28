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

describe Api::PluginImagesController do

  before :each do
    @default_plugin_info_finder = double('default_plugin_info_finder')
    controller.stub('default_plugin_info_finder').and_return(@default_plugin_info_finder)
  end

  it 'should render an image with a hash and a long lived cache header' do
    image = com.thoughtworks.go.plugin.domain.common.Image.new('image/foo', Base64.strict_encode64('some-image-data'), SecureRandom.hex(32))
    @default_plugin_info_finder.should_receive(:getImage).with('foo', image.getHash).and_return(image)

    get :show, plugin_id: 'foo', hash: image.getHash
    expect(response).to be_ok
    expect(response.headers['Cache-Control']).to eq('max-age=31557600, private')
    expect(response.headers['Content-Type']).to eq('image/foo')
    expect(response.body.bytes.to_a).to eq(image.getDataAsBytes.to_a)
  end

  it 'renders 404 when plugin or hash does not match up' do
    hash = SecureRandom.hex(32)
    @default_plugin_info_finder.should_receive(:getImage).with('foo', hash).and_return(nil)

    get :show, plugin_id: 'foo', hash: hash
    expect(response).to be_not_found
  end
end
