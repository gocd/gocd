require 'active_support/deprecation'

module ActiveRecord
  module DeprecatedFinders
    class ScopeWrapper
      def self.wrap(klass, scope)
        if scope.is_a?(Hash)
          ActiveSupport::Deprecation.warn(
            "Calling #scope or #default_scope with a hash is deprecated. Please use a lambda " \
            "containing a scope. E.g. scope :red, -> { where(color: 'red') }"
          )

          new(klass, scope)
        elsif !scope.is_a?(Relation) && scope.respond_to?(:call)
          new(klass, scope)
        else
          scope
        end
      end

      def initialize(klass, scope)
        @klass = klass
        @scope = scope
      end

      def call(*args)
        if @scope.respond_to?(:call)
          result = @scope.call(*args)

          if result.is_a?(Hash)
            msg = "Returning a hash from a #scope or #default_scope block is deprecated. Please " \
              "return an actual scope object instead. E.g. scope :red, -> { where(color: 'red') } " \
              "rather than scope :red, -> { { conditions: { color: 'red' } } }. "

            if @scope.respond_to?(:source_location)
              msg << "(The scope was defined at #{@scope.source_location.join(':')}.)"
            end

            ActiveSupport::Deprecation.warn(msg)
          end
        else
          result = @scope
        end

        if result.is_a?(Hash)
          @klass.unscoped.apply_finder_options(result, true)
        else
          result
        end
      end
    end

    def default_scope(scope = {}, &block)
      if block_given?
        super ScopeWrapper.new(self, block), &nil
      else
        super ScopeWrapper.wrap(self, scope)
      end
    end

    def scoped(options = nil)
      ActiveSupport::Deprecation.warn("Model.scoped is deprecated. Please use Model.all instead.")
      options ? all.apply_finder_options(options, true) : all
    end

    def all(options = nil)
      options ? super().all(options) : super()
    end

    def scope(name, body = {}, &block)
      super(name, ScopeWrapper.wrap(self, body), &block)
    end

    def with_scope(scope = {}, action = :merge)
      ActiveSupport::Deprecation.warn(
        "ActiveRecord::Base#with_scope and #with_exclusive_scope are deprecated. " \
        "Please use ActiveRecord::Relation#scoping instead. (You can use #merge " \
        "to merge multiple scopes together.)"
      )

      # If another Active Record class has been passed in, get its current scope
      scope = scope.current_scope if !scope.is_a?(Relation) && scope.respond_to?(:current_scope)

      previous_scope = self.current_scope

      if scope.is_a?(Hash)
        # Dup first and second level of hash (method and params).
        scope = scope.dup
        scope.each do |method, params|
          scope[method] = params.dup unless params == true
        end

        scope.assert_valid_keys([ :find, :create ])
        relation = construct_finder_arel(scope[:find] || {})
        relation.default_scoped = true unless action == :overwrite

        if previous_scope && previous_scope.create_with_value && scope[:create]
          scope_for_create = if action == :merge
            previous_scope.create_with_value.merge(scope[:create])
          else
            scope[:create]
          end

          relation = relation.create_with(scope_for_create)
        else
          scope_for_create = scope[:create]
          scope_for_create ||= previous_scope.create_with_value if previous_scope
          relation = relation.create_with(scope_for_create) if scope_for_create
        end

        scope = relation
      end

      scope = previous_scope.merge(scope) if previous_scope && action == :merge
      scope.scoping { yield }
    end

    protected

    # Works like with_scope, but discards any nested properties.
    def with_exclusive_scope(method_scoping = {}, &block)
      if method_scoping.values.any? { |e| e.is_a?(ActiveRecord::Relation) }
        raise ArgumentError, <<-MSG
New finder API can not be used with_exclusive_scope. You can either call unscoped to get an anonymous scope not bound to the default_scope:

User.unscoped.where(:active => true)

Or call unscoped with a block:

User.unscoped do
User.where(:active => true).all
end

MSG
      end
      with_scope(method_scoping, :overwrite, &block)
    end

    private

    def construct_finder_arel(options = {}, scope = nil)
      relation = options.is_a?(Hash) ? unscoped.apply_finder_options(options, true) : options
      relation = scope.merge(relation) if scope
      relation
    end
  end

  class Base
    extend DeprecatedFinders
  end
end
