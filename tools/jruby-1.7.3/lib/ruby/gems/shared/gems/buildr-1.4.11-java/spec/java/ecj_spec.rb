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



describe Buildr::Compiler::Ecj do

  before(:all) do
    #Make ecj appear as a compiler that applies:
    class Buildr::Compiler::Ecj
      class << self

        def applies_to?(project, task)
          paths = task.sources + [sources].flatten.map { |src| Array(project.path_to(:source, task.usage, src.to_sym)) }
          paths.flatten!
          ext_glob = Array(source_ext).join(',')

          paths.each { |path| 
            Find.find(path) {|found|
              if (!File.directory?(found)) && found.match(/.*\.#{Array(source_ext).join('|')}/)
                return true
              end
              } if File.exist? path
            }
            false
          end
        end
      end
    end

    it "should be the default Java compiler once loaded" do
      write 'src/main/java/Foo.java', 'public class Foo {}'
    foo = define('foo')
    foo.compile.compiler.should == :ecj
  end

  describe "should compile a Java project just in the same way javac does" do  
    javac_spec = File.read(File.join(File.dirname(__FILE__), "compiler_spec.rb"))
    javac_spec = javac_spec.match(Regexp.escape("require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))\n")).post_match
    javac_spec.gsub!("javac", "ecj")
    javac_spec.gsub!("nowarn", "warn:none")
    eval(javac_spec)
  end

  # Redirect the java error ouput, yielding so you can do something while it is
  # and returning the content of the error buffer.
  #
  def redirect_java_err
    pending "RJB doesn't support well instantiating a class that has several constructors" unless RUBY_PLATFORM =~ /java/
    err = Java.java.io.ByteArrayOutputStream.new
    original_err = Java.java.lang.System.err
    begin
      printStream = Java.java.io.PrintStream
      print = printStream.new(err)
      Java.java.lang.System.setErr(print)
      yield
    ensure
      Java.java.lang.System.setErr(original_err)
    end
    err.toString
  end

  it "should not issue warnings for type casting when warnings are set to warn:none, by default" do
    write "src/main/java/Main.java", "import java.util.List; public class Main {public List get() {return null;}}"
    foo = define("foo") {
      compile.options.source = "1.5"
      compile.options.target = "1.5"
    }
    redirect_java_err { foo.compile.invoke }.should_not match(/WARNING/)
  end

  it "should issue warnings for type casting when warnings are set" do
    write "src/main/java/Main.java", "import java.util.List; public class Main {public List get() {return null;}}"
    foo = define("foo") {
      compile.options.source = "1.5"
      compile.options.target = "1.5"
      compile.options.warnings = true
    }
    redirect_java_err { foo.compile.invoke }.should match(/WARNING/)
  end

  after(:all) do
    #Make ecj appear as a compiler that doesn't apply:
    module Buildr
      module Compiler

        class Ecj

          class << self

            def applies_to?(project, task)
              false
            end
          end
        end
      end
    end
  end
end


