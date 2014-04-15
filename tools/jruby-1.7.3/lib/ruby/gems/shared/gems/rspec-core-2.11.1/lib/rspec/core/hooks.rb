module RSpec
  module Core
    module Hooks
      include MetadataHashBuilder::WithConfigWarning

      module HookExtension
        attr_reader :options

        def with(options)
          @options = options
          self
        end

        def options_apply?(example_or_group)
          example_or_group.all_apply?(options)
        end
      end

      module BeforeHookExtension
        include HookExtension

        def run(example)
          example.instance_eval(&self)
        end

        def display_name
          "before hook"
        end
      end

      module AfterHookExtension
        include HookExtension

        def run(example)
          example.instance_eval_with_rescue("in an after hook", &self)
        end

        def display_name
          "after hook"
        end
      end

      module AroundHookExtension
        include HookExtension

        def display_name
          "around hook"
        end
      end

      module HookCollectionAliases
        def self.included(host)
          host.send :alias_method, :prepend, :unshift
          host.send :alias_method, :append,  :push
        end
      end

      class HookCollection < Array
        include HookCollectionAliases

        def for(example_or_group)
          self.class.new(select {|hook| hook.options_apply?(example_or_group)}).
            with(example_or_group)
        end

        def with(example)
          @example = example
          self
        end

        def run
          each {|h| h.run(@example) } unless empty?
        end
      end

      class AroundHookCollection < Array
        include HookCollectionAliases

        def for(example, initial_procsy=nil)
          self.class.new(select {|hook| hook.options_apply?(example)}).
            with(example, initial_procsy)
        end

        def with(example, initial_procsy)
          @example = example
          @initial_procsy = initial_procsy
          self
        end

        def run
          inject(@initial_procsy) do |procsy, around_hook|
            Example.procsy(procsy.metadata) do
              @example.instance_eval_with_args(procsy, &around_hook)
            end
          end.call
        end
      end

      class GroupHookCollection < Array
        def for(group)
          @group = group
          self
        end

        def run
          shift.run(@group) until empty?
        end
      end

      module RegistersGlobals
        def register_globals host, globals
          [:before, :after, :around].each do |position|
            process host, globals, position, :each
            next if position == :around # no around(:all) hooks
            process host, globals, position, :all
          end
        end

        private
        def process host, globals, position, scope
          globals[position][scope].each do |hook|
            unless host.ancestors.any? { |a| a.hooks[position][scope].include? hook }
              self[position][scope] << hook if scope == :each || hook.options_apply?(host)
            end
          end
        end
      end

      # @private
      def hooks
        @hooks ||= {
          :around => { :each => AroundHookCollection.new },
          :before => { :each => HookCollection.new, :all => HookCollection.new, :suite => HookCollection.new },
          :after =>  { :each => HookCollection.new, :all => HookCollection.new, :suite => HookCollection.new }
        }.extend(RegistersGlobals)
      end

      # @api public
      # @overload before(&block)
      # @overload before(scope, &block)
      # @overload before(scope, conditions, &block)
      # @overload before(conditions, &block)
      #
      # @param [Symbol] scope `:each`, `:all`, or `:suite` (defaults to `:each`)
      # @param [Hash] conditions
      #   constrains this hook to examples matching these conditions e.g.
      #   `before(:each, :ui => true) { ... }` will only run with examples or
      #   groups declared with `:ui => true`.
      #
      # @see #after
      # @see #around
      # @see ExampleGroup
      # @see SharedContext
      # @see SharedExampleGroup
      # @see Configuration
      #
      # Declare a block of code to be run before each example (using `:each`)
      # or once before any example (using `:all`). These are usually declared
      # directly in the {ExampleGroup} to which they apply, but they can also
      # be shared across multiple groups.
      #
      # You can also use `before(:suite)` to run a block of code before any
      # example groups are run. This should be declared in {RSpec.configure}
      #
      # Instance variables declared in `before(:each)` or `before(:all)` are
      # accessible within each example.
      #
      # ### Order
      #
      # `before` hooks are stored in three scopes, which are run in order:
      # `:suite`, `:all`, and `:each`. They can also be declared in several
      # different places: `RSpec.configure`, a parent group, the current group.
      # They are run in the following order:
      #
      #     before(:suite) # declared in RSpec.configure
      #     before(:all)   # declared in RSpec.configure
      #     before(:all)   # declared in a parent group
      #     before(:all)   # declared in the current group
      #     before(:each)  # declared in RSpec.configure
      #     before(:each)  # declared in a parent group
      #     before(:each)  # declared in the current group
      #
      # If more than one `before` is declared within any one scope, they are run
      # in the order in which they are declared.
      #
      # ### Conditions
      #
      # When you add a conditions hash to `before(:each)` or `before(:all)`,
      # RSpec will only apply that hook to groups or examples that match the
      # conditions. e.g.
      #
      #     RSpec.configure do |config|
      #       config.before(:each, :authorized => true) do
      #         log_in_as :authorized_user
      #       end
      #     end
      #
      #     describe Something, :authorized => true do
      #       # the before hook will run in before each example in this group
      #     end
      #
      #     describe SomethingElse do
      #       it "does something", :authorized => true do
      #         # the before hook will run before this example
      #       end
      #
      #       it "does something else" do
      #         # the hook will not run before this example
      #       end
      #     end
      #
      # ### Warning: `before(:suite, :with => :conditions)`
      #
      # The conditions hash is used to match against specific examples. Since
      # `before(:suite)` is not run in relation to any specific example or
      # group, conditions passed along with `:suite` are effectively ignored.
      #
      # ### Exceptions
      #
      # When an exception is raised in a `before` block, RSpec skips any
      # subsequent `before` blocks and the example, but runs all of the
      # `after(:each)` and `after(:all)` hooks.
      #
      # ### Warning: implicit before blocks
      #
      # `before` hooks can also be declared in shared contexts which get
      # included implicitly either by you or by extension libraries. Since
      # RSpec runs these in the order in which they are declared within each
      # scope, load order matters, and can lead to confusing results when one
      # before block depends on state that is prepared in another before block
      # that gets run later.
      #
      # ### Warning: `before(:all)`
      #
      # It is very tempting to use `before(:all)` to speed things up, but we
      # recommend that you avoid this as there are a number of gotchas, as well
      # as things that simply don't work.
      #
      # #### context
      #
      # `before(:all)` is run in an example that is generated to provide group
      # context for the block.
      #
      # #### instance variables
      #
      # Instance variables declared in `before(:all)` are shared across all the
      # examples in the group.  This means that each example can change the
      # state of a shared object, resulting in an ordering dependency that can
      # make it difficult to reason about failures.
      #
      # ### other frameworks
      #
      # Mock object frameworks and database transaction managers (like
      # ActiveRecord) are typically designed around the idea of setting up
      # before an example, running that one example, and then tearing down.
      # This means that mocks and stubs can (sometimes) be declared in
      # `before(:all)`, but get torn down before the first real example is ever
      # run.
      #
      # You _can_ create database-backed model objects in a `before(:all)` in
      # rspec-rails, but it will not be wrapped in a transaction for you, so
      # you are on your own to clean up in an `after(:all)` block.
      #
      # @example before(:each) declared in an {ExampleGroup}
      #
      #     describe Thing do
      #       before(:each) do
      #         @thing = Thing.new
      #       end
      #
      #       it "does something" do
      #         # here you can access @thing
      #       end
      #     end
      #
      # @example before(:all) declared in an {ExampleGroup}
      #
      #     describe Parser do
      #       before(:all) do
      #         File.open(file_to_parse, 'w') do |f|
      #           f.write <<-CONTENT
      #             stuff in the file
      #           CONTENT
      #         end
      #       end
      #
      #       it "parses the file" do
      #         Parser.parse(file_to_parse)
      #       end
      #
      #       after(:all) do
      #         File.delete(file_to_parse)
      #       end
      #     end
      def before(*args, &block)
        register_hook :append, :before, *args, &block
      end

      alias_method :append_before, :before

      # Adds `block` to the front of the list of `before` blocks in the same
      # scope (`:each`, `:all`, or `:suite`).
      #
      # See #before for scoping semantics.
      def prepend_before(*args, &block)
        register_hook :prepend, :before, *args, &block
      end

      # @api public
      # @overload after(&block)
      # @overload after(scope, &block)
      # @overload after(scope, conditions, &block)
      # @overload after(conditions, &block)
      #
      # @param [Symbol] scope `:each`, `:all`, or `:suite` (defaults to `:each`)
      # @param [Hash] conditions
      #   constrains this hook to examples matching these conditions e.g.
      #   `after(:each, :ui => true) { ... }` will only run with examples or
      #   groups declared with `:ui => true`.
      #
      # @see #before
      # @see #around
      # @see ExampleGroup
      # @see SharedContext
      # @see SharedExampleGroup
      # @see Configuration
      #
      # Declare a block of code to be run after each example (using `:each`) or
      # once after all examples (using `:all`). See
      # [#before](Hooks#before-instance_method) for more information about
      # ordering.
      #
      # ### Exceptions
      #
      # `after` hooks are guaranteed to run even when there are exceptions in
      # `before` hooks or examples.  When an exception is raised in an after
      # block, the exception is captured for later reporting, and subsequent
      # `after` blocks are run.
      #
      # ### Order
      #
      # `after` hooks are stored in three scopes, which are run in order:
      # `:each`, `:all`, and `:suite`. They can also be declared in several
      # different places: `RSpec.configure`, a parent group, the current group.
      # They are run in the following order:
      #
      #     after(:each) # declared in the current group
      #     after(:each) # declared in a parent group
      #     after(:each) # declared in RSpec.configure
      #     after(:all)  # declared in the current group
      #     after(:all)  # declared in a parent group
      #     after(:all)  # declared in RSpec.configure
      #
      # This is the reverse of the order in which `before` hooks are run.
      # Similarly, if more than one `after` is declared within any one scope,
      # they are run in reverse order of that in which they are declared.
      def after(*args, &block)
        register_hook :prepend, :after, *args, &block
      end

      alias_method :prepend_after, :after

      # Adds `block` to the back of the list of `after` blocks in the same
      # scope (`:each`, `:all`, or `:suite`).
      #
      # See #after for scoping semantics.
      def append_after(*args, &block)
        register_hook :append, :after, *args, &block
      end

      # @api public
      # @overload around(&block)
      # @overload around(scope, &block)
      # @overload around(scope, conditions, &block)
      # @overload around(conditions, &block)
      #
      # @param [Symbol] scope `:each` (defaults to `:each`)
      #   present for syntax parity with `before` and `after`, but `:each` is
      #   the only supported value.
      #
      # @param [Hash] conditions
      #   constrains this hook to examples matching these conditions e.g.
      #   `around(:each, :ui => true) { ... }` will only run with examples or
      #   groups declared with `:ui => true`.
      #
      # @yield [Example] the example to run
      #
      # @note the syntax of `around` is similar to that of `before` and `after`
      #   but the semantics are quite different. `before` and `after` hooks are
      #   run in the context of of the examples with which they are associated,
      #   whereas `around` hooks are actually responsible for running the
      #   examples. Consequently, `around` hooks do not have direct access to
      #   resources that are made available within the examples and their
      #   associated `before` and `after` hooks.
      #
      # @note `:each` is the only supported scope.
      #
      # Declare a block of code, parts of which will be run before and parts
      # after the example. It is your responsibility to run the example:
      #
      #     around(:each) do |ex|
      #       # do some stuff before
      #       ex.run
      #       # do some stuff after
      #     end
      #
      # The yielded example aliases `run` with `call`, which lets you treat it
      # like a `Proc`.  This is especially handy when working with libaries
      # that manage their own setup and teardown using a block or proc syntax,
      # e.g.
      #
      #     around(:each) {|ex| Database.transaction(&ex)}
      #     around(:each) {|ex| FakeFS(&ex)}
      #
      def around(*args, &block)
        register_hook :prepend, :around, *args, &block
      end

      # @private
      #
      # Runs all of the blocks stored with the hook in the context of the
      # example. If no example is provided, just calls the hook directly.
      def run_hook(hook, scope, example_or_group=ExampleGroup.new, initial_procsy=nil)
        find_hook(hook, scope, example_or_group, initial_procsy).run
      end

      # @private
      def around_each_hooks_for(example, initial_procsy=nil)
        AroundHookCollection.new(ancestors.map {|a| a.hooks[:around][:each]}.flatten).for(example, initial_procsy)
      end

    private

      SCOPES = [:each, :all, :suite]

      EXTENSIONS = {
        :before => BeforeHookExtension,
        :after  => AfterHookExtension,
        :around => AroundHookExtension
      }

      def before_all_hooks_for(group)
        GroupHookCollection.new(hooks[:before][:all]).for(group)
      end

      def after_all_hooks_for(group)
        GroupHookCollection.new(hooks[:after][:all]).for(group)
      end

      def before_each_hooks_for(example)
        HookCollection.new(ancestors.reverse.map {|a| a.hooks[:before][:each]}.flatten).for(example)
      end

      def after_each_hooks_for(example)
        HookCollection.new(ancestors.map {|a| a.hooks[:after][:each]}.flatten).for(example)
      end

      def register_hook prepend_or_append, hook, *args, &block
        scope, options = scope_and_options_from(*args)
        hooks[hook][scope].send(prepend_or_append, block.extend(EXTENSIONS[hook]).with(options))
      end

      def find_hook(hook, scope, example_or_group, initial_procsy)
        case [hook, scope]
        when [:before, :all]
          before_all_hooks_for(example_or_group)
        when [:after, :all]
          after_all_hooks_for(example_or_group)
        when [:around, :each]
          around_each_hooks_for(example_or_group, initial_procsy)
        when [:before, :each]
          before_each_hooks_for(example_or_group)
        when [:after, :each]
          after_each_hooks_for(example_or_group)
        when [:before, :suite], [:after, :suite]
          hooks[hook][:suite].with(example_or_group)
        end
      end

      def scope_and_options_from(*args)
        return extract_scope_from(args), build_metadata_hash_from(args)
      end

      def extract_scope_from(args)
        if SCOPES.include?(args.first)
          args.shift
        elsif args.any? { |a| a.is_a?(Symbol) }
          raise ArgumentError.new("You must explicitly give a scope (:each, :all, or :suite) when using symbols as metadata for a hook.")
        else
          :each
        end
      end
    end
  end
end
