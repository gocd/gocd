module RSpec
  module Core
    # ExampleGroup and {Example} are the main structural elements of
    # rspec-core.  Consider this example:
    #
    #     describe Thing do
    #       it "does something" do
    #       end
    #     end
    #
    # The object returned by `describe Thing` is a subclass of ExampleGroup.
    # The object returned by `it "does something"` is an instance of Example,
    # which serves as a wrapper for an instance of the ExampleGroup in which it
    # is declared.
    class ExampleGroup
      extend  MetadataHashBuilder::WithDeprecationWarning
      extend  Extensions::ModuleEvalWithArgs
      extend  Hooks

      include MemoizedHelpers
      include Extensions::InstanceEvalWithArgs
      include Pending
      include SharedExampleGroup
      extend SharedExampleGroup

      # @private
      def self.world
        RSpec.world
      end

      # @private
      def self.register
        world.register(self)
      end

      class << self
        # @private
        def self.delegate_to_metadata(*names)
          names.each do |name|
            define_method name do
              metadata[:example_group][name]
            end
          end
        end

        def description
          description = metadata[:example_group][:description]
          RSpec.configuration.format_docstrings_block.call(description)
        end

        delegate_to_metadata :described_class, :file_path

        # @private
        def display_name
          RSpec.deprecate('`RSpec::Core::ExampleGroup.display_name`',
                          :replacement => "`RSpec::Core::ExampleGroup.description`")
          description
        end

        # @private
        def describes
          RSpec.deprecate('`RSpec::Core::ExampleGroup.describes`',
                          :replacement => "`RSpec::Core::ExampleGroup.described_class`")
          described_class
        end
      end

      # @private
      # @macro [attach] define_example_method
      #   @param [String] name
      #   @param [Hash] extra_options
      #   @param [Block] implementation
      #   @yield [Example] the example object
      def self.define_example_method(name, extra_options={})
        module_eval(<<-END_RUBY, __FILE__, __LINE__)
          def self.#{name}(desc=nil, *args, &block)
            if #{name.inspect} == :pending
              RSpec.warn_deprecation(<<-EOS.gsub(/^\s+\\|/, ''))
                |The semantics of `RSpec::Core::ExampleGroup.pending` are changing in RSpec 3.
                |In RSpec 2.x, it caused the example to be skipped. In RSpec 3, the example will
                |still be run but is expected to fail, and will be marked as a failure (rather
                |than as pending) if the example passes, just like how `pending` with a block
                |from within an example already works.
                |
                |To keep the same skip semantics, change `pending` to `skip`.  Otherwise, if you
                |want the new RSpec 3 behavior, you can safely ignore this warning and continue
                |to upgrade to RSpec 3 without addressing it.
                |
                |Called from \#{CallerFilter.first_non_rspec_line}.
                |
              EOS
            end
            options = build_metadata_hash_from(args)
            options.update(:pending => RSpec::Core::Pending::NOT_YET_IMPLEMENTED) unless block
            # Backport from RSpec 3 to assist with upgrading
            options.update(:pending => options[:skip]) if options[:skip]
            options.update(#{extra_options.inspect})
            examples << RSpec::Core::Example.new(self, desc, options, block)
            examples.last
          end
        END_RUBY
      end

      # Defines an example within a group.
      # @example
      #   example do
      #   end
      #
      #   example "does something" do
      #   end
      #
      #   example "does something", :with => 'additional metadata' do
      #   end
      #
      #   example "does something" do |ex|
      #     # ex is the Example object that evals this block
      #   end
      define_example_method :example
      # Defines an example within a group.
      # @example
      define_example_method :it
      # Defines an example within a group.
      # This is here primarily for backward compatibility with early versions
      # of RSpec which used `context` and `specify` instead of `describe` and
      # `it`.
      define_example_method :specify

      # Shortcut to define an example with `:focus` => true
      # @see example
      define_example_method :focus,   :focused => true, :focus => true
      # Shortcut to define an example with `:focus` => true
      # @see example
      define_example_method :fit,     :focused => true, :focus => true

      # Shortcut to define an example with :pending => true
      # @see example
      define_example_method :pending,  :pending => true
      # Shortcut to define an example with :pending => true
      # Backported from RSpec 3 to aid migration.
      # @see example
      define_example_method :skip,     :pending => true
      # Shortcut to define an example with :pending => 'Temporarily disabled with xexample'
      # @see example
      define_example_method :xexample, :pending => 'Temporarily disabled with xexample'
      # Shortcut to define an example with :pending => 'Temporarily disabled with xit'
      # @see example
      define_example_method :xit,      :pending => 'Temporarily disabled with xit'
      # Shortcut to define an example with :pending => 'Temporarily disabled with xspecify'
      # @see example
      define_example_method :xspecify, :pending => 'Temporarily disabled with xspecify'

      # Shortcut to define an example with `:focus` => true
      # @see example
      def self.focused(desc=nil, *args, &block)
        RSpec.deprecate("`RSpec::Core::ExampleGroup.focused`",
                        :replacement => "`RSpec::Core::ExampleGroup.focus`")

        metadata = Hash === args.last ? args.pop : {}
        metadata.merge!(:focus => true, :focused => true)
        args << metadata

        example(desc, *args, &block)
      end

      # Works like `alias_method :name, :example` with the added benefit of
      # assigning default metadata to the generated example.
      #
      # @note Use with caution. This extends the language used in your
      #   specs, but does not add any additional documentation.  We use this
      #   in rspec to define methods like `focus` and `xit`, but we also add
      #   docs for those methods.
      def self.alias_example_to name, extra={}
        RSpec.deprecate("`RSpec::Core::ExampleGroup.alias_example_to`",
                        :replacement => "`RSpec::Core::Configuration#alias_example_to`")
        define_example_method name, extra
      end

      # @private
      # @macro [attach] define_nested_shared_group_method
      #
      #   @see SharedExampleGroup
      def self.define_nested_shared_group_method(new_name, report_label=nil)
        module_eval(<<-END_RUBY, __FILE__, __LINE__)
          def self.#{new_name}(name, *args, &customization_block)
            group = describe("#{report_label || "it should behave like"} \#{name}") do
              find_and_eval_shared("examples", name, *args, &customization_block)
            end
            group.metadata[:shared_group_name] = name
            group
          end
        END_RUBY
      end

      # Generates a nested example group and includes the shared content
      # mapped to `name` in the nested group.
      define_nested_shared_group_method :it_behaves_like, "behaves like"
      # Generates a nested example group and includes the shared content
      # mapped to `name` in the nested group.
      define_nested_shared_group_method :it_should_behave_like

      # Works like `alias_method :name, :it_behaves_like` with the added
      # benefit of assigning default metadata to the generated example.
      #
      # @note Use with caution. This extends the language used in your
      #   specs, but does not add any additional documentation.  We use this
      #   in rspec to define `it_should_behave_like` (for backward
      #   compatibility), but we also add docs for that method.
      def self.alias_it_behaves_like_to name, *args, &block
        RSpec.deprecate("`RSpec::Core::ExampleGroup.alias_it_behaves_like_to`",
                        :replacement => "`RSpec::Core::Configuration#alias_it_behaves_like_to`")
        define_nested_shared_group_method name, *args, &block
      end

      # Includes shared content mapped to `name` directly in the group in which
      # it is declared, as opposed to `it_behaves_like`, which creates a nested
      # group. If given a block, that block is also eval'd in the current context.
      #
      # @see SharedExampleGroup
      def self.include_context(name, *args, &block)
        find_and_eval_shared("context", name, *args, &block)
      end

      # Includes shared content mapped to `name` directly in the group in which
      # it is declared, as opposed to `it_behaves_like`, which creates a nested
      # group. If given a block, that block is also eval'd in the current context.
      #
      # @see SharedExampleGroup
      def self.include_examples(name, *args, &block)
        find_and_eval_shared("examples", name, *args, &block)
      end

      if Proc.method_defined?(:parameters) # for >= 1.9
        # Warn when submitting the name of more than one example group to
        # include_examples, it_behaves_like, etc.
        #
        # Helpful when upgrading from rspec-1 (which supported multiple shared
        # groups in one call) to rspec-2 (which does not).
        #
        # See https://github.com/rspec/rspec-core/issues/1066 for background.
        def self.warn_unexpected_args(label, name, args, shared_block)
          if !args.empty? && shared_block.parameters.count == 0
            if shared_example_groups[args.first]
              warn <<-WARNING
shared #{label} support#{'s' if /context/ =~ label.to_s} the name of only one example group, received #{[name, *args].inspect}
called from #{CallerFilter.first_non_rspec_line}"
WARNING
            else
                warn <<-WARNING
shared #{label} #{name.inspect} expected #{shared_block.arity} args, got #{args.inspect}
called from #{CallerFilter.first_non_rspec_line}"
WARNING
            end
          end
        end
      else
        # no-op for Ruby < 1.9
        #
        # Ruby 1.8 reports lambda {}.arity == -1, so can't support this warning
        # reliably
        def self.warn_unexpected_args(*)
        end
      end

      # @private
      def self.find_and_eval_shared(label, name, *args, &customization_block)
        raise ArgumentError, "Could not find shared #{label} #{name.inspect}" unless
          shared_block = shared_example_groups[name]

        warn_unexpected_args(label, name, args, shared_block)

        module_eval_with_args(*args, &shared_block)
        module_eval(&customization_block) if customization_block
      end

      # @private
      def self.examples
        @examples ||= []
      end

      # @private
      def self.filtered_examples
        world.filtered_examples[self]
      end

      # @private
      def self.descendant_filtered_examples
        @descendant_filtered_examples ||= filtered_examples + children.inject([]){|l,c| l + c.descendant_filtered_examples}
      end

      # The [Metadata](Metadata) object associated with this group.
      # @see Metadata
      def self.metadata
        @metadata if defined?(@metadata)
      end

      # @private
      # @return [Metadata] belonging to the parent of a nested {ExampleGroup}
      def self.superclass_metadata
        @superclass_metadata ||= self.superclass.respond_to?(:metadata) ? self.superclass.metadata : nil
      end

      # Generates a subclass of this example group which inherits
      # everything except the examples themselves.
      #
      # ## Examples
      #
      #     describe "something" do # << This describe method is defined in
      #                             # << RSpec::Core::DSL, included in the
      #                             # << global namespace
      #       before do
      #         do_something_before
      #       end
      #
      #       let(:thing) { Thing.new }
      #
      #       describe "attribute (of something)" do
      #         # examples in the group get the before hook
      #         # declared above, and can access `thing`
      #       end
      #     end
      #
      # @see DSL#describe
      def self.describe(*args, &example_group_block)
        @_subclass_count ||= 0
        @_subclass_count += 1

        if Symbol === args.first || Hash === args.first
          description_arg_behavior_changing_in_rspec_3 = DescriptionBehaviorChange.new(
            args.first, CallerFilter.first_non_rspec_line
          )
        end

        args << {} unless args.last.is_a?(Hash)
        args.last.update(
          :example_group_block => example_group_block,
          :description_arg_behavior_changing_in_rspec_3 => description_arg_behavior_changing_in_rspec_3
        )

        # TODO 2010-05-05: Because we don't know if const_set is thread-safe
        child = const_set(
          "Nested_#{@_subclass_count}",
          subclass(self, args, &example_group_block)
        )
        children << child
        child
      end

      DescriptionBehaviorChange = Struct.new(:arg, :call_site) do
        def warning
          <<-EOS.gsub(/^\s+\|/, '')
            |The semantics of `describe <a #{arg.class.name}>` are changing in RSpec 3. In RSpec 2,
            |this would be treated as metadata, but as the first `describe` argument,
            |this will be treated as the described object (affecting the value of
            |`described_class`) in RSpec 3. If you want this to be treated as metadata,
            |pass a description as the first argument.
            |(Example group defined at #{call_site})
          EOS
        end
      end

      class << self
        alias_method :context, :describe
      end

      # @private
      def self.subclass(parent, args, &example_group_block)
        subclass = Class.new(parent)
        subclass.set_it_up(*args)
        subclass.module_eval(&example_group_block) if example_group_block

        # The LetDefinitions module must be included _after_ other modules
        # to ensure that it takes precendence when there are name collisions.
        # Thus, we delay including it until after the example group block
        # has been eval'd.
        MemoizedHelpers.define_helpers_on(subclass)

        subclass
      end

      # @private
      def self.children
        @children ||= [].extend(Extensions::Ordered::ExampleGroups)
      end

      # @private
      def self.descendants
        @_descendants ||= [self] + children.inject([]) {|list, c| list + c.descendants}
      end

      ## @private
      def self.parent_groups
        @parent_groups ||= ancestors.select {|a| a < RSpec::Core::ExampleGroup}
      end

      # @private
      def self.top_level?
        @top_level ||= superclass == ExampleGroup
      end

      # @private
      def self.ensure_example_groups_are_configured
        unless defined?(@@example_groups_configured)
          RSpec.configuration.configure_mock_framework
          RSpec.configuration.configure_expectation_framework
          @@example_groups_configured = true
        end
      end

      # @private
      def self.set_it_up(*args)
        # Ruby 1.9 has a bug that can lead to infinite recursion and a
        # SystemStackError if you include a module in a superclass after
        # including it in a subclass: https://gist.github.com/845896
        # To prevent this, we must include any modules in RSpec::Core::ExampleGroup
        # before users create example groups and have a chance to include
        # the same module in a subclass of RSpec::Core::ExampleGroup.
        # So we need to configure example groups here.
        ensure_example_groups_are_configured

        symbol_description = args.shift if args.first.is_a?(Symbol)
        args << build_metadata_hash_from(args)
        args.unshift(symbol_description) if symbol_description
        @metadata = RSpec::Core::Metadata.new(superclass_metadata).process(*args)
        hooks.register_globals(self, RSpec.configuration.hooks)
        world.configure_group(self)
      end

      # @private
      def self.before_all_ivars
        @before_all_ivars ||= {}
      end

      # @private
      def self.store_before_all_ivars(example_group_instance)
        return if example_group_instance.instance_variables.empty?

        example_group_instance.instance_variables.each { |ivar|
          before_all_ivars[ivar] = example_group_instance.instance_variable_get(ivar)
        }
      end

      # @private
      def self.assign_before_all_ivars(ivars, example_group_instance)
        ivars.each { |ivar, val| example_group_instance.instance_variable_set(ivar, val) }
      end

      # @private
      def self.run_before_all_hooks(example_group_instance)
        return if descendant_filtered_examples.empty?
        begin
          assign_before_all_ivars(superclass.before_all_ivars, example_group_instance)

          AllHookMemoizedHash::Before.isolate_for_all_hook(example_group_instance) do
            run_hook(:before, :all, example_group_instance)
          end
        ensure
          store_before_all_ivars(example_group_instance)
        end
      end

      # @private
      def self.run_around_each_hooks(example, initial_procsy)
        run_hook(:around, :each, example, initial_procsy)
      end

      # @private
      def self.run_before_each_hooks(example)
        run_hook(:before, :each, example)
      end

      # @private
      def self.run_after_each_hooks(example)
        run_hook(:after, :each, example)
      end

      # @private
      def self.run_after_all_hooks(example_group_instance)
        return if descendant_filtered_examples.empty?
        assign_before_all_ivars(before_all_ivars, example_group_instance)

        AllHookMemoizedHash::After.isolate_for_all_hook(example_group_instance) do
          run_hook(:after, :all, example_group_instance)
        end
      end

      # Runs all the examples in this group
      def self.run(reporter)
        if RSpec.wants_to_quit
          RSpec.clear_remaining_example_groups if top_level?
          return
        end
        reporter.example_group_started(self)

        begin
          run_before_all_hooks(new)
          result_for_this_group = run_examples(reporter)
          results_for_descendants = children.ordered.map {|child| child.run(reporter)}.all?
          result_for_this_group && results_for_descendants
        rescue Exception => ex
          RSpec.wants_to_quit = true if fail_fast?
          fail_filtered_examples(ex, reporter)
        ensure
          run_after_all_hooks(new)
          before_all_ivars.clear
          reporter.example_group_finished(self)
        end
      end

      # @private
      def self.run_examples(reporter)
        filtered_examples.ordered.map do |example|
          next if RSpec.wants_to_quit
          instance = new
          set_ivars(instance, before_all_ivars)
          succeeded = example.run(instance, reporter)
          RSpec.wants_to_quit = true if fail_fast? && !succeeded
          succeeded
        end.all?
      end

      # @private
      def self.fail_filtered_examples(exception, reporter)
        filtered_examples.each { |example| example.fail_with_exception(reporter, exception) }

        children.each do |child|
          reporter.example_group_started(child)
          child.fail_filtered_examples(exception, reporter)
          reporter.example_group_finished(child)
        end
        false
      end

      # @private
      def self.fail_fast?
        RSpec.configuration.fail_fast?
      end

      # @private
      def self.any_apply?(filters)
        metadata.any_apply?(filters)
      end

      # @private
      def self.all_apply?(filters)
        metadata.all_apply?(filters)
      end

      # @private
      def self.declaration_line_numbers
        @declaration_line_numbers ||= [metadata[:example_group][:line_number]] +
          examples.collect {|e| e.metadata[:line_number]} +
          children.inject([]) {|l,c| l + c.declaration_line_numbers}
      end

      # @private
      def self.top_level_description
        parent_groups.last.description
      end

      # @private
      def self.set_ivars(instance, ivars)
        ivars.each {|name, value| instance.instance_variable_set(name, value)}
      end

      def example=(current_example)
        RSpec.current_example = current_example
      end

      # @deprecated use a block argument
      def example
        warn_deprecation_of_example_accessor :example
        RSpec.current_example
      end

      # @deprecated use a block argument
      def running_example
        warn_deprecation_of_example_accessor :running_example
        RSpec.current_example
      end

      def warn_deprecation_of_example_accessor(name)
        RSpec.warn_deprecation(<<-EOS.gsub(/^\s*\|/, ''))
          |RSpec::Core::ExampleGroup##{name} is deprecated and will be removed
          |in RSpec 3. There are a few options for what you can use instead:
          |
          |  - rspec-core's DSL methods (`it`, `before`, `after`, `let`, `subject`, etc)
          |    now yield the example as a block argument, and that is the recommended
          |    way to access the current example from those contexts.
          |  - The current example is now exposed via `RSpec.current_example`,
          |    which is accessible from any context.
          |  - If you can't update the code at this call site (e.g. because it is in
          |    an extension gem), you can use this snippet to continue making this
          |    method available in RSpec 2.99 and RSpec 3:
          |
          |      RSpec.configure do |c|
          |        c.expose_current_running_example_as :#{name}
          |      end
          |
          |(Called from #{CallerFilter.first_non_rspec_line})
        EOS
      end

      # Returns the class or module passed to the `describe` method (or alias).
      # Returns nil if the subject is not a class or module.
      # @example
      #     describe Thing do
      #       it "does something" do
      #         described_class == Thing
      #       end
      #     end
      #
      #
      def described_class
        self.class.described_class
      end

      # @private
      # instance_evals the block, capturing and reporting an exception if
      # raised
      def instance_eval_with_rescue(example, context = nil, &hook)
        begin
          instance_eval_with_args(example, &hook)
        rescue Exception => e
          if RSpec.current_example
            RSpec.current_example.set_exception(e, context)
          else
            raise
          end
        end
      end
    end
  end
end
