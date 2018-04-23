module ActiveRecord
  module Locking
    # == What is Optimistic Locking
    #
    # Optimistic locking allows multiple users to access the same record for edits, and assumes a minimum of
    # conflicts with the data. It does this by checking whether another process has made changes to a record since
    # it was opened, an <tt>ActiveRecord::StaleObjectError</tt> exception is thrown if that has occurred
    # and the update is ignored.
    #
    # Check out <tt>ActiveRecord::Locking::Pessimistic</tt> for an alternative.
    #
    # == Usage
    #
    # Active Record supports optimistic locking if the +lock_version+ field is present. Each update to the
    # record increments the +lock_version+ column and the locking facilities ensure that records instantiated twice
    # will let the last one saved raise a +StaleObjectError+ if the first was also updated. Example:
    #
    #   p1 = Person.find(1)
    #   p2 = Person.find(1)
    #
    #   p1.first_name = "Michael"
    #   p1.save
    #
    #   p2.first_name = "should fail"
    #   p2.save # Raises an ActiveRecord::StaleObjectError
    #
    # Optimistic locking will also check for stale data when objects are destroyed. Example:
    #
    #   p1 = Person.find(1)
    #   p2 = Person.find(1)
    #
    #   p1.first_name = "Michael"
    #   p1.save
    #
    #   p2.destroy # Raises an ActiveRecord::StaleObjectError
    #
    # You're then responsible for dealing with the conflict by rescuing the exception and either rolling back, merging,
    # or otherwise apply the business logic needed to resolve the conflict.
    #
    # This locking mechanism will function inside a single Ruby process. To make it work across all
    # web requests, the recommended approach is to add +lock_version+ as a hidden field to your form.
    #
    # This behavior can be turned off by setting <tt>ActiveRecord::Base.lock_optimistically = false</tt>.
    # To override the name of the +lock_version+ column, set the <tt>locking_column</tt> class attribute:
    #
    #   class Person < ActiveRecord::Base
    #     self.locking_column = :lock_person
    #   end
    #
    module Optimistic
      extend ActiveSupport::Concern

      included do
        class_attribute :lock_optimistically, instance_writer: false
        self.lock_optimistically = true
      end

      def locking_enabled? #:nodoc:
        self.class.locking_enabled?
      end

      private

        def increment_lock
          lock_col = self.class.locking_column
          previous_lock_value = send(lock_col).to_i
          send(lock_col + "=", previous_lock_value + 1)
        end

        def _create_record(attribute_names = self.attribute_names, *)
          if locking_enabled?
            # We always want to persist the locking version, even if we don't detect
            # a change from the default, since the database might have no default
            attribute_names |= [self.class.locking_column]
          end
          super
        end

        def _update_record(attribute_names = self.attribute_names)
          return super unless locking_enabled?
          return 0 if attribute_names.empty?

          begin
            lock_col = self.class.locking_column

            previous_lock_value = read_attribute_before_type_cast(lock_col)

            increment_lock

            attribute_names.push(lock_col)

            relation = self.class.unscoped

            affected_rows = relation.where(
              self.class.primary_key => id,
              lock_col => previous_lock_value
            ).update_all(
              attributes_for_update(attribute_names).map do |name|
                [name, _read_attribute(name)]
              end.to_h
            )

            unless affected_rows == 1
              raise ActiveRecord::StaleObjectError.new(self, "update")
            end

            affected_rows

          # If something went wrong, revert the locking_column value.
          rescue Exception
            send(lock_col + "=", previous_lock_value.to_i)
            raise
          end
        end

        def destroy_row
          affected_rows = super

          if locking_enabled? && affected_rows != 1
            raise ActiveRecord::StaleObjectError.new(self, "destroy")
          end

          affected_rows
        end

        def relation_for_destroy
          relation = super

          if locking_enabled?
            locking_column = self.class.locking_column
            relation = relation.where(locking_column => _read_attribute(locking_column))
          end

          relation
        end

        module ClassMethods
          DEFAULT_LOCKING_COLUMN = "lock_version"

          # Returns true if the +lock_optimistically+ flag is set to true
          # (which it is, by default) and the table includes the
          # +locking_column+ column (defaults to +lock_version+).
          def locking_enabled?
            lock_optimistically && columns_hash[locking_column]
          end

          # Set the column to use for optimistic locking. Defaults to +lock_version+.
          def locking_column=(value)
            reload_schema_from_cache
            @locking_column = value.to_s
          end

          # The version column used for optimistic locking. Defaults to +lock_version+.
          def locking_column
            @locking_column = DEFAULT_LOCKING_COLUMN unless defined?(@locking_column)
            @locking_column
          end

          # Reset the column used for optimistic locking back to the +lock_version+ default.
          def reset_locking_column
            self.locking_column = DEFAULT_LOCKING_COLUMN
          end

          # Make sure the lock version column gets updated when counters are
          # updated.
          def update_counters(id, counters)
            counters = counters.merge(locking_column => 1) if locking_enabled?
            super
          end

          private

            # We need to apply this decorator here, rather than on module inclusion. The closure
            # created by the matcher would otherwise evaluate for `ActiveRecord::Base`, not the
            # sub class being decorated. As such, changes to `lock_optimistically`, or
            # `locking_column` would not be picked up.
            def inherited(subclass)
              subclass.class_eval do
                is_lock_column = ->(name, _) { lock_optimistically && name == locking_column }
                decorate_matching_attribute_types(is_lock_column, :_optimistic_locking) do |type|
                  LockingType.new(type)
                end
              end
              super
            end
        end
    end

    # In de/serialize we change `nil` to 0, so that we can allow passing
    # `nil` values to `lock_version`, and not result in `ActiveRecord::StaleObjectError`
    # during update record.
    class LockingType < DelegateClass(Type::Value) # :nodoc:
      def deserialize(value)
        super.to_i
      end

      def serialize(value)
        super.to_i
      end

      def init_with(coder)
        __setobj__(coder["subtype"])
      end

      def encode_with(coder)
        coder["subtype"] = __getobj__
      end
    end
  end
end
