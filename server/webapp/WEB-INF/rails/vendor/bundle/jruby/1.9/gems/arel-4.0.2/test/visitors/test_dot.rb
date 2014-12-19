require 'helper'

module Arel
  module Visitors
    class TestDot < MiniTest::Unit::TestCase
      def setup
        @visitor = Visitors::Dot.new
      end

      # functions
      [
        Nodes::Sum,
        Nodes::Exists,
        Nodes::Max,
        Nodes::Min,
        Nodes::Avg,
      ].each do |klass|
        define_method("test_#{klass.name.gsub('::', '_')}") do
          op = klass.new(:a, "z")
          @visitor.accept op
        end
      end

      def test_named_function
        func = Nodes::NamedFunction.new 'omg', 'omg'
        @visitor.accept func
      end

      # unary ops
      [
        Arel::Nodes::Not,
        Arel::Nodes::Group,
        Arel::Nodes::On,
        Arel::Nodes::Grouping,
        Arel::Nodes::Offset,
        Arel::Nodes::Ordering,
        Arel::Nodes::Having,
        Arel::Nodes::UnqualifiedColumn,
        Arel::Nodes::Top,
        Arel::Nodes::Limit,
      ].each do |klass|
        define_method("test_#{klass.name.gsub('::', '_')}") do
          op = klass.new(:a)
          @visitor.accept op
        end
      end

      # binary ops
      [
        Arel::Nodes::Assignment,
        Arel::Nodes::Between,
        Arel::Nodes::DoesNotMatch,
        Arel::Nodes::Equality,
        Arel::Nodes::GreaterThan,
        Arel::Nodes::GreaterThanOrEqual,
        Arel::Nodes::In,
        Arel::Nodes::LessThan,
        Arel::Nodes::LessThanOrEqual,
        Arel::Nodes::Matches,
        Arel::Nodes::NotEqual,
        Arel::Nodes::NotIn,
        Arel::Nodes::Or,
        Arel::Nodes::TableAlias,
        Arel::Nodes::Values,
        Arel::Nodes::As,
        Arel::Nodes::DeleteStatement,
        Arel::Nodes::JoinSource,
      ].each do |klass|
        define_method("test_#{klass.name.gsub('::', '_')}") do
          binary = klass.new(:a, :b)
          @visitor.accept binary
        end
      end
    end
  end
end
