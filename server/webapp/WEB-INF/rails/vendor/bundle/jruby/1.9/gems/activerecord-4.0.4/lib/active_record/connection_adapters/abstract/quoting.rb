require 'active_support/core_ext/big_decimal/conversions'

module ActiveRecord
  module ConnectionAdapters # :nodoc:
    module Quoting
      # Quotes the column value to help prevent
      # {SQL injection attacks}[http://en.wikipedia.org/wiki/SQL_injection].
      def quote(value, column = nil)
        # records are quoted as their primary key
        return value.quoted_id if value.respond_to?(:quoted_id)

        case value
        when String, ActiveSupport::Multibyte::Chars
          value = value.to_s
          return "'#{quote_string(value)}'" unless column

          case column.type
          when :binary then "'#{quote_string(column.string_to_binary(value))}'"
          when :integer then value.to_i.to_s
          when :float then value.to_f.to_s
          else
            "'#{quote_string(value)}'"
          end

        when true, false
          if column && column.type == :integer
            value ? '1' : '0'
          else
            value ? quoted_true : quoted_false
          end
          # BigDecimals need to be put in a non-normalized form and quoted.
        when nil        then "NULL"
        when BigDecimal then value.to_s('F')
        when Numeric, ActiveSupport::Duration then value.to_s
        when Date, Time then "'#{quoted_date(value)}'"
        when Symbol     then "'#{quote_string(value.to_s)}'"
        when Class      then "'#{value.to_s}'"
        else
          "'#{quote_string(YAML.dump(value))}'"
        end
      end

      # Cast a +value+ to a type that the database understands. For example,
      # SQLite does not understand dates, so this method will convert a Date
      # to a String.
      def type_cast(value, column)
        if value.respond_to?(:quoted_id) && value.respond_to?(:id)
          return value.id
        end

        case value
        when String, ActiveSupport::Multibyte::Chars
          value = value.to_s
          return value unless column

          case column.type
          when :binary then value
          when :integer then value.to_i
          when :float then value.to_f
          else
            value
          end

        when true, false
          if column && column.type == :integer
            value ? 1 : 0
          else
            value ? 't' : 'f'
          end
          # BigDecimals need to be put in a non-normalized form and quoted.
        when nil        then nil
        when BigDecimal then value.to_s('F')
        when Numeric    then value
        when Date, Time then quoted_date(value)
        when Symbol     then value.to_s
        else
          to_type = column ? " to #{column.type}" : ""
          raise TypeError, "can't cast #{value.class}#{to_type}"
        end
      end

      # Quotes a string, escaping any ' (single quote) and \ (backslash)
      # characters.
      def quote_string(s)
        s.gsub(/\\/, '\&\&').gsub(/'/, "''") # ' (for ruby-mode)
      end

      # Quotes the column name. Defaults to no quoting.
      def quote_column_name(column_name)
        column_name
      end

      # Quotes the table name. Defaults to column name quoting.
      def quote_table_name(table_name)
        quote_column_name(table_name)
      end

      # Override to return the quoted table name for assignment. Defaults to
      # table quoting.
      #
      # This works for mysql and mysql2 where table.column can be used to
      # resolve ambiguity.
      #
      # We override this in the sqlite and postgresql adapters to use only
      # the column name (as per syntax requirements).
      def quote_table_name_for_assignment(table, attr)
        quote_table_name("#{table}.#{attr}")
      end

      def quoted_true
        "'t'"
      end

      def quoted_false
        "'f'"
      end

      def quoted_date(value)
        if value.acts_like?(:time)
          zone_conversion_method = ActiveRecord::Base.default_timezone == :utc ? :getutc : :getlocal

          if value.respond_to?(zone_conversion_method)
            value = value.send(zone_conversion_method)
          end
        end

        value.to_s(:db)
      end
    end
  end
end
