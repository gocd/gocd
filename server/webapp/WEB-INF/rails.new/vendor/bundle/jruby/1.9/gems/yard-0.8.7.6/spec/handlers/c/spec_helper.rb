require File.dirname(__FILE__) + "/../spec_helper"

def parse(src, file = '(stdin)')
  YARD::Registry.clear
  parser = YARD::Parser::SourceParser.new(:c)
  parser.file = file
  parser.parse(StringIO.new(src))
end

def parse_init(src)
  YARD::Registry.clear
  YARD.parse_string("void Init_Foo() {\n#{src}\n}", :c)
end
