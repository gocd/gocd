module Spec
  module Example

    module ExampleGroupMethods
      class << self
        attr_accessor :matcher_class

        def build_description_from(*args)
          text = args.inject("") do |description, arg|
            description << " " unless (description == "" || arg.to_s =~ /^(\s|\.|#)/)
            description << arg.to_s
          end
          text == "" ? nil : text
        end
      end

      include Spec::Example::BeforeAndAfterHooks
      include Spec::Example::Subject::ExampleGroupMethods
      include Spec::Example::PredicateMatchers
      include Spec::Example::ArgsAndOptions

      attr_reader :location

      def options # :nodoc:
        @options ||= {}
      end

      def inherited(klass) # :nodoc:
        super
        ExampleGroupFactory.register_example_group(klass)
      end

      # Makes the describe/it syntax available from a class. For example:
      #
      #   class StackSpec < Spec::ExampleGroup
      #     describe Stack, "with no elements"
      #
      #     before
      #       @stack = Stack.new
      #     end
      #
      #     it "should raise on pop" do
      #       lambda{ @stack.pop }.should raise_error
      #     end
      #   end
      #
      def describe(*args, &example_group_block)
        raise Spec::Example::NoDescriptionError.new("example group", caller(0)[1]) if args.empty?
        if example_group_block
          options = add_options(args)
          set_location(options, caller(0)[1])
          if options[:shared]
            ExampleGroupFactory.create_shared_example_group(*args, &example_group_block)
          else
            subclass(*args, &example_group_block)
          end
        else
          set_description(*args)
        end
      end
      alias :context :describe

      # Use this to pull in examples from shared example groups.
      def it_should_behave_like(*shared_example_groups)
        shared_example_groups.each do |group|
          include_shared_example_group(group)
        end
      end

      # Creates an instance of the current example group class and adds it to
      # a collection of examples of the current example group.
      def example(description=nil, options={}, backtrace=nil, &implementation)
        example_proxy = ExampleProxy.new(description, options, backtrace || caller(0)[1])
        example_proxies << example_proxy
        example_implementations[example_proxy] = implementation || pending_implementation
        example_proxy
      end

      def pending_implementation
        lambda { raise(Spec::Example::NotYetImplementedError) }
      end

      alias_method :it, :example
      alias_method :specify, :example

      # Use this to temporarily disable an example.
      def xexample(description=nil, opts={}, &block)
        Kernel.warn("Example disabled: #{description}")
      end

      alias_method :xit, :xexample
      alias_method :xspecify, :xexample

      def run(run_options)
        examples = examples_to_run(run_options)
        notify(run_options.reporter) unless examples.empty?
        return true if examples.empty?
        return dry_run(examples, run_options) if run_options.dry_run?

        define_methods_from_predicate_matchers

        success, before_all_instance_variables = run_before_all(run_options)
        success, after_all_instance_variables  = run_examples(success, before_all_instance_variables, examples, run_options)
        success                                = run_after_all(success, after_all_instance_variables, run_options)
      end

      def set_description(*args)
        @description_args, @options = args_and_options(*args)
        @backtrace = caller(1)
        @location = File.expand_path(options[:location]) if options[:location]
        self
      end

      def notify(reporter) # :nodoc:
        reporter.example_group_started(ExampleGroupProxy.new(self))
      end

      def description
        @description ||= ExampleGroupMethods.build_description_from(*description_parts) || to_s
      end

      def described_type
        @described_type ||= description_parts.reverse.find {|part| part.is_a?(Module)}
      end

      def described_class
        @described_class ||= Class === described_type ? described_type : nil
      end

      def description_args
        @description_args ||= []
      end

      def description_parts #:nodoc:
        @description_parts ||= example_group_hierarchy.inject([]) do |parts, example_group_class|
          [parts << example_group_class.description_args].flatten
        end
      end

      def example_proxies # :nodoc:
        @example_proxies ||= []
      end

      def example_implementations # :nodoc:
        @example_implementations ||= {}
      end

      def examples(run_options=nil) #:nodoc:
        (run_options && run_options.reverse) ? example_proxies.reverse : example_proxies
      end

      def number_of_examples #:nodoc:
        example_proxies.length
      end

      def example_group_hierarchy
        @example_group_hierarchy ||= ExampleGroupHierarchy.new(self)
      end

      def nested_descriptions
        example_group_hierarchy.nested_descriptions
      end

      def include_constants_in(mod)
        include mod if (Spec::Ruby.version.to_f >= 1.9) & (Module === mod) & !(Class === mod)
      end

    private

      def subclass(*args, &example_group_block)
        @class_count ||= 0
        @class_count += 1
        klass = const_set("Subclass_#{@class_count}", Class.new(self))
        klass.set_description(*args)
        klass.include_constants_in(args.last[:scope])
        klass.module_eval(&example_group_block)
        klass
      end

      def dry_run(examples, run_options)
        examples.each do |example|
          run_options.reporter.example_started(example)
          run_options.reporter.example_finished(example)
        end
      end

      def run_before_all(run_options)
        return [true,{}] if example_group_hierarchy.before_all_parts.empty?
        example_proxy = ExampleProxy.new("before(:all)")
        before_all = new(example_proxy)
        begin
          example_group_hierarchy.run_before_all(before_all)
          return [true, before_all.instance_variable_hash]
        rescue Exception => e
          run_options.reporter.example_failed(example_proxy, e)
          return [false, before_all.instance_variable_hash]
        end
      end

      def run_examples(success, instance_variables, examples, run_options)
        return [success, instance_variables] unless success

        after_all_instance_variables = instance_variables

        examples.each do |example|
          example_group_instance = new(example, &example_implementations[example])
          success &= example_group_instance.execute(run_options, instance_variables)
          after_all_instance_variables = example_group_instance.instance_variable_hash
        end

        return [success, after_all_instance_variables]
      end

      def run_after_all(success, instance_variables, run_options)
        return success if example_group_hierarchy.after_all_parts.empty?
        example_proxy = ExampleProxy.new("after(:all)")
        after_all = new(example_proxy)
        after_all.set_instance_variables_from_hash(instance_variables)
        example_group_hierarchy.run_after_all(after_all)
        success
      rescue Exception => e
        run_options.reporter.example_failed(example_proxy, e)
        false
      end

      def examples_to_run(run_options)
        return example_proxies unless examples_were_specified?(run_options)
        if run_options.line_number_requested?
          if location =~ /:#{run_options.example_line}:?/
            example_proxies
          else
            example_proxies.select {|proxy| proxy.location =~ /:#{run_options.example_line}:?/}
          end
        else
          example_proxies.reject do |proxy|
            matcher = ExampleGroupMethods.matcher_class.
              new(description.to_s, proxy.description)
            !matcher.matches?(run_options.examples)
          end
        end
      end

      def examples_were_specified?(run_options)
        !run_options.examples.empty?
      end

      def method_added(name) # :nodoc:
        example(name.to_s, {}, caller(0)[1]) {__send__ name.to_s} if example_method?(name.to_s)
      end

      def example_method?(method_name)
        should_method?(method_name)
      end

      def should_method?(method_name)
        !(method_name =~ /^should(_not)?$/) &&
        method_name =~ /^should/ &&
        instance_method(method_name).arity < 1
      end

      def include_shared_example_group(shared_example_group)
        case shared_example_group
        when SharedExampleGroup
          include shared_example_group
        else
          unless example_group = SharedExampleGroup.find(shared_example_group)
            raise RuntimeError.new("Shared Example Group '#{shared_example_group}' can not be found")
          end
          include(example_group)
        end
      end
    end

  end
end
