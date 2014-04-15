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


require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))

COMPILERS = Buildr::Compiler.compilers.dup
COMPILERS_WITHOUT_JAVAC = COMPILERS.dup
COMPILERS_WITHOUT_JAVAC.delete Buildr::Compiler::Javac

describe Buildr::Compiler::ExternalJavac do

  before(:all) do
    Buildr::Compiler.send :compilers=, COMPILERS_WITHOUT_JAVAC
  end

  describe "should compile a Java project just in the same way javac does" do
    javac_spec = File.read(File.join(File.dirname(__FILE__), "compiler_spec.rb"))
    javac_spec = javac_spec.match(Regexp.escape("require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))\n")).post_match
    javac_spec.gsub!("javac", "externaljavac")
    javac_spec.gsub!("--trace=externaljavac", "--trace=javac")
    javac_spec.gsub!("trace_categories = [:externaljavac]", "trace_categories = [:javac]")
    eval(javac_spec)
  end

  it "should accept a :jvm option as JAVA_HOME" do
    write 'src/main/java/Foo.java', 'public class Foo {}'
    define "foo" do
      compile.using(:externaljavac).options.jvm = "somepath"
    end
    begin
      trace true #We set it true to grab the trace statement with the jvm path in it!
      lambda {lambda {project("foo").compile.invoke}.should raise_error(RuntimeError, /Failed to compile, see errors above/)}.should show(/somepath\/bin\/javac .*/)
    end
    trace false
  end

  after :all do
    Buildr::Compiler.send :compilers=, COMPILERS
  end

end


