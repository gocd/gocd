# testresult_spec.rb
#
# Copyright Caleb Powell 2007
#
# Licensed under the LGPL, see the file README.txt in the distribution
require 'fileutils'
$LOAD_PATH.push(FileUtils::pwd + '/lib')
require 'java_adapter.rb'

describe Antwrap::JavaAdapter do

  it "should extract the Java class name from a string" do
    Antwrap::JavaAdapter.extract_class_name("java.lang.String").should eql("String")
    Antwrap::JavaAdapter.extract_class_name("Foo").should eql("Foo")
    Antwrap::JavaAdapter.extract_class_name("java.lang.__String").should eql("__String")
    Antwrap::JavaAdapter.extract_class_name("java.lang.Outer$Inner").should eql("Outer$Inner")
  end

  it "should import a class" do
    result = Antwrap::JavaAdapter.import_class("java.lang.String")
    result.should_not be_nil
    result.should respond_to(:new)
  end
end