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

module Buildr #:nodoc:
  module PackageAsTestJar
    def package_as_test_jar_spec(spec) #:nodoc:
      spec.merge(:type => :jar, :classifier => 'tests')
    end

    def package_as_test_jar(file_name) #:nodoc:
      ZipTask.define_task(file_name).tap do |zip|
        zip.include :from => [test.compile.target, test.resources.target].compact
      end
    end
  end
end

class Buildr::Project
  include Buildr::PackageAsTestJar
end
