# encoding: UTF-8
require 'stringio'
require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Uglifier" do
  it "minifies JS" do
    source = File.open("lib/uglify.js", "r:UTF-8").read
    minified = Uglifier.new.compile(source)
    expect(minified.length).to be < source.length
    expect { Uglifier.new.compile(minified) }.not_to raise_error
  end

  it "throws an exception when compilation fails" do
    expect { Uglifier.new.compile(")(") }.to raise_error(Uglifier::Error)
  end

  it "throws an exception on invalid option" do
    expect { Uglifier.new(:foo => true) }.to raise_error(ArgumentError)
  end

  it "doesn't omit null character in strings" do
    expect(Uglifier.new.compile('var foo="\0bar"')).to include("\\x00bar")
  end

  it "adds trailing semicolon to minified source" do
    source = "(function id(i) {return i;}());"
    expect(Uglifier.new.compile(source)[-1]).to eql(";"[0])
  end

  describe "argument name mangling" do
    it "doesn't try to mangle $super by default to avoid breaking PrototypeJS" do
      expect(Uglifier.compile('function foo($super) {return $super}')).to include("$super")
    end

    it "allows variables to be excluded from mangling" do
      code = "function bar(foo) {return foo + 'bar'};"
      expect(Uglifier.compile(code, :mangle => { :except => ["foo"] })).to include("(foo)")
    end

    it "skips mangling when set to false" do
      code = "function bar(foo) {return foo + 'bar'};"
      expect(Uglifier.compile(code, :mangle => false)).to include("(foo)")
    end

    it "mangles argumen names by default" do
      code = "function bar(foo) {return foo + 'bar'};"
      expect(Uglifier.compile(code, :mangle => true)).not_to include("(foo)")
    end

    it "mangles top-level names when explicitly instructed" do
      code = "function bar(foo) {return foo + 'bar'};"
      expect(Uglifier.compile(code, :mangle => { :toplevel => false })).to include("bar(")
      expect(Uglifier.compile(code, :mangle => { :toplevel => true })).not_to include("bar(")
    end
  end

  describe "comment preservation" do
    let(:source) do
      <<-EOS
        /* @preserve Copyright Notice */
        /* (c) 2011 */
        // INCLUDED
        function identity(p) { return p; }
        /* Another Copyright */
        function add(a, b) { return a + b; }
      EOS
    end

    it "handles copyright option" do
      compiled = Uglifier.compile(source, :copyright => false)
      expect(compiled).not_to match(/Copyright/)
    end

    describe ":copyright" do
      subject { Uglifier.compile(source, :comments => :copyright) }

      it "preserves comments with string Copyright" do
        expect(subject).to match(/Copyright Notice/)
        expect(subject).to match(/Another Copyright/)
      end

      it "ignores other comments" do
        expect(subject).not_to match(/INCLUDED/)
      end
    end

    describe ":jsdoc" do
      subject { Uglifier.compile(source, :output => { :comments => :jsdoc }) }

      it "preserves jsdoc license/preserve blocks" do
        expect(subject).to match(/Copyright Notice/)
      end

      it "ignores other comments" do
        expect(subject).not_to match(/Another Copyright/)
      end
    end

    describe ":all" do
      subject { Uglifier.compile(source, :comments => :all) }

      it "preserves all comments" do
        expect(subject).to match(/INCLUDED/)
        expect(subject).to match(/2011/)
      end
    end

    describe ":none" do
      subject { Uglifier.compile(source, :comments => :none) }

      it "omits all comments" do
        expect(subject).not_to match %r{//}
        expect(subject).not_to match(/\/\*/)
      end
    end

    describe "regular expression" do
      subject { Uglifier.compile(source, :comments => /included/i) }

      it "matches comment blocks with regex" do
        expect(subject).to match(/INCLUDED/)
      end

      it "omits other blocks" do
        expect(subject).not_to match(/2011/)
      end
    end
  end

  it "squeezes code only if squeeze is set to true" do
    code = "function a(a){if(a) { return 0; } else { return 1; }}"
    minified = Uglifier.compile(code, :squeeze => false)
    squeezed = Uglifier.compile(code, :squeeze => true)
    expect(minified.length).to be > squeezed.length
  end

  it "honors max line length" do
    code = "var foo = 123;function bar() { return foo; }"
    uglifier = Uglifier.new(:output => { :max_line_len => 16 }, :compress => false)
    expect(uglifier.compile(code).split("\n").length).to eq(2)
  end

  it "hoists vars to top of the scope" do
    code = "function something() { var a = 1; a = 2; var b = 3; return a + b;}"
    minified = Uglifier.compile(code, :compress => { :hoist_vars => true })
    expect(minified).to match(/var \w,\w/)
  end

  it "forwards screw_ie8 option to UglifyJS" do
    code = "function something() { return g['switch']; }"
    expect(Uglifier.compile(code, :mangle => false, :screw_ie8 => true)).to match(/g\.switch/)
    expect(Uglifier.compile(code, :compress => false, :screw_ie8 => true)).to match(/g\.switch/)
  end

  it "can be configured to output only ASCII" do
    code = "function emoji() { return '\\ud83c\\ude01'; }"
    minified = Uglifier.compile(code, :output => { :ascii_only => true })
    expect(minified).to include("\\ud83c\\ude01")
  end

  it "escapes </script when asked to" do
    code = "function test() { return '</script>';}"
    minified = Uglifier.compile(code, :output => { :inline_script => true })
    expect(minified).not_to include("</script>")
  end

  it "quotes keys" do
    code = "var a = {foo: 1}"
    minified = Uglifier.compile(code, :output => { :quote_keys => true })
    expect(minified).to include('"foo"')
  end

  it "quotes unsafe keys by default" do
    code = 'var code = {"class": "", "\u200c":"A"}'
    expect(Uglifier.compile(code)).to include('"class"')
    expect(Uglifier.compile(code)).to include('"\u200c"')

    uglifier = Uglifier.new(:output => { :ascii_only => false, :quote_keys => false })
    expect(uglifier.compile(code)).to include(["200c".to_i(16)].pack("U*"))
  end

  it "handles constant definitions" do
    code = "if (BOOL) { var a = STR; var b = NULL; var c = NUM; }"
    defines = { "NUM" => 1234, "BOOL" => true, "NULL" => nil, "STR" => "str" }
    processed = Uglifier.compile(code, :define => defines)
    expect(processed).to include("a=\"str\"")
    expect(processed).not_to include("if")
    expect(processed).to include("b=null")
    expect(processed).to include("c=1234")
  end

  it "can disable IIFE negation" do
    code = "(function() { console.log('test')})();"
    disabled_negation = Uglifier.compile(code, :compress => { :negate_iife => false })
    expect(disabled_negation).not_to include("!")
    negation = Uglifier.compile(code, :compress => { :negate_iife => true })
    expect(negation).to include("!")
  end

  it "can drop console logging" do
    code = "(function() { console.log('test')})();"
    compiled = Uglifier.compile(code, :compress => { :drop_console => true })
    expect(compiled).not_to include("console")
  end

  it "processes @ngInject annotations" do
    code = <<-EOF
    /**
     * @ngInject
     */
    var f = function(foo, bar) { return foo + bar};
    EOF
    with_angular = Uglifier.compile(code, :compress => { :angular => true })
    without_angular = Uglifier.compile(code, :compress => { :angular => false })
    expect(with_angular).to include("f.$inject")
    expect(without_angular).not_to include("f.$inject")
  end

  it "keeps unused function arguments when keep_fargs option is set" do
    code = <<-EOF
    function plus(a, b, c) { return a + b};
    plus(1, 2);
    EOF
    expect(Uglifier.compile(code, :mangle => false)).not_to include("c)")

    keep_fargs = Uglifier.compile(code, :mangle => false, :compress => { :keep_fargs => true })
    expect(keep_fargs).to include("c)")

  end

  describe "Input Formats" do
    let(:code) { "function hello() { return 'hello world'; }" }

    it "handles strings" do
      expect(Uglifier.new.compile(code)).not_to be_empty
    end

    it "handles IO objects" do
      expect(Uglifier.new.compile(StringIO.new(code))).not_to be_empty
    end
  end

  describe "enclose" do
    let(:code) { "$.foo()" }

    it "encloses code with given arguments" do
      minified = Uglifier.compile(code, :enclose => { 'window.jQuery' => '$' })
      expect(minified).to match(/window.jQuery/)
    end

    it "handles multiple definitions" do
      definitions = [%w(lol lulz), %w(foo bar)]
      minified = Uglifier.compile(code, :enclose => definitions)
      expect(minified).to match(/lol,foo/)
      expect(minified).to match(/lulz,bar/)
    end

    it "wraps with function when given empty object" do
      minified = Uglifier.compile(code, :enclose => {})
      expect(minified).to match(/function\(/)
    end
  end
end
