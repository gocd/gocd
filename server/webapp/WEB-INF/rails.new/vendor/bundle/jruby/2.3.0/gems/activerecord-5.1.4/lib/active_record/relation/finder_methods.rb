require "active_support/core_ext/string/filters"

module ActiveRecord
  module FinderMethods
    ONE_AS_ONE = "1 AS one"

    # Find by id - This can either be a specific id (1), a list of ids (1, 5, 6), or an array of ids ([5, 6, 10]).
    # If one or more records can not be found for the requested ids, then RecordNotFound will be raised. If the primary key
    # is an integer, find by id coerces its arguments using +to_i+.
    #
    #   Person.find(1)          # returns the object for ID = 1
    #   Person.find("1")        # returns the object for ID = 1
    #   Person.find("31-sarah") # returns the object for ID = 31
    #   Person.find(1, 2, 6)    # returns an array for objects with IDs in (1, 2, 6)
    #   Person.find([7, 17])    # returns an array for objects with IDs in (7, 17)
    #   Person.find([1])        # returns an array for the object with ID = 1
    #   Person.where("administrator = 1").order("created_on DESC").find(1)
    #
    # NOTE: The returned records may not be in the same order as the ids you
    # provide since database rows are unordered. You will need to provide an explicit QueryMethods#order
    # option if you want the results to be sorted.
    #
    # ==== Find with lock
    #
    # Example for find with a lock: Imagine two concurrent transactions:
    # each will read <tt>person.visits == 2</tt>, add 1 to it, and save, resulting
    # in two saves of <tt>person.visits = 3</tt>. By locking the row, the second
    # transaction has to wait until the first is finished; we get the
    # expected <tt>person.visits == 4</tt>.
    #
    #   Person.transaction do
    #     person = Person.lock(true).find(1)
    #     person.visits += 1
    #     person.save!
    #   end
    #
    # ==== Variations of #find
    #
    #   Person.where(name: 'Spartacus', rating: 4)
    #   # returns a chainable list (which can be empty).
    #
    #   Person.find_by(name: 'Spartacus', rating: 4)
    #   # returns the first item or nil.
    #
    #   Person.find_or_initialize_by(name: 'Spartacus', rating: 4)
    #   # returns the first item or returns a new instance (requires you call .save to persist against the database).
    #
    #   Person.find_or_create_by(name: 'Spartacus', rating: 4)
    #   # returns the first item or creates it and returns it.
    #
    # ==== Alternatives for #find
    #
    #   Person.where(name: 'Spartacus', rating: 4).exists?(conditions = :none)
    #   # returns a boolean indicating if any record with the given conditions exist.
    #
    #   Person.where(name: 'Spartacus', rating: 4).select("field1, field2, field3")
    #   # returns a chainable list of instances with only the mentioned fields.
    #
    #   Person.where(name: 'Spartacus', rating: 4).ids
    #   # returns an Array of ids.
    #
    #   Person.where(name: 'Spartacus', rating: 4).pluck(:field1, :field2)
    #   # returns an Array of the required fields.
    def find(*args)
      return super if block_given?
      find_with_ids(*args)
    end

    # Finds the first record matching the specified conditions. There
    # is no implied ordering so if order matters, you should specify it
    # yourself.
    #
    # If no record is found, returns <tt>nil</tt>.
    #
    #   Post.find_by name: 'Spartacus', rating: 4
    #   Post.find_by "published_at < ?", 2.weeks.ago
    def find_by(arg, *args)
      where(arg, *args).take
    rescue ::RangeError
      nil
    end

    # Like #find_by, except that if no record is found, raises
    # an ActiveRecord::RecordNotFound error.
    def find_by!(arg, *args)
      where(arg, *args).take!
    rescue ::RangeError
      raise RecordNotFound.new("Couldn't find #{@klass.name} with an out of range value",
                               @klass.name)
    end

    # Gives a record (or N records if a parameter is supplied) without any implied
    # order. The order will depend on the database implementation.
    # If an order is supplied it will be respected.
    #
    #   Person.take # returns an object fetched by SELECT * FROM people LIMIT 1
    #   Person.take(5) # returns 5 objects fetched by SELECT * FROM people LIMIT 5
    #   Person.where(["name LIKE '%?'", name]).take
    def take(limit = nil)
      limit ? find_take_with_limit(limit) : find_take
    end

    # Same as #take but raises ActiveRecord::RecordNotFound if no record
    # is found. Note that #take! accepts no arguments.
    def take!
      take || raise_record_not_found_exception!
    end

    # Find the first record (or first N records if a parameter is supplied).
    # If no order is defined it will order by primary key.
    #
    #   Person.first # returns the first object fetched by SELECT * FROM people ORDER BY people.id LIMIT 1
    #   Person.where(["user_name = ?", user_name]).first
    #   Person.where(["user_name = :u", { u: user_name }]).first
    #   Person.order("created_on DESC").offset(5).first
    #   Person.first(3) # returns the first three objects fetched by SELECT * FROM people ORDER BY people.id LIMIT 3
    #
    def first(limit = nil)
      if limit
        find_nth_with_limit(0, limit)
      else
        find_nth 0
      end
    end

    # Same as #first but raises ActiveRecord::RecordNotFound if no record
    # is found. Note that #first! accepts no arguments.
    def first!
      first || raise_record_not_found_exception!
    end

    # Find the last record (or last N records if a parameter is supplied).
    # If no order is defined it will order by primary key.
    #
    #   Person.last # returns the last object fetched by SELECT * FROM people
    #   Person.where(["user_name = ?", user_name]).last
    #   Person.order("created_on DESC").offset(5).last
    #   Person.last(3) # returns the last three objects fetched by SELECT * FROM people.
    #
    # Take note that in that last case, the results are sorted in ascending order:
    #
    #   [#<Person id:2>, #<Person id:3>, #<Person id:4>]
    #
    # and not:
    #
    #   [#<Person id:4>, #<Person id:3>, #<Person id:2>]
    def last(limit = nil)
      return find_last(limit) if loaded? || limit_value

      result = limit(limit)
      result.order!(arel_attribute(primary_key)) if order_values.empty? && primary_key
      result = result.reverse_order!

      limit ? result.reverse : result.first
    end

    # Same as #last but raises ActiveRecord::RecordNotFound if no record
    # is found. Note that #last! accepts no arguments.
    def last!
      last || raise_record_not_found_exception!
    end

    # Find the second record.
    # If no order is defined it will order by primary key.
    #
    #   Person.second # returns the second object fetched by SELECT * FROM people
    #   Person.offset(3).second # returns the second object from OFFSET 3 (which is OFFSET 4)
    #   Person.where(["user_name = :u", { u: user_name }]).second
    def second
      find_nth 1
    end

    # Same as #second but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def second!
      second || raise_record_not_found_exception!
    end

    # Find the third record.
    # If no order is defined it will order by primary key.
    #
    #   Person.third # returns the third object fetched by SELECT * FROM people
    #   Person.offset(3).third # returns the third object from OFFSET 3 (which is OFFSET 5)
    #   Person.where(["user_name = :u", { u: user_name }]).third
    def third
      find_nth 2
    end

    # Same as #third but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def third!
      third || raise_record_not_found_exception!
    end

    # Find the fourth record.
    # If no order is defined it will order by primary key.
    #
    #   Person.fourth # returns the fourth object fetched by SELECT * FROM people
    #   Person.offset(3).fourth # returns the fourth object from OFFSET 3 (which is OFFSET 6)
    #   Person.where(["user_name = :u", { u: user_name }]).fourth
    def fourth
      find_nth 3
    end

    # Same as #fourth but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def fourth!
      fourth || raise_record_not_found_exception!
    end

    # Find the fifth record.
    # If no order is defined it will order by primary key.
    #
    #   Person.fifth # returns the fifth object fetched by SELECT * FROM people
    #   Person.offset(3).fifth # returns the fifth object from OFFSET 3 (which is OFFSET 7)
    #   Person.where(["user_name = :u", { u: user_name }]).fifth
    def fifth
      find_nth 4
    end

    # Same as #fifth but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def fifth!
      fifth || raise_record_not_found_exception!
    end

    # Find the forty-second record. Also known as accessing "the reddit".
    # If no order is defined it will order by primary key.
    #
    #   Person.forty_two # returns the forty-second object fetched by SELECT * FROM people
    #   Person.offset(3).forty_two # returns the forty-second object from OFFSET 3 (which is OFFSET 44)
    #   Person.where(["user_name = :u", { u: user_name }]).forty_two
    def forty_two
      find_nth 41
    end

    # Same as #forty_two but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def forty_two!
      forty_two || raise_record_not_found_exception!
    end

    # Find the third-to-last record.
    # If no order is defined it will order by primary key.
    #
    #   Person.third_to_last # returns the third-to-last object fetched by SELECT * FROM people
    #   Person.offset(3).third_to_last # returns the third-to-last object from OFFSET 3
    #   Person.where(["user_name = :u", { u: user_name }]).third_to_last
    def third_to_last
      find_nth_from_last 3
    end

    # Same as #third_to_last but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def third_to_last!
      third_to_last || raise_record_not_found_exception!
    end

    # Find the second-to-last record.
    # If no order is defined it will order by primary key.
    #
    #   Person.second_to_last # returns the second-to-last object fetched by SELECT * FROM people
    #   Person.offset(3).second_to_last # returns the second-to-last object from OFFSET 3
    #   Person.where(["user_name = :u", { u: user_name }]).second_to_last
    def second_to_last
      find_nth_from_last 2
    end

    # Same as #second_to_last but raises ActiveRecord::RecordNotFound if no record
    # is found.
    def second_to_last!
      second_to_last || raise_record_not_found_exception!
    end

    # Returns true if a record exists in the table that matches the +id+ or
    # conditions given, or false otherwise. The argument can take six forms:
    #
    # * Integer - Finds the record with this primary key.
    # * String - Finds the record with a primary key corresponding to this
    #   string (such as <tt>'5'</tt>).
    # * Array - Finds the record that matches these +find+-style conditions
    #   (such as <tt>['name LIKE ?', "%#{query}%"]</tt>).
    # * Hash - Finds the record that matches these +find+-style conditions
    #   (such as <tt>{name: 'David'}</tt>).
    # * +false+ - Returns always +false+.
    # * No args - Returns +false+ if the table is empty, +true+ otherwise.
    #
    # For more information about specifying conditions as a hash or array,
    # see the Conditions section in the introduction to ActiveRecord::Base.
    #
    # Note: You can't pass in a condition as a string (like <tt>name =
    # 'Jamie'</tt>), since it would be sanitized and then queried against
    # the primary key column, like <tt>id = 'name = \'Jamie\''</tt>.
    #
    #   Person.exists?(5)
    #   Person.exists?('5')
    #   Person.exists?(['name LIKE ?', "%#{query}%"])
    #   Person.exists?(id: [1, 4, 8])
    #   Person.exists?(name: 'David')
    #   Person.exists?(false)
    #   Person.exists?
    def exists?(conditions = :none)
      if Base === conditions
        raise ArgumentError, <<-MSG.squish
          You are passing an instance of ActiveRecord::Base to `exists?`.
          Please pass the id of the object by calling `.id`.
        MSG
      end

      return false if !conditions || limit_value == 0

      relation = self unless eager_loading?
      relation ||= apply_join_dependency(self, construct_join_dependency(eager_loading: false))

      return false if ActiveRecord::NullRelation === relation

      relation = construct_relation_for_exists(relation, conditions)

      connection.select_value(relation, "#{name} Exists", relation.bound_attributes) ? true : false
    rescue ::RangeError
      false
    end

    # This method is called whenever no records are found with either a single
    # id or multiple ids and raises an ActiveRecord::RecordNotFound exception.
    #
    # The error message is different depending on whether a single id or
    # multiple ids are provided. If multiple ids are provided, then the number
    # of results obtained should be provided in the +result_size+ argument and
    # the expected number of results should be provided in the +expected_size+
    # argument.
    def raise_record_not_found_exception!(ids = nil, result_size = nil, expected_size = nil, key = primary_key) # :nodoc:
      conditions = arel.where_sql(@klass.arel_engine)
      conditions = " [#{conditions}]" if conditions
      name = @klass.name

      if ids.nil?
        error = "Couldn't find #{name}"
        error << " with#{conditions}" if conditions
        raise RecordNotFound.new(error, name)
      elsif Array(ids).size == 1
        error = "Couldn't find #{name} with '#{key}'=#{ids}#{conditions}"
        raise RecordNotFound.new(error, name, key, ids)
      else
        error = "Couldn't find all #{name.pluralize} with '#{key}': "
        error << "(#{ids.join(", ")})#{conditions} (found #{result_size} results, but was looking for #{expected_size})"

        raise RecordNotFound.new(error, name, primary_key, ids)
      end
    end

    private

      def offset_index
        offset_value || 0
      end

      def find_with_associations
        # NOTE: the JoinDependency constructed here needs to know about
        #       any joins already present in `self`, so pass them in
        #
        # failing to do so means that in cases like activerecord/test/cases/associations/inner_join_association_test.rb:136
        # incorrect SQL is generated. In that case, the join dependency for
        # SpecialCategorizations is constructed without knowledge of the
        # preexisting join in joins_values to categorizations (by way of
        # the `has_many :through` for categories).
        #
        join_dependency = construct_join_dependency(joins_values)

        aliases  = join_dependency.aliases
        relation = select aliases.columns
        relation = apply_join_dependency(relation, join_dependency)

        if block_given?
          yield relation
        else
          if ActiveRecord::NullRelation === relation
            []
          else
            arel = relation.arel
            rows = connection.select_all(arel, "SQL", relation.bound_attributes)
            join_dependency.instantiate(rows, aliases)
          end
        end
      end

      def construct_relation_for_exists(relation, conditions)
        relation = relation.except(:select, :distinct, :order)._select!(ONE_AS_ONE).limit!(1)

        case conditions
        when Array, Hash
          relation.where!(conditions)
        else
          relation.where!(primary_key => conditions) unless conditions == :none
        end

        relation
      end

      def construct_join_dependency(joins = [], eager_loading: true)
        including = eager_load_values + includes_values
        ActiveRecord::Associations::JoinDependency.new(@klass, including, joins, eager_loading: eager_loading)
      end

      def construct_relation_for_association_calculations
        apply_join_dependency(self, construct_join_dependency(joins_values))
      end

      def apply_join_dependency(relation, join_dependency)
        relation = relation.except(:includes, :eager_load, :preload).joins!(join_dependency)

        if using_limitable_reflections?(join_dependency.reflections)
          relation
        else
          if relation.limit_value
            limited_ids = limited_ids_for(relation)
            limited_ids.empty? ? relation.none! : relation.where!(primary_key => limited_ids)
          end
          relation.except(:limit, :offset)
        end
      end

      def limited_ids_for(relation)
        values = @klass.connection.columns_for_distinct(
          "#{quoted_table_name}.#{quoted_primary_key}", relation.order_values)

        relation = relation.except(:select).select(values).distinct!
        arel = relation.arel

        id_rows = @klass.connection.select_all(arel, "SQL", relation.bound_attributes)
        id_rows.map { |row| row[primary_key] }
      end

      def using_limitable_reflections?(reflections)
        reflections.none?(&:collection?)
      end

      def find_with_ids(*ids)
        raise UnknownPrimaryKey.new(@klass) if primary_key.nil?

        expects_array = ids.first.kind_of?(Array)
        return ids.first if expects_array && ids.first.empty?

        ids = ids.flatten.compact.uniq

        case ids.size
        when 0
          raise RecordNotFound, "Couldn't find #{@klass.name} without an ID"
        when 1
          result = find_one(ids.first)
          expects_array ? [ result ] : result
        else
          find_some(ids)
        end
      rescue ::RangeError
        raise RecordNotFound, "Couldn't find #{@klass.name} with an out of range ID"
      end

      def find_one(id)
        if ActiveRecord::Base === id
          raise ArgumentError, <<-MSG.squish
            You are passing an instance of ActiveRecord::Base to `find`.
            Please pass the id of the object by calling `.id`.
          MSG
        end

        relation = where(primary_key => id)
        record = relation.take

        raise_record_not_found_exception!(id, 0, 1) unless record

        record
      end

      def find_some(ids)
        return find_some_ordered(ids) unless order_values.present?

        result = where(primary_key => ids).to_a

        expected_size =
          if limit_value && ids.size > limit_value
            limit_value
          else
            ids.size
          end

        # 11 ids with limit 3, offset 9 should give 2 results.
        if offset_value && (ids.size - offset_value < expected_size)
          expected_size = ids.size - offset_value
        end

        if result.size == expected_size
          result
        else
          raise_record_not_found_exception!(ids, result.size, expected_size)
        end
      end

      def find_some_ordered(ids)
        ids = ids.slice(offset_value || 0, limit_value || ids.size) || []

        result = except(:limit, :offset).where(primary_key => ids).records

        if result.size == ids.size
          pk_type = @klass.type_for_attribute(primary_key)

          records_by_id = result.index_by(&:id)
          ids.map { |id| records_by_id.fetch(pk_type.cast(id)) }
        else
          raise_record_not_found_exception!(ids, result.size, ids.size)
        end
      end

      def find_take
        if loaded?
          records.first
        else
          @take ||= limit(1).records.first
        end
      end

      def find_take_with_limit(limit)
        if loaded?
          records.take(limit)
        else
          limit(limit).to_a
        end
      end

      def find_nth(index)
        @offsets[offset_index + index] ||= find_nth_with_limit(index, 1).first
      end

      def find_nth_with_limit(index, limit)
        if loaded?
          records[index, limit] || []
        else
          relation = if order_values.empty? && primary_key
            order(arel_attribute(primary_key).asc)
          else
            self
          end

          if limit_value.nil? || index < limit_value
            relation = relation.offset(offset_index + index) unless index.zero?
            relation.limit(limit).to_a
          else
            []
          end
        end
      end

      def find_nth_from_last(index)
        if loaded?
          records[-index]
        else
          relation = if order_values.empty? && primary_key
            order(arel_attribute(primary_key).asc)
          else
            self
          end

          relation.to_a[-index]
          # TODO: can be made more performant on large result sets by
          # for instance, last(index)[-index] (which would require
          # refactoring the last(n) finder method to make test suite pass),
          # or by using a combination of reverse_order, limit, and offset,
          # e.g., reverse_order.offset(index-1).first
        end
      end

      def find_last(limit)
        limit ? records.last(limit) : records.last
      end
  end
end
