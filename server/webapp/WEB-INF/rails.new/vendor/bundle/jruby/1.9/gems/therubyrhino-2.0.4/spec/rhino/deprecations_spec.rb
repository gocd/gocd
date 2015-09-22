require File.expand_path('../spec_helper', File.dirname(__FILE__))

require 'stringio'

describe 'deprecations' do
  
  stderr = $stderr
  
  before do
    $stderr = StringIO.new
  end
  
  after do
    $stderr = stderr
  end
  
  it "To ruby 42" do
    Rhino::To.ruby(42).should == 42
  end

  it "To javascript 42" do
    Rhino::To.javascript(42).should == 42
  end

  it "J constant still works" do
    lambda { Rhino::J::Scriptable }.should_not raise_error
  end
  
  it "NativeObject constant exists" do
    lambda { Rhino::NativeObject }.should_not raise_error
  end

  it "NativeFunction constant exists" do
    lambda { Rhino::NativeFunction }.should_not raise_error
  end

  it "JavascriptError returns JSError" do
    lambda { Rhino::JavascriptError.should be(Rhino::JSError) }.should_not raise_error
  end
  
end