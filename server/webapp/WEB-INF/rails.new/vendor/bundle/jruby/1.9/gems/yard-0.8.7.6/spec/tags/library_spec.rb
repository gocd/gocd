require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::Tags::Library do
  def tag(docstring)
    Docstring.new(docstring).tags.first
  end

  describe '#see_tag' do
    it "should take a URL" do
      tag("@see http://example.com").name.should == "http://example.com"
    end

    it "should take an object path" do
      tag("@see String#reverse").name.should == "String#reverse"
    end

    it "should take a description after the url/object" do
      tag = tag("@see http://example.com An Example Site")
      tag.name.should == "http://example.com"
      tag.text.should == "An Example Site"
    end
  end

  describe '.define_tag' do
    it "should allow defining tags with '.' in the name (x.y.z defines method x_y_z)" do
      Tags::Library.define_tag("foo", 'x.y.z')
      Tags::Library.define_tag("foo2", 'x.y.zz', Tags::OverloadTag)
      Tags::Library.instance.method(:x_y_z_tag).should_not be_nil
      Tags::Library.instance.method(:x_y_zz_tag).should_not be_nil
      tag('@x.y.z foo bar').text.should == 'foo bar'
      tag('@x.y.zz foo(bar)').signature.should == 'foo(bar)'
    end
  end
end
