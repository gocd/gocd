require File.dirname(__FILE__) + "/spec_helper"

class MyDocServerSerializerRouter
  attr_accessor :request
  def docs_prefix; 'PREFIX' end
  def initialize; @request = mock_request end
end

class MockDocServerHelper
  include YARD::Templates::Helpers::BaseHelper
  include YARD::Templates::Helpers::HtmlHelper
  include YARD::Server::DocServerHelper

  attr_accessor :adapter
  attr_accessor :single_library
  attr_accessor :library

  def initialize
    @single_library = false
    @library = LibraryVersion.new('foo')
    @adapter = mock_adapter(:router => MyDocServerSerializerRouter.new)
    @serializer = YARD::Server::DocServerSerializer.new
    @object = YARD::Registry.root
  end

  def options; OpenStruct.new end
end

describe YARD::Server::DocServerHelper do
  before do
    @helper = MockDocServerHelper.new
  end

  describe '#url_for' do
    it "should not link to /library/ if single_library = true" do
      @helper.single_library = true
      @helper.url_for(Registry.root).should == "/PREFIX/toplevel"
    end

    it "should return /PREFIX/foo/version if foo has a version" do
      @helper.library = LibraryVersion.new('foo', 'bar')
      @helper.adapter.router.request.version_supplied = true
      @helper.url_for(P('A')).should == '/PREFIX/foo/bar/A'
    end
  end

  describe '#url_for_file' do
    it "should properly link file objects using file/ prefix" do
      file = CodeObjects::ExtraFileObject.new('a/b/FooBar.md', '')
      @helper.url_for_file(file).should == '/PREFIX/foo/file/a/b/FooBar.md'
    end
  end
end