require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::MixinHandler do
  it "should add includes to modules or classes" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      cBar = rb_define_class("Bar", rb_cObject);
      mBaz = rb_define_module("Baz");
      rb_include_module(cBar, mFoo);
      rb_include_module(mBaz, mFoo);
    eof
    foo = Registry.at('Foo')
    bar = Registry.at('Bar')
    baz = Registry.at('Baz')
    bar.mixins(:instance).should == [foo]
    baz.mixins(:instance).should == [foo]
  end

  it "should add include as proxy if symbol lookup fails" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_include_module(mFoo, mXYZ);
    eof
    foo = Registry.at('Foo')
    foo.mixins(:instance).should == [P('XYZ')]
  end
end
