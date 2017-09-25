require File.dirname(__FILE__) + '/spec_helper'

describe YARD::Server::DocServerSerializer do
  describe '#serialized_path' do
    before do
      Registry.clear
      @serializer = Server::DocServerSerializer.new
    end

    after(:all) { Server::Adapter.shutdown }

    it "should return '/PREFIX/library/toplevel' for root" do
      @serializer.serialized_path(Registry.root).should == "toplevel"
    end

    it "should return /PREFIX/library/Object for Object in a library" do
      @serializer.serialized_path(P('A::B::C')).should == 'A/B/C'
    end

    it "should link to instance method as Class:method" do
      obj = CodeObjects::MethodObject.new(:root, :method)
      @serializer.serialized_path(obj).should == 'toplevel:method'
    end

    it "should link to class method as Class.method" do
      obj = CodeObjects::MethodObject.new(:root, :method, :class)
      @serializer.serialized_path(obj).should == 'toplevel.method'
    end

    it "should link to anchor for constant" do
      obj = CodeObjects::ConstantObject.new(:root, :FOO)
      @serializer.serialized_path(obj).should == 'toplevel#FOO-constant'
    end

    it "should link to anchor for class variable" do
      obj = CodeObjects::ClassVariableObject.new(:root, :@@foo)
      @serializer.serialized_path(obj).should == 'toplevel#@@foo-classvariable'
    end

    it "should link files using file/ prefix" do
      file = CodeObjects::ExtraFileObject.new('a/b/FooBar.md', '')
      @serializer.serialized_path(file).should == 'file/FooBar'
    end

    it "should handle unicode data" do
      file = CodeObjects::ExtraFileObject.new("test\u0160", '')
      if file.name.encoding == Encoding.find("Windows-1252")
        @serializer.serialized_path(file).should == 'file/test_8A'
      else
        @serializer.serialized_path(file).should == 'file/test_C5A0'
      end
    end if defined?(::Encoding)
  end
end