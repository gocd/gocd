# frozen_string_literal: true
require 'capybara/selector/filter'

module Capybara
  class Selector
    class FilterSet
      attr_reader :descriptions

      def initialize(name, &block)
        @name = name
        @descriptions = []
        instance_eval(&block)
      end

      def filter(name, *types_and_options, &block)
        add_filter(name, Filters::NodeFilter, *types_and_options, &block)
      end

      def expression_filter(name, *types_and_options, &block)
        add_filter(name, Filters::ExpressionFilter, *types_and_options, &block)
      end

      def describe(&block)
        descriptions.push block
      end

      def description(options={})
        options_with_defaults = options.dup
        filters.each do |name, filter|
          options_with_defaults[name] = filter.default if filter.default? && !options_with_defaults.has_key?(name)
        end

        @descriptions.map do |desc|
          desc.call(options_with_defaults).to_s
        end.join
      end

      def filters
        @filters ||= {}
      end

      def node_filters
        filters.reject { |_n, f| f.nil? || f.is_a?(Filters::ExpressionFilter) }.freeze
      end

      def expression_filters
        filters.select { |_n, f| f.nil? || f.is_a?(Filters::ExpressionFilter)  }.freeze
      end

      class << self

        def all
          @filter_sets ||= {}
        end

        def add(name, &block)
          all[name.to_sym] = FilterSet.new(name.to_sym, &block)
        end

        def remove(name)
          all.delete(name.to_sym)
        end
      end

      private

      def add_filter(name, filter_class, *types_and_options, &block)
        options = types_and_options.last.is_a?(Hash) ? types_and_options.pop.dup : {}
        types_and_options.each { |k| options[k] = true}
        filters[name] = filter_class.new(name, block, options)
      end
    end
  end
end