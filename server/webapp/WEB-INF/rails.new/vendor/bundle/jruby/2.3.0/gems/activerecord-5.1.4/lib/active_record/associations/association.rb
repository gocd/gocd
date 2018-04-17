require "active_support/core_ext/array/wrap"

module ActiveRecord
  module Associations
    # = Active Record Associations
    #
    # This is the root class of all associations ('+ Foo' signifies an included module Foo):
    #
    #   Association
    #     SingularAssociation
    #       HasOneAssociation + ForeignAssociation
    #         HasOneThroughAssociation + ThroughAssociation
    #       BelongsToAssociation
    #         BelongsToPolymorphicAssociation
    #     CollectionAssociation
    #       HasManyAssociation + ForeignAssociation
    #         HasManyThroughAssociation + ThroughAssociation
    class Association #:nodoc:
      attr_reader :owner, :target, :reflection
      attr_accessor :inversed

      delegate :options, to: :reflection

      def initialize(owner, reflection)
        reflection.check_validity!

        @owner, @reflection = owner, reflection

        reset
        reset_scope
      end

      # Returns the name of the table of the associated class:
      #
      #   post.comments.aliased_table_name # => "comments"
      #
      def aliased_table_name
        klass.table_name
      end

      # Resets the \loaded flag to +false+ and sets the \target to +nil+.
      def reset
        @loaded = false
        @target = nil
        @stale_state = nil
        @inversed = false
      end

      # Reloads the \target and returns +self+ on success.
      def reload
        reset
        reset_scope
        load_target
        self unless target.nil?
      end

      # Has the \target been already \loaded?
      def loaded?
        @loaded
      end

      # Asserts the \target has been loaded setting the \loaded flag to +true+.
      def loaded!
        @loaded = true
        @stale_state = stale_state
        @inversed = false
      end

      # The target is stale if the target no longer points to the record(s) that the
      # relevant foreign_key(s) refers to. If stale, the association accessor method
      # on the owner will reload the target. It's up to subclasses to implement the
      # stale_state method if relevant.
      #
      # Note that if the target has not been loaded, it is not considered stale.
      def stale_target?
        !inversed && loaded? && @stale_state != stale_state
      end

      # Sets the target of this association to <tt>\target</tt>, and the \loaded flag to +true+.
      def target=(target)
        @target = target
        loaded!
      end

      def scope
        target_scope.merge!(association_scope)
      end

      # The scope for this association.
      #
      # Note that the association_scope is merged into the target_scope only when the
      # scope method is called. This is because at that point the call may be surrounded
      # by scope.scoping { ... } or with_scope { ... } etc, which affects the scope which
      # actually gets built.
      def association_scope
        if klass
          @association_scope ||= AssociationScope.scope(self, klass.connection)
        end
      end

      def reset_scope
        @association_scope = nil
      end

      # Set the inverse association, if possible
      def set_inverse_instance(record)
        if invertible_for?(record)
          inverse = record.association(inverse_reflection_for(record).name)
          inverse.target = owner
          inverse.inversed = true
        end
        record
      end

      # Remove the inverse association, if possible
      def remove_inverse_instance(record)
        if invertible_for?(record)
          inverse = record.association(inverse_reflection_for(record).name)
          inverse.target = nil
          inverse.inversed = false
        end
      end

      # Returns the class of the target. belongs_to polymorphic overrides this to look at the
      # polymorphic_type field on the owner.
      def klass
        reflection.klass
      end

      # Can be overridden (i.e. in ThroughAssociation) to merge in other scopes (i.e. the
      # through association's scope)
      def target_scope
        AssociationRelation.create(klass, klass.arel_table, klass.predicate_builder, self).merge!(klass.all)
      end

      def extensions
        extensions = klass.default_extensions | reflection.extensions

        if scope = reflection.scope
          extensions |= klass.unscoped.instance_exec(owner, &scope).extensions
        end

        extensions
      end

      # Loads the \target if needed and returns it.
      #
      # This method is abstract in the sense that it relies on +find_target+,
      # which is expected to be provided by descendants.
      #
      # If the \target is already \loaded it is just returned. Thus, you can call
      # +load_target+ unconditionally to get the \target.
      #
      # ActiveRecord::RecordNotFound is rescued within the method, and it is
      # not reraised. The proxy is \reset and +nil+ is the return value.
      def load_target
        @target = find_target if (@stale_state && stale_target?) || find_target?

        loaded! unless loaded?
        target
      rescue ActiveRecord::RecordNotFound
        reset
      end

      def interpolate(sql, record = nil)
        if sql.respond_to?(:to_proc)
          owner.instance_exec(record, &sql)
        else
          sql
        end
      end

      # We can't dump @reflection since it contains the scope proc
      def marshal_dump
        ivars = (instance_variables - [:@reflection]).map { |name| [name, instance_variable_get(name)] }
        [@reflection.name, ivars]
      end

      def marshal_load(data)
        reflection_name, ivars = data
        ivars.each { |name, val| instance_variable_set(name, val) }
        @reflection = @owner.class._reflect_on_association(reflection_name)
      end

      def initialize_attributes(record, except_from_scope_attributes = nil) #:nodoc:
        except_from_scope_attributes ||= {}
        skip_assign = [reflection.foreign_key, reflection.type].compact
        assigned_keys = record.changed_attribute_names_to_save
        assigned_keys += except_from_scope_attributes.keys.map(&:to_s)
        attributes = create_scope.except(*(assigned_keys - skip_assign))
        record.assign_attributes(attributes)
        set_inverse_instance(record)
      end

      def create(attributes = {}, &block)
        _create_record(attributes, &block)
      end

      def create!(attributes = {}, &block)
        _create_record(attributes, true, &block)
      end

      private

        def find_target?
          !loaded? && (!owner.new_record? || foreign_key_present?) && klass
        end

        def creation_attributes
          attributes = {}

          if (reflection.has_one? || reflection.collection?) && !options[:through]
            attributes[reflection.foreign_key] = owner[reflection.active_record_primary_key]

            if reflection.options[:as]
              attributes[reflection.type] = owner.class.base_class.name
            end
          end

          attributes
        end

        # Sets the owner attributes on the given record
        def set_owner_attributes(record)
          creation_attributes.each { |key, value| record[key] = value }
        end

        # Returns true if there is a foreign key present on the owner which
        # references the target. This is used to determine whether we can load
        # the target if the owner is currently a new record (and therefore
        # without a key). If the owner is a new record then foreign_key must
        # be present in order to load target.
        #
        # Currently implemented by belongs_to (vanilla and polymorphic) and
        # has_one/has_many :through associations which go through a belongs_to.
        def foreign_key_present?
          false
        end

        # Raises ActiveRecord::AssociationTypeMismatch unless +record+ is of
        # the kind of the class of the associated objects. Meant to be used as
        # a sanity check when you are about to assign an associated record.
        def raise_on_type_mismatch!(record)
          unless record.is_a?(reflection.klass)
            fresh_class = reflection.class_name.safe_constantize
            unless fresh_class && record.is_a?(fresh_class)
              message = "#{reflection.class_name}(##{reflection.klass.object_id}) expected, "\
                "got #{record.inspect} which is an instance of #{record.class}(##{record.class.object_id})"
              raise ActiveRecord::AssociationTypeMismatch, message
            end
          end
        end

        # Can be redefined by subclasses, notably polymorphic belongs_to
        # The record parameter is necessary to support polymorphic inverses as we must check for
        # the association in the specific class of the record.
        def inverse_reflection_for(record)
          reflection.inverse_of
        end

        # Returns true if inverse association on the given record needs to be set.
        # This method is redefined by subclasses.
        def invertible_for?(record)
          foreign_key_for?(record) && inverse_reflection_for(record)
        end

        # Returns true if record contains the foreign_key
        def foreign_key_for?(record)
          record.has_attribute?(reflection.foreign_key)
        end

        # This should be implemented to return the values of the relevant key(s) on the owner,
        # so that when stale_state is different from the value stored on the last find_target,
        # the target is stale.
        #
        # This is only relevant to certain associations, which is why it returns +nil+ by default.
        def stale_state
        end

        def build_record(attributes)
          reflection.build_association(attributes) do |record|
            initialize_attributes(record, attributes)
          end
        end

        # Returns true if statement cache should be skipped on the association reader.
        def skip_statement_cache?
          reflection.has_scope? ||
            scope.eager_loading? ||
            klass.scope_attributes? ||
            reflection.source_reflection.active_record.default_scopes.any?
        end
    end
  end
end
