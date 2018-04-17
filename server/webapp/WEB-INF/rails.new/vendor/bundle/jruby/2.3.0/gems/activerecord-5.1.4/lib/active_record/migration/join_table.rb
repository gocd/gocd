module ActiveRecord
  class Migration
    module JoinTable #:nodoc:
      private

        def find_join_table_name(table_1, table_2, options = {})
          options.delete(:table_name) || join_table_name(table_1, table_2)
        end

        def join_table_name(table_1, table_2)
          ModelSchema.derive_join_table_name(table_1, table_2).to_sym
        end
    end
  end
end
