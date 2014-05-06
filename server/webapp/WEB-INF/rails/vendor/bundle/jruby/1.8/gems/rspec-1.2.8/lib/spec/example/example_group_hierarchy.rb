module Spec
  module Example
    class ExampleGroupHierarchy < Array
      def initialize(example_group_class)
        push example_group_class
        if example_group_class.respond_to?(:superclass) && example_group_class.superclass.respond_to?(:example_group_hierarchy)
          unshift example_group_class.superclass.example_group_hierarchy
          flatten!
        end
      end

      def run_before_all(example)
        example.eval_each_fail_fast(before_all_parts)
      end

      def run_before_each(example)
        example.eval_each_fail_fast(before_each_parts)
      end

      def run_after_each(example)
        example.eval_each_fail_slow(after_each_parts)
      end

      def run_after_all(example)
        example.eval_each_fail_slow(after_all_parts)
      end

      def before_all_parts
        @before_all_parts ||= collect {|klass| klass.before_all_parts}.flatten
      end

      def before_each_parts
        @before_each_parts ||= collect {|klass| klass.before_each_parts}.flatten
      end

      def after_each_parts
        @after_each_parts ||= reverse.collect {|klass| klass.after_each_parts}.flatten
      end

      def after_all_parts
        @after_all_parts ||= reverse.collect {|klass| klass.after_all_parts}.flatten
      end

      def nested_descriptions
        @nested_descriptions ||= collect {|eg| nested_description_from(eg) == "" ? nil : nested_description_from(eg) }.compact
      end

      def nested_description_from(example_group)
        example_group.description_args.join
      end
    end
  end
end
