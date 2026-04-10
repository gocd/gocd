#
# Copyright Thoughtworks, Inc.
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

Gem::Specification.new do |s|
  s.name     = 'irb'
  s.version  = '1.99.0'
  s.summary  = 'Stubbed irb gem used to satisfy railties dependency'
  s.authors  = ['GoCD Team']
  s.email    = ['go-cd-dev@googlegroups.com']
  s.homepage = "https://github.com/gocd/gocd"
  s.licenses = ["Apache-2.0"]

  s.files         = ['lib/irb.rb']
  s.require_paths = ['lib']
end
