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

module ApiV1
  class GoLatestVersion
    def initialize(latest_version, system_environment)
      @message = latest_version[:message]
      @message_signature = latest_version[:message_signature]
      @signing_public_key = latest_version[:signing_public_key]
      @signing_public_key_signature = latest_version[:signing_public_key_signature]
      @system_environment = system_environment
    end

    def valid?
      !signing_public_key_tampered? and !message_tampered?
    end

    def latest_version
      JSON.parse(@message)['latest-version']
    end

    private

    def message_tampered?
      !MessageVerifier.verify(@message, @message_signature, @signing_public_key)
    end

    def signing_public_key_tampered?
      public_key = File.read(@system_environment.getUpdateServerPublicKeyPath())

      !MessageVerifier.verify(@signing_public_key, @signing_public_key_signature, public_key)
    end
  end
end