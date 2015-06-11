# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.


require File.expand_path('../spec_helpers', File.dirname(__FILE__))
Sandbox.require_optional_extension 'buildr/bnd'

def open_zip_file(file = 'target/foo-2.1.3.jar')
  jar_filename = @foo._(file)
  File.should be_exist(jar_filename)
  Zip::ZipFile.open(jar_filename) do |zip|
    yield zip
  end
end

def open_main_manifest_section(file = 'target/foo-2.1.3.jar')
  jar_filename = @foo._(file)
  File.should be_exist(jar_filename)
  yield Buildr::Packaging::Java::Manifest.from_zip(jar_filename).main
end

describe Buildr::Bnd do
  before do
    repositories.remote << Buildr::Bnd.remote_repository
  end

  describe "project.bnd version (assure backward compatibility)" do

    after do
      STDERR.puts("backward compatibility: used #{Buildr::Bnd.version} restoring #{@savedVersion}")
      Buildr::Bnd.version = @savedVersion
    end

    before do
      @savedVersion = Buildr::Bnd.version
      Buildr::Bnd.version = '0.0.384'
      write "src/main/java/com/biz/Foo.java", <<SRC
package com.biz;
public class Foo {}
SRC
      write "bar/src/main/java/com/biz/bar/Bar.java", <<SRC
package com.biz.bar;
public class Bar {}
SRC

        @foo = define "foo" do
          project.version = "2.1.3"
          project.group = "mygroup"
          manifest["Magic-Food"] = "Chocolate"
          manifest["Magic-Drink"] = "Wine"
          package(:bundle).tap do |bnd|
            bnd["Export-Package"] = "com.*"
          end

          define "bar" do
            project.version = "2.2"
            package(:bundle).tap do |bnd|
              bnd["Magic-Food"] = "Cheese"
              bnd["Export-Package"] = "com.*"
            end
          end
        end
        task('package').invoke
      end

      it "version 0.0.384 does not export the version and wrong import-package" do
        open_main_manifest_section do |attribs|
          attribs['Bundle-Name'].should eql('foo')
          attribs['Bundle-Version'].should eql('2.1.3')
          attribs['Bundle-SymbolicName'].should eql('mygroup.foo')
          attribs['Export-Package'].should eql('com.biz')
          attribs['Import-Package'].should eql('com.biz')
        end
      end
  end

  describe "package :bundle" do
    describe "with a valid bundle" do
      before do
        write "src/main/java/com/biz/Foo.java", <<SRC
package com.biz;
public class Foo {}
SRC
        write "src/main/resources/IRIS-INF/iris.config", <<SRC
some=setting
SRC
        write "bar/src/main/java/com/biz/bar/Bar.java", <<SRC
package com.biz.bar;
public class Bar {}
SRC
        @foo = define "foo" do
          project.version = "2.1.3"
          project.group = "mygroup"
          manifest["Magic-Food"] = "Chocolate"
          manifest["Magic-Drink"] = "Wine"
          package(:bundle).tap do |bnd|
            bnd["Export-Package"] = "com.*"
          end

          define "bar" do
            project.version = "2.2"
            package(:bundle).tap do |bnd|
              bnd["Magic-Food"] = "Cheese"
              bnd["Export-Package"] = "com.*"
            end
          end
        end
        task('package').invoke
      end

      it "produces a .bnd in the correct location for root project" do
        File.should be_exist(@foo._("target/foo-2.1.3.bnd"))
      end

      it "produces a .jar in the correct location for root project" do
        File.should be_exist(@foo._("target/foo-2.1.3.jar"))
      end

      it "produces a .jar containing correct .class files for root project" do
        open_zip_file do |zip|
          zip.file.exist?('com/biz/Foo.class').should be_true
        end
      end

      it "produces a .jar containing resoruces from resource directory root project" do
        open_zip_file do |zip|
          zip.file.exist?('IRIS-INF/iris.config').should be_true
        end
      end

      it "produces a .jar containing expected manifest entries derived from project.bnd for root project" do
        open_main_manifest_section do |attribs|
          attribs['Bundle-Name'].should eql('foo')
          attribs['Bundle-Version'].should eql('2.1.3')
          attribs['Bundle-SymbolicName'].should eql('mygroup.foo')
          attribs['Export-Package'].should eql('com.biz;version="2.1.3"')
          attribs['Import-Package'].should be_nil
        end
      end

      it "produces a .jar containing expected manifest entries derived from project.manifest root project" do
        open_main_manifest_section do |attribs|
          attribs['Magic-Drink'].should eql('Wine')
          attribs['Magic-Food'].should eql('Chocolate')
        end
      end

      it "produces a .bnd in the correct location for subproject project" do
        File.should be_exist(@foo._("bar/target/foo-bar-2.2.bnd"))
      end

      it "produces a .jar in the correct location for subproject project" do
        File.should be_exist(@foo._("bar/target/foo-bar-2.2.jar"))
      end

      it "produces a .jar containing correct .class files for subproject project" do
        open_zip_file('bar/target/foo-bar-2.2.jar') do |zip|
          zip.file.exist?('com/biz/bar/Bar.class').should be_true
        end
      end

      it "produces a .jar containing expected manifest entries derived from project.bnd for subproject project" do
        open_main_manifest_section('bar/target/foo-bar-2.2.jar') do |attribs|
          attribs['Bundle-Name'].should eql('foo:bar')
          attribs['Bundle-Version'].should eql('2.2')
          attribs['Bundle-SymbolicName'].should eql('mygroup.foo.bar')
          attribs['Export-Package'].should eql('com.biz.bar;version="2.2"')
          attribs['Import-Package'].should be_nil
        end
      end

      it "produces a .jar containing expected manifest entries derived from project.manifest subproject project" do
        open_main_manifest_section('bar/target/foo-bar-2.2.jar') do |attribs|
          attribs['Magic-Drink'].should eql('Wine')
          attribs['Magic-Food'].should eql('Cheese')
        end
      end
    end

    describe "with an invalid bundle" do
      before do
        # bundle invalid as no source
        @foo = define "foo" do
          project.version = "2.1.3"
          project.group = "mygroup"
          package(:bundle).tap do |bnd|
            bnd["Export-Package"] = "*"
          end
        end
      end

      it "raise an error if unable to build a valid bundle" do
        lambda { task('package').invoke }.should raise_error
      end

      it "raise not produce an invalid jar file" do
        lambda { task('package').invoke }.should raise_error
        File.should_not be_exist(@foo._("target/foo-2.1.3.jar"))
      end
    end

    describe "using classpath_element to specify dependency" do
      before do
        @foo = define "foo" do
          project.version = "2.1.3"
          project.group = "mygroup"
          package(:bundle).tap do |bnd|
            bnd['Export-Package'] = 'org.apache.tools.zip.*'
            Buildr::Ant.dependencies.each do |d|
              bnd.classpath_element d
            end
          end
        end
      end

      it "should not raise an error during packaging" do
        lambda { task('package').invoke }.should_not raise_error
      end

      it "should generate package with files exported from dependency" do
        task('package').invoke
        open_main_manifest_section do |attribs|
          attribs['Export-Package'].should eql('org.apache.tools.zip;version="2.1.3"')
        end
      end
    end

    describe "using classpath to specify dependencies" do
      before do
        write "src/main/java/com/biz/Foo.java", <<SRC
package com.biz;
public class Foo {}
SRC
        write "bar/src/main/java/com/biz/bar/Bar.java", <<SRC
package com.biz.bar;
public class Bar {}
SRC
        @foo = define "foo" do
          project.version = "2.1.3"
          project.group = "mygroup"
          package(:bundle).tap do |bnd|
            bnd['Export-Package'] = 'org.apache.tools.zip.*'
            bnd.classpath = bnd.classpath + Buildr::Ant.dependencies
          end
        end
      end

      it "should not raise an error during packaging" do
        lambda { task('package').invoke }.should_not raise_error
      end

      it "should generate package with files exported from dependency" do
        task('package').invoke
        open_main_manifest_section do |attribs|
          attribs['Export-Package'].should eql('org.apache.tools.zip;version="2.1.3"')
        end
      end
    end

    describe "using compile dependencies to specify dependency" do
      before do
        @foo = define "foo" do
          project.version = "2.1.3"
          project.group = "mygroup"
          compile.with Buildr::Ant.dependencies
          package(:bundle).tap do |bnd|
            bnd['Export-Package'] = 'org.apache.tools.zip.*'
          end
        end
      end

      it "should not raise an error during packaging" do
        lambda { task('package').invoke }.should_not raise_error
      end

      it "should generate package with files exported from dependency" do
        task('package').invoke
        open_main_manifest_section do |attribs|
          attribs['Export-Package'].should eql('org.apache.tools.zip;version="2.1.3"')
        end
      end
    end
  end

  describe "project.bnd defaults" do

    before do
      write "src/main/java/com/biz/Foo.java", <<SRC
package com.biz;
public class Foo {}
SRC
      write "bar/src/main/java/com/biz/bar/Bar.java", <<SRC
package com.biz.bar;
public class Bar {}
SRC

      @foo = define "foo" do
        project.version = "2.1.3"
        project.group = "mygroup"
        package :bundle
        compile.with Buildr::Ant.dependencies
        desc "My Bar Project"
        define "bar" do
          package :bundle
        end
      end
      @bar = @foo.project('bar')
    end

    it "defaults Bundle-Version to project.version" do
      @foo.packages[0].to_params['Bundle-Version'].should eql('2.1.3')
      @bar.packages[0].to_params['Bundle-Version'].should eql('2.1.3')
    end

    it "defaults -classpath to compile path and dependencies" do
      @foo.packages[0].to_params['-classpath'].should include(@foo.compile.target.to_s)
      @foo.packages[0].to_params['-classpath'].should include(Buildr.artifact(Buildr::Ant.dependencies[0]).to_s)
      @bar.packages[0].to_params['-classpath'].should include(@bar.compile.target.to_s)
    end

    it "classpath method returns compile path and dependencies" do
      @foo.packages[0].classpath.should include(@foo.compile.target)
      Buildr::Ant.dependencies.each do |dependency|
        @foo.packages[0].classpath.to_s.should include(Buildr.artifact(dependency).to_s)
      end
      @bar.packages[0].classpath.should include(@bar.compile.target)
    end

    it "defaults Bundle-SymbolicName to combination of group and name" do
      @foo.packages[0].to_params['Bundle-SymbolicName'].should eql('mygroup.foo')
      @bar.packages[0].to_params['Bundle-SymbolicName'].should eql('mygroup.foo.bar')
    end

    it "defaults Export-Package to nil" do
      @foo.packages[0].to_params['Export-Package'].should be_nil
      @bar.packages[0].to_params['Export-Package'].should be_nil
    end

    it "defaults Import-Package to nil" do
      @foo.packages[0].to_params['Import-Package'].should be_nil
      @bar.packages[0].to_params['Import-Package'].should be_nil
    end

    it "defaults Bundle-Name to project.name if comment not present" do
      @foo.packages[0].to_params['Bundle-Name'].should eql('foo')
    end

    it "defaults Bundle-Name to comment if present" do
      @bar.packages[0].to_params['Bundle-Name'].should eql('My Bar Project')
    end

    it "defaults Bundle-Description to project.full_comment" do
      @foo.packages[0].to_params['Bundle-Description'].should be_nil
      @bar.packages[0].to_params['Bundle-Description'].should eql('My Bar Project')
    end

    it "defaults -removeheaders to" do
      @foo.packages[0].to_params['-removeheaders'].should eql("Include-Resource,Bnd-LastModified,Created-By,Implementation-Title,Tool")
    end
  end

  describe "project extension" do
    it "provides an 'bnd:print' task" do
      Rake::Task.tasks.detect { |task| task.to_s == "bnd:print" }.should_not be_nil
    end

    it "documents the 'bnd:print' task" do
      Rake::Task.tasks.detect { |task| task.to_s == "bnd:print" }.comment.should_not be_nil
    end
  end

end
