require 'spec_helper'

describe Jasmine do
  it "should provide the root path" do
    File.stub(:dirname).and_return('lib/jasmine')
    File.should_receive(:expand_path) { |path| path }
    Jasmine.root.should == 'lib/jasmine'
  end
  it "should append passed file paths" do
    File.stub(:dirname).and_return('lib/jasmine')
    File.should_receive(:expand_path) { |path| path }
    Jasmine.root('subdir1', 'subdir2').should == File.join('lib/jasmine', 'subdir1', 'subdir2')
  end
end
