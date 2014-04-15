# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  VERSION = '1.3.4'.freeze
end

require 'buildr/core'
require 'buildr/packaging'
require 'buildr/java'
require 'buildr/ide'

# Methods defined in Buildr are both instance methods (e.g. when included in Project)
# and class methods when invoked like Buildr.artifacts().
module Buildr ; extend self ; end
# The Buildfile object (self) has access to all the Buildr methods and constants.
class << self ; include Buildr ; end
class Object #:nodoc:
  Buildr.constants.each do |name|
    const = Buildr.const_get(name)
    const_set name, const if const.is_a?(Module)
  end
end
