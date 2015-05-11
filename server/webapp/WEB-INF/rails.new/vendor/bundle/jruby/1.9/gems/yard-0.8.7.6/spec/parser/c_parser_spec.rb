require File.join(File.dirname(__FILE__), '..', 'spec_helper')

describe YARD::Parser::C::CParser do
  describe '#parse' do
    def parse(contents)
      Registry.clear
      YARD.parse_string(contents, :c)
    end

    describe 'Array class' do
      before(:all) do
        file = File.join(File.dirname(__FILE__), 'examples', 'array.c.txt')
        parse(File.read(file))
      end

      it "should parse Array class" do
        obj = YARD::Registry.at('Array')
        obj.should_not be_nil
        obj.docstring.should_not be_blank
      end

      it "should parse method" do
        obj = YARD::Registry.at('Array#initialize')
        obj.docstring.should_not be_blank
        obj.tags(:overload).size.should > 1
      end
    end

    describe 'Source located in extra files' do
      before(:all) do
        @multifile = File.join(File.dirname(__FILE__), 'examples', 'multifile.c.txt')
        @extrafile = File.join(File.dirname(__FILE__), 'examples', 'extrafile.c.txt')
        @contents = File.read(@multifile)
      end

      it "should look for methods in extra files (if 'in' comment is found)" do
        extra_contents = File.read(@extrafile)
        File.should_receive(:read).with('extra.c').and_return(extra_contents)
        parse(@contents)
        Registry.at('Multifile#extra').docstring.should == 'foo'
      end

      it "should stop searching for extra source file gracefully if file is not found" do
        File.should_receive(:read).with('extra.c').and_raise(Errno::ENOENT)
        log.should_receive(:warn).with("Missing source file `extra.c' when parsing Multifile#extra")
        parse(@contents)
        Registry.at('Multifile#extra').docstring.should == ''
      end

      it "should differentiate between a struct and a pointer to a struct retval" do
        parse(@contents)
        Registry.at('Multifile#hello_mars').docstring.should == 'Hello Mars'
      end
    end

    describe 'Foo class' do
      it 'should not include comments in docstring source' do
        parse <<-eof
          /*
           * Hello world
           */
          VALUE foo(VALUE x) {
            int value = x;
          }

          void Init_Foo() {
            rb_define_method(rb_cFoo, "foo", foo, 1);
          }
        eof
        Registry.at('Foo#foo').source.gsub(/\s\s+/, ' ').should ==
          "VALUE foo(VALUE x) { int value = x;\n}"
      end
    end

    describe 'Constant' do
      it 'should not truncate docstring' do
        parse <<-eof
          #define MSK_DEADBEEF 0xdeadbeef
          void
          Init_Mask(void)
          {
              rb_cMask  = rb_define_class("Mask", rb_cObject);
              /* 0xdeadbeef: This constant is frequently used to indicate a
               * software crash or deadlock in embedded systems. */
              rb_define_const(rb_cMask, "DEADBEEF", INT2FIX(MSK_DEADBEEF));
          }
        eof
        constant = Registry.at('Mask::DEADBEEF')
        constant.value.should == '0xdeadbeef'
        constant.docstring.should == "This constant is frequently used to indicate a\nsoftware crash or deadlock in embedded systems."
      end
    end

    describe 'Macros' do
      it "should handle param## inside of macros" do
        thr = Thread.new do
          parse <<-eof
          void
          Init_gobject_gparamspecs(void)
          {
              VALUE cParamSpec = GTYPE2CLASS(G_TYPE_PARAM);
              VALUE c;

          #define DEF_NUMERIC_PSPEC_METHODS(c, typename) \
            G_STMT_START {\
              rbg_define_method(c, "initialize", typename##_initialize, 7); \
              rbg_define_method(c, "minimum", typename##_minimum, 0); \
              rbg_define_method(c, "maximum", typename##_maximum, 0); \
              rbg_define_method(c, "range", typename##_range, 0); \
            } G_STMT_END

          #if 0
              rbg_define_method(c, "default_value", typename##_default_value, 0); \
              rb_define_alias(c, "default", "default_value"); \

          #endif

              c = G_DEF_CLASS(G_TYPE_PARAM_CHAR, "Char", cParamSpec);
              DEF_NUMERIC_PSPEC_METHODS(c, char);
          eof
        end
        thr.join(5)
        if thr.alive?
          fail "Did not parse in time"
          thr.kill
        end
      end
    end

    describe 'C macros in declaration' do
      it "should handle C macros in method declaration" do
        Registry.clear
        parse <<-eof
        // docstring
        FOOBAR VALUE func() { }

        void
        Init_mod(void)
        {
          rb_define_method(rb_cFoo, "func", func, 0); \
        }
        eof

        Registry.at('Foo#func').docstring.should == "docstring"
      end
    end
  end

  describe 'Override comments' do
    before(:all) do
      Registry.clear
      override_file = File.join(File.dirname(__FILE__), 'examples', 'override.c.txt')
      @override_parser = YARD.parse_string(File.read(override_file), :c)
    end

    it "should parse GMP::Z class" do
      z = YARD::Registry.at('GMP::Z')
      z.should_not be_nil
      z.docstring.should_not be_blank
    end

    it "should parse GMP::Z methods w/ bodies" do
      add = YARD::Registry.at('GMP::Z#+')
      add.docstring.should_not be_blank
      add.source.should_not be_nil
      add.source.should_not be_empty

      add_self = YARD::Registry.at('GMP::Z#+')
      add_self.docstring.should_not be_blank
      add_self.source.should_not be_nil
      add_self.source.should_not be_empty

      sqrtrem = YARD::Registry.at('GMP::Z#+')
      sqrtrem.docstring.should_not be_blank
      sqrtrem.source.should_not be_nil
      sqrtrem.source.should_not be_empty
    end

    it "should parse GMP::Z methods w/o bodies" do
      neg = YARD::Registry.at('GMP::Z#neg')
      neg.docstring.should_not be_blank
      neg.source.should be_nil

      neg_self = YARD::Registry.at('GMP::Z#neg')
      neg_self.docstring.should_not be_blank
      neg_self.source.should be_nil
    end
  end
end
