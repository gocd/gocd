# coding: utf-8

shared_context 'AST', :ast do
  let(:root_node) do
    fail '#source must be defined with #let' unless respond_to?(:source)

    require 'astrolabe/builder'
    require 'parser/current'

    buffer = Parser::Source::Buffer.new('(string)')
    buffer.source = source

    builder = Astrolabe::Builder.new
    parser = Parser::CurrentRuby.new(builder)
    parser.parse(buffer)
  end
end
