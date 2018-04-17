module ActiveRecord
  module Associations
    class SingularAssociation < Association #:nodoc:
      # Implements the reader method, e.g. foo.bar for Foo.has_one :bar
      def reader
        if !loaded? || stale_target?
          reload
        end

        target
      end

      # Implements the writer method, e.g. foo.bar= for Foo.belongs_to :bar
      def writer(record)
        replace(record)
      end

      def build(attributes = {})
        record = build_record(attributes)
        yield(record) if block_given?
        set_new_record(record)
        record
      end

      # Implements the reload reader method, e.g. foo.reload_bar for
      # Foo.has_one :bar
      def force_reload_reader
        klass.uncached { reload }
        target
      end

      private

        def create_scope
          scope.scope_for_create.stringify_keys.except(klass.primary_key)
        end

        def find_target
          return scope.take if skip_statement_cache?

          conn = klass.connection
          sc = reflection.association_scope_cache(conn, owner) do
            StatementCache.create(conn) { |params|
              as = AssociationScope.create { params.bind }
              target_scope.merge(as.scope(self, conn)).limit(1)
            }
          end

          binds = AssociationScope.get_bind_values(owner, reflection.chain)
          sc.execute(binds, klass, conn) do |record|
            set_inverse_instance record
          end.first
        rescue ::RangeError
          nil
        end

        def replace(record)
          raise NotImplementedError, "Subclasses must implement a replace(record) method"
        end

        def set_new_record(record)
          replace(record)
        end

        def _create_record(attributes, raise_error = false)
          record = build_record(attributes)
          yield(record) if block_given?
          saved = record.save
          set_new_record(record)
          raise RecordInvalid.new(record) if !saved && raise_error
          record
        end
    end
  end
end
