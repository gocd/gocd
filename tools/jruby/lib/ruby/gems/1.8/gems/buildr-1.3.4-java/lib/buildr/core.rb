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


require 'buildr/core/common'
require 'buildr/core/application'
require 'buildr/core/project'
require 'buildr/core/environment'
require 'buildr/core/help'
require 'buildr/core/build'
require 'buildr/core/filter'
require 'buildr/core/compile'
require 'buildr/core/test'
require 'buildr/core/checks'
require 'buildr/core/transports'
require 'buildr/core/generate'
require 'buildr/core/osx' if RUBY_PLATFORM =~ /darwin/
