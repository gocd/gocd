module ActiveRecord
  # Statement cache is used to cache a single statement in order to avoid creating the AST again.
  # Initializing the cache is done by passing the statement in the create block:
  #
  #   cache = StatementCache.create(Book.connection) do |params|
  #     Book.where(name: "my book").where("author_id > 3")
  #   end
  #
  # The cached statement is executed by using the
  # {connection.execute}[rdoc-ref:ConnectionAdapters::DatabaseStatements#execute] method:
  #
  #   cache.execute([], Book, Book.connection)
  #
  # The relation returned by the block is cached, and for each
  # {execute}[rdoc-ref:ConnectionAdapters::DatabaseStatements#execute]
  # call the cached relation gets duped. Database is queried when +to_a+ is called on the relation.
  #
  # If you want to cache the statement without the values you can use the +bind+ method of the
  # block parameter.
  #
  #   cache = StatementCache.create(Book.connection) do |params|
  #     Book.where(name: params.bind)
  #   end
  #
  # And pass the bind values as the first argument of +execute+ call.
  #
  #   cache.execute(["my book"], Book, Book.connection)
  class StatementCache # :nodoc:
    class Substitute; end # :nodoc:

    class Query # :nodoc:
      def initialize(sql)
        @sql = sql
      end

      def sql_for(binds, connection)
        @sql
      end
    end

    class PartialQuery < Query # :nodoc:
      def initialize(values)
        @values = values
        @indexes = values.each_with_index.find_all { |thing, i|
          Arel::Nodes::BindParam === thing
        }.map(&:last)
      end

      def sql_for(binds, connection)
        val = @values.dup
        casted_binds = binds.map(&:value_for_database)
        @indexes.each { |i| val[i] = connection.quote(casted_binds.shift) }
        val.join
      end
    end

    def self.query(sql)
      Query.new(sql)
    end

    def self.partial_query(values)
      PartialQuery.new(values)
    end

    class Params # :nodoc:
      def bind; Substitute.new; end
    end

    class BindMap # :nodoc:
      def initialize(bound_attributes)
        @indexes = []
        @bound_attributes = bound_attributes

        bound_attributes.each_with_index do |attr, i|
          if Substitute === attr.value
            @indexes << i
          end
        end
      end

      def bind(values)
        bas = @bound_attributes.dup
        @indexes.each_with_index { |offset, i| bas[offset] = bas[offset].with_cast_value(values[i]) }
        bas
      end
    end

    attr_reader :bind_map, :query_builder

    def self.create(connection, block = Proc.new)
      relation      = block.call Params.new
      bind_map      = BindMap.new relation.bound_attributes
      query_builder = connection.cacheable_query(self, relation.arel)
      new query_builder, bind_map
    end

    def initialize(query_builder, bind_map)
      @query_builder = query_builder
      @bind_map      = bind_map
    end

    def execute(params, klass, connection, &block)
      bind_values = bind_map.bind params

      sql = query_builder.sql_for bind_values, connection

      klass.find_by_sql(sql, bind_values, preparable: true, &block)
    end
    alias :call :execute
  end
end
