require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::PathHandler do
  it 'should track variable names defined under namespaces' do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      cBar = rb_define_class_under(mFoo, "Bar", rb_cObject);
      rb_define_method(cBar, "foo", foo, 1);
    eof
    Registry.at('Foo::Bar').should_not be_nil
    Registry.at('Foo::Bar#foo').should_not be_nil
  end

  it 'should track variable names defined under namespaces' do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      cBar = rb_define_class_under(mFoo, "Bar", rb_cObject);
      mBaz = rb_define_module_under(cBar, "Baz");
      rb_define_method(mBaz, "foo", foo, 1);
    eof
    Registry.at('Foo::Bar::Baz').should_not be_nil
    Registry.at('Foo::Bar::Baz#foo').should_not be_nil
  end

  it "should handle rb_path2class() calls" do
    parse_init <<-eof
      somePath = rb_path2class("Foo::Bar::Baz")
      mFoo = rb_define_module("Foo");
      cBar = rb_define_class_under(mFoo, "Bar", rb_cObject);
      mBaz = rb_define_module_under(cBar, "Baz");
      rb_define_method(somePath, "foo", foo, 1);
    eof
    Registry.at('Foo::Bar::Baz#foo').should_not be_nil
  end
end
