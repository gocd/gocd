module ActiveRecord
  module Querying
    delegate :find, :take, :take!, :first, :first!, :last, :last!, :exists?, :any?, :many?, :to => :all
    delegate :first_or_create, :first_or_create!, :first_or_initialize, :to => :all
    delegate :find_or_create_by, :find_or_create_by!, :find_or_initialize_by, :to => :all
    delegate :find_by, :find_by!, :to => :all
    delegate :destroy, :destroy_all, :delete, :delete_all, :update, :update_all, :to => :all
    delegate :find_each, :find_in_batches, :to => :all
    delegate :select, :group, :order, :except, :reorder, :limit, :offset, :joins,
             :where, :preload, :eager_load, :includes, :from, :lock, :readonly,
             :having, :create_with, :uniq, :distinct, :references, :none, :unscope, :to => :all
    delegate :count, :average, :minimum, :maximum, :sum, :calculate, :pluck, :ids, :to => :all

    # Executes a custom SQL query against your database and returns all the results. The results will
    # be returned as an array with columns requested encapsulated as attributes of the model you call
    # this method from. If you call <tt>Product.find_by_sql</tt> then the results will be returned in
    # a Product object with the attributes you specified in the SQL query.
    #
    # If you call a complicated SQL query which spans multiple tables the columns specified by the
    # SELECT will be attributes of the model, whether or not they are columns of the corresponding
    # table.
    #
    # The +sql+ parameter is a full SQL query as a string. It will be called as is, there will be
    # no database agnostic conversions performed. This should be a last resort because using, for example,
    # MySQL specific terms will lock you to using that particular database engine or require you to
    # change your call if you switch engines.
    #
    #   # A simple SQL query spanning multiple tables
    #   Post.find_by_sql "SELECT p.title, c.author FROM posts p, comments c WHERE p.id = c.post_id"
    #   # => [#<Post:0x36bff9c @attributes={"title"=>"Ruby Meetup", "first_name"=>"Quentin"}>, ...]
    #
    #   # You can use the same string replacement techniques as you can with ActiveRecord#find
    #   Post.find_by_sql ["SELECT title FROM posts WHERE author = ? AND created > ?", author_id, start_date]
    #   # => [#<Post:0x36bff9c @attributes={"title"=>"The Cheap Man Buys Twice"}>, ...]
    def find_by_sql(sql, binds = [])
      result_set = connection.select_all(sanitize_sql(sql), "#{name} Load", binds)
      column_types = {}

      if result_set.respond_to? :column_types
        column_types = result_set.column_types
      else
        ActiveSupport::Deprecation.warn "the object returned from `select_all` must respond to `column_types`"
      end

      result_set.map { |record| instantiate(record, column_types) }
    end

    # Returns the result of an SQL statement that should only include a COUNT(*) in the SELECT part.
    # The use of this method should be restricted to complicated SQL queries that can't be executed
    # using the ActiveRecord::Calculations class methods. Look into those before using this.
    #
    # ==== Parameters
    #
    # * +sql+ - An SQL statement which should return a count query from the database, see the example below.
    #
    #   Product.count_by_sql "SELECT COUNT(*) FROM sales s, customers c WHERE s.customer_id = c.id"
    def count_by_sql(sql)
      sql = sanitize_conditions(sql)
      connection.select_value(sql, "#{name} Count").to_i
    end
  end
end
