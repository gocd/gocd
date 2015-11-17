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

class MessageVerifier
  def self.verify(message, signature, public_key)
    signature = Base64.decode64(signature)

    digest = OpenSSL::Digest::SHA512.new
    pkey = OpenSSL::PKey::RSA.new(public_key)

    result = pkey.verify(digest, signature, message)
    rescue => e
      result = false
    result
  end
end