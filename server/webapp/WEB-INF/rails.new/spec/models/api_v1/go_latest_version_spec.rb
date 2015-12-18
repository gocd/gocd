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

describe ApiV1::GoLatestVersion do
  describe :valid? do
    before(:each) do
      @public_key_path = 'update_server_public_key_path'
      @public_key = 'public_key'
      @message = %Q({\n  "latest-version": "16.1.0-123",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
      @message_signature = 'message_signature'
      @signing_public_key = "signing_public_key"
      @signing_public_key_signature = "signing_public_key_signature"

      @latest_version_hash = {:message => @message,
                              :message_signature => @message_signature,
                              :signing_public_key => @signing_public_key,
                              :signing_public_key_signature => @signing_public_key_signature}

      @system_environment = double('system_environment', :getUpdateServerPublicKeyPath => @public_key_path)
      @result = double('HttpLocalizedOperationResult')
      File.stub(:read).with(@public_key_path).and_return(@public_key)
    end

    it 'should be valid if message and signing public key are not tampered' do
      @system_environment.should_receive(:getUpdateServerPublicKeyPath).and_return(@public_key_path)
      File.should_receive(:read).with(@public_key_path).and_return(@public_key)
      MessageVerifier.should_receive(:verify).ordered.with(@signing_public_key, @signing_public_key_signature, @public_key).and_return(true)
      MessageVerifier.should_receive(:verify).ordered.with(@message, @message_signature, @signing_public_key).and_return(true)

      latest_version = ApiV1::GoLatestVersion.new(@latest_version_hash, @system_environment)

      expect(latest_version.valid?).to be(true)
    end

    it 'should be invalid if signing public key is tampered' do
      bad_signing_public_key = 'bad_signing_public_key'
      latest_version_hash = {:message => @message,
                             :message_signature => @message_signature,
                             :signing_public_key => bad_signing_public_key,
                             :signing_public_key_signature => @signing_public_key_signature}

      MessageVerifier.should_receive(:verify).with(bad_signing_public_key, @signing_public_key_signature, @public_key).and_return(false)
      MessageVerifier.should_receive(:verify).with(@message, @message_signature, @signing_public_key).never

      latest_version = ApiV1::GoLatestVersion.new(latest_version_hash, @system_environment)

      expect(latest_version.valid?).to be(false)
    end

    it 'should be invalid if message is tampered' do
      bad_message = 'bad_message'
      latest_version_hash = {:message => bad_message,
                             :message_signature => @message_signature,
                             :signing_public_key => @signing_public_key,
                             :signing_public_key_signature => @signing_public_key_signature}

      MessageVerifier.should_receive(:verify).with(@signing_public_key, @signing_public_key_signature, @public_key).and_return(true)
      MessageVerifier.should_receive(:verify).with(bad_message, @message_signature, @signing_public_key).and_return(false)

      latest_version = ApiV1::GoLatestVersion.new(latest_version_hash, @system_environment)

      expect(latest_version.valid?).to be(false)
    end
  end
  describe :latest_version do
    it 'should return the latest_version'do
      latest_version_hash = {:message => %Q({\n  "latest-version": "16.1.0-123",\n  "release-time": "2015-07-13 17:52:28 UTC"\n}),
                              :message_signature => 'message_signature',
                              :signing_public_key => 'signing_public_key',
                              :signing_public_key_signature => 'signing_public_key_signature'}

      go_latest_version = ApiV1::GoLatestVersion.new(latest_version_hash, nil)

      expect(go_latest_version.latest_version).to eq("16.1.0-123")
    end
  end
end
