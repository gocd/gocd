# coding: utf-8

require_relative 'benchmark_helper'
require 'astrolabe/node'
require 'parser/current'

describe 'performance' do
  Benchmarking.warm_up = true
  Benchmarking.trial_count = 3
  Benchmarking.loop_count_in_trial = 1000

  def generate_source(max, current_nest_level = 1)
    <<-END
    class SomeClass#{current_nest_level}
      def some_method(foo)
        do_something do |bar|
        end
      end

      #{generate_source(max, current_nest_level + 1) if current_nest_level < max}
    end
    END
  end

  let(:source) { generate_source(100) }

  def create_builder_class(node_class)
    Class.new(Parser::Builders::Default) do
      define_method(:n) do |type, children, source_map|
        node_class.new(type, children, location: source_map)
      end
    end
  end

  def ast_with_node_class(node_class)
    buffer = Parser::Source::Buffer.new('(string)')
    buffer.source = source
    builder = create_builder_class(node_class).new
    parser = Parser::CurrentRuby.new(builder)
    parser.parse(buffer)
  end

  describe 'Node#each_descendant' do
    class EachDescendantBenchmark < Benchmarking
      def initialize(name, root_node)
        @name = name
        @root_node = root_node
      end

      def run
        count = 0

        @root_node.each_descendant do
          count += 1
        end

        fail if count == 0
      end
    end

    let(:current_implementation) do
      EachDescendantBenchmark.new('current implementation', ast_with_node_class(Astrolabe::Node))
    end

    describe 'nested-yield vs. block-pass vs. proc-pass' do
      let(:nested_yield) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant
            return to_enum(__method__) unless block_given?

            children.each do |child|
              next unless child.is_a?(self.class)
              yield child
              child.each_descendant { |node| yield node }
            end
          end
        end

        EachDescendantBenchmark.new('nested-yield', ast_with_node_class(node_class))
      end

      let(:block_pass) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?

            children.each do |child|
              next unless child.is_a?(self.class)
              yield child
              child.each_descendant(&block)
            end
          end
        end

        EachDescendantBenchmark.new('block-pass', ast_with_node_class(node_class))
      end

      let(:proc_pass) do
        node_class = Class.new(Astrolabe::Node) do
          # Yeah, this is ugly interface, but just for reference.
          def each_descendant(proc_object = nil, &block)
            return to_enum(__method__) if !block_given? && !proc_object

            proc_object ||= block

            children.each do |child|
              next unless child.is_a?(self.class)
              proc_object.call(child)
              child.each_descendant(proc_object)
            end
          end
        end

        EachDescendantBenchmark.new('proc-pass', ast_with_node_class(node_class))
      end

      specify 'block-pass is obviously (at least 4 times) faster than nested-yield' do
        expect(block_pass).to be_faster_than(nested_yield).at_least(4).times
      end

      specify 'block-pass is a bit faster than proc-pass' do
        expect(block_pass).to be_faster_than(proc_pass)
      end

      describe 'current implementation' do
        it 'is as fast as block-pass' do
          expect(current_implementation).to be_as_fast_as(block_pass)
        end
      end
    end

    describe 'inline code vs. delegation to #each_child_node' do
      let(:inline_code) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?

            children.each do |child|
              next unless child.is_a?(self.class)
              yield child
              child.each_descendant(&block)
            end

            self
          end
        end

        EachDescendantBenchmark.new('inline code', ast_with_node_class(node_class))
      end

      let(:delegation) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?

            each_child_node do |child_node|
              yield child_node
              child_node.each_descendant(&block)
            end
          end
        end

        EachDescendantBenchmark.new('delegation', ast_with_node_class(node_class))
      end

      specify 'inline code is relatively faster than delegation' do
        expect(inline_code).to be_faster_than(delegation)
      end

      describe 'current implementation' do
        it 'is as fast as inline code' do
          expect(current_implementation).to be_as_fast_as(inline_code)
        end
      end
    end

    describe 'Array#each vs. for-loop' do
      let(:array_each) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?

            children.each do |child|
              next unless child.is_a?(self.class)
              yield child
              child.each_descendant(&block)
            end

            self
          end
        end

        EachDescendantBenchmark.new('Array#each', ast_with_node_class(node_class))
      end

      let(:for_loop) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?

            for child in children # rubocop:disable For
              next unless child.is_a?(self.class)
              yield child
              child.each_descendant(&block)
            end

            self
          end
        end

        EachDescendantBenchmark.new('for-loop', ast_with_node_class(node_class))
      end

      specify "there's no obvious difference between them" do
        expect(array_each).to be_as_fast_as(for_loop)
      end

      describe 'current implementation' do
        it 'is as fast as Array#each' do
          expect(current_implementation).to be_as_fast_as(array_each)
        end
      end
    end

    describe 'pure recursion vs. combination of entry and recursion methods' do
      let(:pure_recursion) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?

            children.each do |child|
              next unless child.is_a?(self.class)
              yield child
              child.each_descendant(&block)
            end

            self
          end
        end

        EachDescendantBenchmark.new('pure recursion', ast_with_node_class(node_class))
      end

      let(:combination) do
        node_class = Class.new(Astrolabe::Node) do
          def each_descendant(&block)
            return to_enum(__method__) unless block_given?
            visit_descendants(&block)
            self
          end

          def visit_descendants(&block)
            children.each do |child|
              next unless child.is_a?(self.class)
              yield child
              child.visit_descendants(&block)
            end
          end
        end

        EachDescendantBenchmark.new('combination', ast_with_node_class(node_class))
      end

      specify 'combination is faster than pure recursion' do
        expect(combination).to be_faster_than(pure_recursion)
      end

      describe 'current implementation' do
        it 'is as fast as combination' do
          expect(current_implementation).to be_as_fast_as(combination)
        end
      end
    end
  end

  describe 'Node#each_ancestor' do
    class EachAncestorBenchmark < Benchmarking
      def initialize(name, root_node)
        @name = name
        @deepest_node = root_node.each_node.max_by { |node| node.each_ancestor.count }
      end

      def run
        count = 0

        @deepest_node.each_ancestor do
          count += 1
        end

        fail if count == 0
      end
    end

    let(:current_implementation) do
      EachAncestorBenchmark.new('current implementation', ast_with_node_class(Astrolabe::Node))
    end

    describe 'recursion vs. while-loop' do
      let(:while_loop) do
        node_class = Class.new(Astrolabe::Node) do
          def each_ancestor
            return to_enum(__method__) unless block_given?

            last_node = self

            while (current_node = last_node.parent)
              yield current_node
              last_node = current_node
            end

            self
          end
        end

        EachAncestorBenchmark.new('while-loop', ast_with_node_class(node_class))
      end

      let(:recursion) do
        node_class = Class.new(Astrolabe::Node) do
          def each_ancestor(&block)
            return to_enum(__method__) unless block_given?

            if parent
              yield parent
              parent.each_ancestor(&block)
            end

            self
          end
        end

        EachAncestorBenchmark.new('recursion', ast_with_node_class(node_class))
      end

      specify 'while-loop is faster than recursion' do
        expect(while_loop).to be_faster_than(recursion)
      end

      describe 'current implementation' do
        it 'is as fast as while-loop' do
          expect(current_implementation).to be_as_fast_as(while_loop)
        end
      end
    end
  end
end
