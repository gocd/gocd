require File.join(File.dirname(__FILE__), '..', '..', 'spec_helper')

describe YARD::Parser::Ruby::RubyParser do
  def stmt(stmt)
    YARD::Parser::Ruby::RubyParser.new(stmt, nil).parse.root.first
  end

  def stmts(stmts)
    YARD::Parser::Ruby::RubyParser.new(stmts, nil).parse.root
  end

  def tokenize(stmt)
    YARD::Parser::Ruby::RubyParser.new(stmt, nil).parse.tokens
  end

  describe '#parse' do
    it "should get comment line numbers" do
      s = stmt <<-eof
        # comment
        # comment
        # comment
        def method; end
      eof
      s.comments.should == "comment\ncomment\ncomment"
      s.comments_range.should == (1..3)

      s = stmt <<-eof

        # comment
        # comment
        def method; end
      eof
      s.comments.should == "comment\ncomment"
      s.comments_range.should == (2..3)

      s = stmt <<-eof
        # comment
        # comment

        def method; end
      eof
      s.comments.should == "comment\ncomment"
      s.comments_range.should == (1..2)

      s = stmt <<-eof
        # comment
        def method; end
      eof
      s.comments.should == "comment"
      s.comments_range.should == (1..1)

      s = stmt <<-eof
        def method; end # comment
      eof
      s.comments.should == "comment"
      s.comments_range.should == (1..1)
    end

    it "should only look up to two lines back for comments" do
      s = stmts <<-eof
        # comments

        # comments

        def method; end
      eof
      s[1].comments.should == "comments"

      s = stmts <<-eof
        # comments


        def method; end
      eof
      s[1].comments.should == nil

      ss = stmts <<-eof
        # comments


        def method; end

        # hello
        def method2; end
      eof
      ss[1].comments.should == nil
      ss[2].comments.should == 'hello'
    end

    it "should handle block comment followed by line comment" do
      ss = stmts <<-eof
# comments1

=begin
comments2
=end
# comments3
def hello; end
eof
      ss.last.comments.should == "comments3"
    end

    it "should handle block comment followed by block comment" do
      ss = stmts <<-eof
=begin
comments1
=end
=begin
comments2
=end
def hello; end
eof
      ss.last.comments.strip.should == "comments2"
    end

    it "should handle 1.9 lambda syntax with args" do
      src = "->(a,b,c=1,*args,&block) { hello_world }"
      stmt(src).source.should == src
    end

    it "should handle 1.9 lambda syntax" do
      src = "-> { hello_world }"
      stmt(src).source.should == src
    end

    it "should handle standard lambda syntax" do
      src = "lambda { hello_world }"
      stmt(src).source.should == src
    end

    it "should throw a ParserSyntaxError on invalid code" do
      lambda { stmt("Foo, bar.") }.should raise_error(YARD::Parser::ParserSyntaxError)
    end

    it "should handle bare hashes as method parameters" do
      src = "command :a => 1, :b => 2, :c => 3"
      stmt(src).jump(:command)[1].source.should == ":a => 1, :b => 2, :c => 3"

      src = "command a: 1, b: 2, c: 3"
      stmt(src).jump(:command)[1].source.should == "a: 1, b: 2, c: 3"
    end

    it "should handle source for hash syntax" do
      src = "{ :a => 1, :b => 2, :c => 3 }"
      stmt(src).jump(:hash).source.should == "{ :a => 1, :b => 2, :c => 3 }"
    end

    it "should handle an empty hash" do
      stmt("{}").jump(:hash).source.should == "{}"
    end

    it "new hash label syntax should show label without colon" do
      ast = stmt("{ a: 1 }").jump(:label)
      ast[0].should == "a"
      ast.source.should == "a:"
    end

    it "should handle begin/rescue blocks" do
      ast = stmt("begin; X; rescue => e; Y end").jump(:rescue)
      ast.source.should == "rescue => e; Y end"

      ast = stmt("begin; X; rescue A => e; Y end").jump(:rescue)
      ast.source.should == "rescue A => e; Y end"

      ast = stmt("begin; X; rescue A, B => e; Y end").jump(:rescue)
      ast.source.should == "rescue A, B => e; Y end"
    end

    it "should handle method rescue blocks" do
      ast = stmt("def x; A; rescue Y; B end")
      ast.source.should == "def x; A; rescue Y; B end"
      ast.jump(:rescue).source.should == "rescue Y; B end"
    end

    it "should handle defs with keywords as method name" do
      ast = stmt("# docstring\nclass A;\ndef class; end\nend")
      ast.jump(:class).docstring.should == "docstring"
      ast.jump(:class).line_range.should == (2..4)
    end

    it 'should handle defs with unnamed argument with default values' do
      ast = stmt('def hello(one, two = 2, three = 3) end').jump(:params)
      ast.source.should == 'one, two = 2, three = 3'
    end

    it 'should handle defs with splats' do
      ast = stmt('def hello(one, *two) end').jump(:params)
      ast.source.should == 'one, *two'
    end

    if YARD.ruby2?
      it 'should handle defs with named arguments with default values' do
        ast = stmt('def hello(one, two: 2, three: 3) end').jump(:params)
        ast.source.should == 'one, two: 2, three: 3'
      end
    end

    if NAMED_OPTIONAL_ARGUMENTS
      it 'should handle defs with named arguments without default values' do
        ast = stmt('def hello(one, two:, three:) end').jump(:params)
        ast.source.should == 'one, two:, three:'
      end

      it 'should handle defs with double splats' do
        ast = stmt('def hello(one, **two) end').jump(:params)
        ast.source.should == 'one, **two'
      end
    end

    it "should end source properly on array reference" do
      ast = stmt("AS[0, 1 ]   ")
      ast.source.should == 'AS[0, 1 ]'

      ast = stmt('def x(a = S[1]) end').jump(:params)
      ast.source.should == 'a = S[1]'
    end

    it "should end source properly on if/unless mod" do
      %w(if unless while).each do |mod|
        stmt("A=1 #{mod} true").source.should == "A=1 #{mod} true"
      end
    end

    it "should show proper source for assignment" do
      stmt("A=1").jump(:assign).source.should == "A=1"
    end

    it "should show proper source for a top_const_ref" do
      s = stmt("::\nFoo::Bar")
      s.jump(:top_const_ref).source.should == "::\nFoo"
      s.should be_ref
      s.jump(:top_const_ref).should be_ref
      s.source.should == "::\nFoo::Bar"
      s.line_range.to_a.should == [1, 2]
    end

    it "should show proper source for inline heredoc" do
      src = "def foo\n  foo(<<-XML, 1, 2)\n    bar\n\n  XML\nend"
      s = stmt(src)
      t = tokenize(src)
      s.source.should == src
      t.map {|x| x[1] }.join.should == src
    end

    it "should show proper source for regular heredoc" do
      src = "def foo\n  x = <<-XML\n  Hello \#{name}!\n  Bye!\n  XML\nend"
      s = stmt(src)
      t = tokenize(src)
      s.source.should == src
      t.map {|x| x[1] }.join.should == src
    end

    it "should show proper source for heredoc with comment" do
      src = "def foo\n  x = <<-XML # HI!\n  Hello \#{name}!\n  Bye!\n  XML\nend"
      s = stmt(src)
      t = tokenize(src)
      s.source.should == src
      t.map {|x| x[1] }.join.should == src
    end

    it "should show proper source for string" do
      ["'", '"'].each do |q|
        src = "#{q}hello\n\nworld#{q}"
        s = stmt(src)
        s.jump(:string_content).source.should == "hello\n\nworld"
        s.source.should == src
      end

      src = '("this is a string")'
      stmt(src).jump(:string_literal).source.should == '"this is a string"'
    end

    it "should show proper source for %w() array" do
      src = "%w(\na b c\n d e f\n)"
      stmt(src).jump(:qwords_literal).source.should == src
    end

    it "should show proper source for %w{} array" do
      src = "%w{\na b c\n d e f\n}"
      stmt(src).jump(:array).source.should == src
    end

    it "should parse %w() array in constant declaration" do
      s = stmt(<<-eof)
        class Foo
          FOO = %w( foo bar )
        end
      eof
      s.jump(:qwords_literal).source.should == '%w( foo bar )'
      if RUBY_VERSION >= '1.9.3' # ripper fix: array node encapsulates qwords
        s.jump(:array).source.should == '%w( foo bar )'
      end
    end

    it "should parse %w() array source in object[] parsed context" do
      s = stmts(<<-eof)
        {}[:key]
        FOO = %w( foo bar )
      eof
      s[1].jump(:array).source.should == '%w( foo bar )'
    end

    it "should parse %w() array source in object[]= parsed context" do
      s = stmts(<<-eof)
        {}[:key] = :value
        FOO = %w( foo bar )
      eof
      s[1].jump(:array).source.should == '%w( foo bar )'
    end

    it "should parse [] as array" do
      s = stmt(<<-eof)
        class Foo
          FOO = ['foo', 'bar']
        end
      eof
      s.jump(:array).source.should == "['foo', 'bar']"
    end

    it "should show source for unary minus" do
      stmt("X = - 1").jump(:unary).source.should == '- 1'
    end

    it "should show source for unary exclamation" do
      stmt("X = !1").jump(:unary).source.should == '!1'
    end

    it "should have the correct line range for class/modules" do
      s = stmt(<<-eof)
        class Foo
          def foo; end



          # Ending comment
        end
      eof
      s.jump(:class).line_range.should == (1..7)
    end

    it "should find lone comments" do
      Registry.clear
      ast = YARD.parse_string(<<-eof).enumerator
        class Foo
          ##
          # comment here


          def foo; end

          # end comment
        end
      eof
      comment = ast.first.last.first
      comment.type.should == :comment
      comment.docstring_hash_flag.should be_true
      comment.docstring.strip.should == "comment here"

      ast.first.last.last.type.should == :comment
      ast.first.last.last.docstring.should == "end comment"
    end

    it "should not group comments if they don't begin the line" do
      Registry.clear
      ast = YARD.parse_string(<<-eof).enumerator
        class Foo
          CONST1 = 1 # Comment here
          CONST2 = 2 # Another comment here
        end
      eof
      Registry.at("Foo::CONST1").docstring.should == "Comment here"
      Registry.at("Foo::CONST2").docstring.should == "Another comment here"
    end
  end
end if HAVE_RIPPER
