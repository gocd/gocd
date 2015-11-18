require File.dirname(__FILE__) + "/spec_helper"

describe YARD::Handlers::C::MethodHandler do
  it "should register methods" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_method(mFoo, "bar", bar, 0);
    eof
    Registry.at('Foo#bar').should_not be_nil
    Registry.at('Foo#bar').visibility.should == :public
  end

  it "should register private methods" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_private_method(mFoo, "bar", bar, 0);
    eof
    Registry.at('Foo#bar').should_not be_nil
    Registry.at('Foo#bar').visibility.should == :private
  end

  it "should register singleton methods" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_singleton_method(mFoo, "bar", bar, 0);
    eof
    Registry.at('Foo.bar').should_not be_nil
    Registry.at('Foo.bar').visibility.should == :public
  end

  it "should register module functions" do
    parse <<-eof
      /* DOCSTRING
       * @return [String] foo!
      */
      static VALUE bar(VALUE self) { x(); y(); z(); }

      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_module_function(mFoo, "bar", bar, 0);
      }
    eof
    bar_c = Registry.at('Foo.bar')
    bar_i = Registry.at('Foo#bar')
    bar_c.should be_module_function
    bar_c.visibility.should == :public
    bar_c.docstring.should == "DOCSTRING"
    bar_c.tag(:return).object.should == bar_c
    bar_c.source.should == "static VALUE bar(VALUE self) { x(); y(); z(); }"
    bar_i.should_not be_module_function
    bar_i.visibility.should == :private
    bar_i.docstring.should == "DOCSTRING"
    bar_i.tag(:return).object.should == bar_i
    bar_i.source.should == bar_c.source
  end

  it "should register global functions into Kernel" do
    parse_init 'rb_define_global_function("bar", bar, 0);'
    Registry.at('Kernel#bar').should_not be_nil
  end

  it "should look for symbol containing method source" do
    parse <<-eof
      static VALUE foo(VALUE self) { x(); y(); z(); }
      VALUE bar() { a(); b(); c(); }
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo", foo, 0);
        rb_define_method(mFoo, "bar", bar, 0);
      }
    eof
    foo = Registry.at('Foo#foo')
    bar = Registry.at('Foo#bar')
    foo.source.should == "static VALUE foo(VALUE self) { x(); y(); z(); }"
    foo.file.should == '(stdin)'
    foo.line.should == 1
    bar.source.should == "VALUE bar() { a(); b(); c(); }"
    bar.file.should == '(stdin)'
    bar.line.should == 2
  end

  it "should find docstrings attached to method symbols" do
    parse <<-eof
      /* DOCSTRING */
      static VALUE foo(VALUE self) { x(); y(); z(); }
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo", foo, 0);
      }
    eof
    foo = Registry.at('Foo#foo')
    foo.docstring.should == 'DOCSTRING'
  end

  it "should use declaration comments as docstring if there are no others" do
    parse <<-eof
      static VALUE foo(VALUE self) { x(); y(); z(); }
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        /* DOCSTRING */
        rb_define_method(mFoo, "foo", foo, 0);
        // DOCSTRING!
        rb_define_method(mFoo, "bar", bar, 0);
      }
    eof
    foo = Registry.at('Foo#foo')
    foo.docstring.should == 'DOCSTRING'
    bar = Registry.at('Foo#bar')
    bar.docstring.should == 'DOCSTRING!'
  end

  it "should look for symbols in other file" do
    other = <<-eof
      /* DOCSTRING! */
      static VALUE foo() { x(); }
    eof
    File.should_receive(:read).with('other.c').and_return(other)
    parse <<-eof
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo", foo, 0); // in other.c
      }
    eof
    foo = Registry.at('Foo#foo')
    foo.docstring.should == 'DOCSTRING!'
    foo.file.should == 'other.c'
    foo.line.should == 2
    foo.source.should == 'static VALUE foo() { x(); }'
  end

  it "should allow extra file to include /'s and other filename characters" do
    File.should_receive(:read).at_least(1).times.with('ext/a-file.c').and_return(<<-eof)
      /* FOO */
      VALUE foo(VALUE x) { int value = x; }

      /* BAR */
      VALUE bar(VALUE x) { int value = x; }
    eof
    parse_init <<-eof
      rb_define_method(rb_cFoo, "foo", foo, 1); /* in ext/a-file.c */
      rb_define_global_function("bar", bar, 1); /* in ext/a-file.c */
    eof
    Registry.at('Foo#foo').docstring.should == 'FOO'
    Registry.at('Kernel#bar').docstring.should == 'BAR'
  end

  it "should warn if other file can't be found" do
    log.should_receive(:warn).with(/Missing source file `other.c' when parsing Foo#foo/)
    parse <<-eof
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo", foo, 0); // in other.c
      }
    eof
  end

  it "should look at override comments for docstring" do
    parse <<-eof
      /* Document-method: Foo::foo
       * Document-method: new
       * Document-method: Foo::Bar#baz
       * Foo bar!
       */

      // init comments
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo", foo, 0);
        rb_define_method(mFoo, "initialize", foo, 0);
        mBar = rb_define_module_under(mFoo, "Bar");
        rb_define_method(mBar, "baz", foo, 0);
      }
    eof
    Registry.at('Foo#foo').docstring.should == 'Foo bar!'
    Registry.at('Foo#initialize').docstring.should == 'Foo bar!'
    Registry.at('Foo::Bar#baz').docstring.should == 'Foo bar!'
  end

  it "should look at overrides in other files" do
    other = <<-eof
      /* Document-method: Foo::foo
       * Document-method: new
       * Document-method: Foo::Bar#baz
       * Foo bar!
       */
    eof
    File.should_receive(:read).with('foo/bar/other.c').and_return(other)
    src = <<-eof
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo", foo, 0); // in foo/bar/other.c
        rb_define_method(mFoo, "initialize", foo, 0); // in foo/bar/other.c
        mBar = rb_define_module_under(mFoo, "Bar"); // in foo/bar/other.c
        rb_define_method(mBar, "baz", foo, 0); // in foo/bar/other.c
      }
    eof
    parse(src, 'foo/bar/baz/init.c')
    Registry.at('Foo#foo').docstring.should == 'Foo bar!'
    Registry.at('Foo#initialize').docstring.should == 'Foo bar!'
    Registry.at('Foo::Bar#baz').docstring.should == 'Foo bar!'
  end

  it "should add return tag on methods ending in '?'" do
    parse <<-eof
      /* DOCSTRING */
      static VALUE foo(VALUE self) { x(); y(); z(); }
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo?", foo, 0);
      }
    eof
    foo = Registry.at('Foo#foo?')
    foo.docstring.should == 'DOCSTRING'
    foo.tag(:return).types.should == ['Boolean']
  end

  it "should not add return tag if return tags exist" do
    parse <<-eof
      // @return [String] foo
      static VALUE foo(VALUE self) { x(); y(); z(); }
      void Init_Foo() {
        mFoo = rb_define_module("Foo");
        rb_define_method(mFoo, "foo?", foo, 0);
      }
    eof
    foo = Registry.at('Foo#foo?')
    foo.tag(:return).types.should == ['String']
  end

  it "should handle casted method names" do
    parse_init <<-eof
      mFoo = rb_define_module("Foo");
      rb_define_method(mFoo, "bar", (METHOD)bar, 0);
      rb_define_global_function("baz", (METHOD)baz, 0);
    eof
    Registry.at('Foo#bar').should_not be_nil
    Registry.at('Kernel#baz').should_not be_nil
  end
end
