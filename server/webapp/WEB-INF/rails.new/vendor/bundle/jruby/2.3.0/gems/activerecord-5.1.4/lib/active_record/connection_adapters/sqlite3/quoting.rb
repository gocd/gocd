module ActiveRecord
  module ConnectionAdapters
    module SQLite3
      module Quoting # :nodoc:
        def quote_string(s)
          @connection.class.quote(s)
        end

        def quote_table_name_for_assignment(table, attr)
          quote_column_name(attr)
        end

        def quote_column_name(name)
          @quoted_column_names[name] ||= %Q("#{super.gsub('"', '""')}").freeze
        end

        def quoted_time(value)
          quoted_date(value)
        end

        def quoted_binary(value)
          "x'#{value.hex}'"
        end

        private

          def _type_cast(value)
            case value
            when BigDecimal
              value.to_f
            when String
              if value.encoding == Encoding::ASCII_8BIT
                super(value.encode(Encoding::UTF_8))
              else
                super
              end
            else
              super
            end
          end
      end
    end
  end
end
