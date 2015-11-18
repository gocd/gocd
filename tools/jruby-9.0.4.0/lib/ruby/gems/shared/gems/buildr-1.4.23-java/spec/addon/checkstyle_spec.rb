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

require File.expand_path('../spec_helpers', File.dirname(__FILE__))
if Java.java.lang.System.getProperty('java.runtime.version') >= '1.7'

Sandbox.require_optional_extension 'buildr/checkstyle'
artifacts(Buildr::Checkstyle::dependencies).map(&:invoke)

CHECKS_CONTENT = <<CHECKS
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
            "-//Puppy Crawl//DTD Check Configuration 1.2//EN"
            "http://www.puppycrawl.com/dtds/configuration_1_2.dtd">
<module name="Checker">
</module>
CHECKS
GOOD_CONTENT = <<GOOD
public final class SomeClass {
}
GOOD

describe Buildr::Checkstyle do

  before do
    # Reloading the extension because the sandbox removes all its actions
    Buildr.module_eval { remove_const :Checkstyle }
    load File.expand_path('../addon/buildr/checkstyle.rb')
    @tool_module = Buildr::Checkstyle

    write 'src/main/java/SomeClass.java', GOOD_CONTENT
    write 'src/main/etc/checkstyle/checks.xml', CHECKS_CONTENT
  end

  it 'should generate an XML report' do
    define 'foo'
    task('foo:checkstyle:xml').invoke
    file(project('foo')._('reports/checkstyle/checkstyle.xml')).should exist
  end

  it 'should generate an HTML report' do
    define 'foo'
    task('foo:checkstyle:html').invoke
    file(project('foo')._('reports/checkstyle/checkstyle.html')).should exist
  end

end
end
