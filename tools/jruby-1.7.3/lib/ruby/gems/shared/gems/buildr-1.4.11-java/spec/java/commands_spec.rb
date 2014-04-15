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


describe Java::Commands do

  it "should not be verbose by default" do
    write "build.xml", <<-BUILD
    <project name="MyProject" default="dist" basedir=".">
        <description>
            simple example build file
        </description>
        <target name="dist"/>
    </project>
BUILD
    lambda { Java::Commands.java("org.apache.tools.ant.Main", :classpath => Buildr::Ant.dependencies) }.should_not show_info(/java/)
    lambda { Java::Commands.java("org.apache.tools.ant.Main", :classpath => Buildr::Ant.dependencies, :verbose => true) }.should show_info(/java/)
  end
  
  describe "Java::Commands.javac" do
    
    it "should compile java" do
      write "Foo.java", "public class Foo {}"
      lambda { Java::Commands.javac("Foo.java") }.should change {File.exist?("Foo.class")}.to(true)
    end
      
    it 'should let the user specify an output directory' do
      write "Foo.java", "public class Foo {}"
      lambda { Java::Commands.javac("Foo.java", :output => "classes") }.should change {File.exist?("classes/Foo.class")}.to(true)
    end
    
    it "should let the user specify a different name" do
      write "Foo.java", "public class Foo {}"
      lambda { Java::Commands.javac("Foo.java", :name => "bar") }.should show_info("Compiling 1 source files in bar")
    end
    
    it "should let the user specify a source path" do
      write "ext/org/Bar.java", "package org; public class Bar {}"
      write "Foo.java", "import org.Bar;\n public class Foo {}"
      lambda { Java::Commands.javac("Foo.java", :sourcepath => File.expand_path("ext")) }.should change {File.exist?("Foo.class")}.to(true)
    end
    
    it "should let the user specify a classpath" do
      write "ext/org/Bar.java", "package org; public class Bar {}"
      Java::Commands.javac("ext/org/Bar.java", :output => "lib")
      write "Foo.java", "import org.Bar;\n public class Foo {}"
      lambda { Java::Commands.javac("Foo.java", :classpath => File.expand_path("lib")) }.should change {File.exist?("Foo.class")}.to(true)
    end
  end
  
  describe "Java::Commands.javadoc" do
    
    it "should fail if no output is defined" do
      lambda { Java::Commands.javadoc("Foo.java") }.should raise_error(/No output defined for javadoc/)
    end
    
    it "should create javadoc" do
      write "Foo.java", "public class Foo {}"
      lambda { Java::Commands.javadoc("Foo.java", :output => "doc") }.should change {File.exist?("doc/Foo.html")}.to(true)
    end
    
    it "should accept file tasks as arguments" do
      foo = file("Foo.java") do |file|
        file.enhance do
          write file.to_s, "public class Foo {}"
        end
      end
      lambda { Java::Commands.javadoc(foo, :output => "doc") }.should change {File.exist?("doc/Foo.html")}.to(true)
    end
      
    it "should accept projects as arguments" do
      write "src/main/java/Foo.java", "public class Foo {}"
      write "src/main/java/bar/Foobar.java", "package bar; public class Foobar {}"
      define "foo" do
      end
      lambda { Java::Commands.javadoc(project("foo"), :output => "doc") }.should change {File.exist?("doc/Foo.html") && File.exist?("doc/bar/Foobar.html")}.to(true)
    end
  end
end