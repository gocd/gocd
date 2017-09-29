# coding: utf-8

require 'astrolabe/node'

module Transpec
  module AST
    class Node < Astrolabe::Node
      attr_reader :metadata

      def initialize(type, children = [], properties = {})
        @metadata = {}
        super
      end
    end
  end
end
