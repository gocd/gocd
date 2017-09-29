# coding: utf-8

require 'astrolabe/node'
require 'parser'

module Astrolabe
  # `Astrolabe::Builder` is an AST builder that is utilized to let `Parser` generate AST with
  # {Astrolabe::Node}.
  #
  # @example
  #   require 'astrolabe/builder'
  #   require 'parser/current'
  #
  #   buffer = Parser::Source::Buffer.new('(string)')
  #   buffer.source = 'puts :foo'
  #
  #   builder = Astrolabe::Builder.new
  #   parser = Parser::CurrentRuby.new(builder)
  #   root_node = parser.parse(buffer)
  class Builder < Parser::Builders::Default
    # Generates {Node} from the given information.
    #
    # @return [Node] the generated node
    def n(type, children, source_map)
      Node.new(type, children, location: source_map)
    end
  end
end
