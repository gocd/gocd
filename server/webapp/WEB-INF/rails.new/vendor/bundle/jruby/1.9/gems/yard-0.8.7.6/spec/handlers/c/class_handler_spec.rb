require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::ClassHandler do
  it "should register classes" do
    parse_init 'cFoo = rb_define_class("Foo", rb_cObject);'
    Registry.at('Foo').type.should == :class
  end

  it "should register classes under namespaces" do
    parse_init 'cFoo = rb_define_class_under(cBar, "Foo", rb_cObject);'
    Registry.at('Bar::Foo').type.should == :class
  end

  it "should remember symbol defined with class" do
    parse_init(<<-eof)
      cXYZ = rb_define_class("Foo", rb_cObject);
      rb_define_method(cXYZ, "bar", bar, 0);
    eof
    Registry.at('Foo').type.should == :class
    Registry.at('Foo#bar').should_not be_nil
  end

  it "should lookup superclass symbol name" do
    parse_init(<<-eof)
      cXYZ = rb_define_class("Foo", rb_cObject);
      cBar = rb_define_class("Bar", cXYZ);
    eof
    Registry.at('Bar').superclass.should == Registry.at('Foo')
  end

  it "should user superclass symbol name as proxy if not found" do
    parse_init(<<-eof)
      // cXYZ = rb_define_class("Foo", rb_cObject);
      cBar = rb_define_class("Bar", cXYZ);
    eof
    Registry.at('Bar').superclass.should == P('XYZ')
  end

  it "should not associate declaration comments as class docstring" do
    parse_init(<<-eof)
      /* Docstring! */
      cFoo = rb_define_class("Foo", cObject);
    eof
    Registry.at('Foo').docstring.should be_blank
  end

  it "should associate a file with the declaration" do
    parse_init(<<-eof)
      cFoo = rb_define_class("Foo", cObject);
    eof
    Registry.at('Foo').file.should == '(stdin)'
    Registry.at('Foo').line.should == 2
  end

  it "should properly handle Proxy superclasses" do
    parse_init <<-eof
      cFoo = rb_define_class_under(mFoo, "Bar", rb_cBar);
    eof
    Registry.at('Foo::Bar').type.should == :class
    Registry.at('Foo::Bar').superclass.should == P('Bar')
    Registry.at('Foo::Bar').superclass.type.should == :class
  end
end
