require File.dirname(__FILE__) + '/spec_helper'

describe YARD::Handlers::C::StructHandler do
  after { Registry.clear }

  it "should handle Struct class definitions" do
    parse_init <<-eof
      rb_cRange = rb_struct_define_without_accessor(
          "Range", rb_cFoo, range_alloc,
          "begin", "end", "excl", NULL);
    eof
    Registry.at('Range').type.should == :class
    Registry.at('Range').superclass.should == P(:Foo)
  end
end