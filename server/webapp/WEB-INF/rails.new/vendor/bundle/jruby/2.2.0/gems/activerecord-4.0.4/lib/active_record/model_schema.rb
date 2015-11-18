module ActiveRecord
  module ModelSchema
    extend ActiveSupport::Concern

    included do
      ##
      # :singleton-method:
      # Accessor for the prefix type that will be prepended to every primary key column name.
      # The options are :table_name and :table_name_with_underscore. If the first is specified,
      # the Product class will look for "productid" instead of "id" as the primary column. If the
      # latter is specified, the Product class will look for "product_id" instead of "id". Remember
      # that this is a global setting for all Active Records.
      mattr_accessor :primary_key_prefix_type, instance_writer: false

      ##
      # :singleton-method:
      # Accessor for the name of the prefix string to prepend to every table name. So if set
      # to "basecamp_", all table names will be named like "basecamp_projects", "basecamp_people",
      # etc. This is a convenient way of creating a namespace for tables in a shared database.
      # By default, the prefix is the empty string.
      #
      # If you are organising your models within modules you can add a prefix to the models within
      # a namespace by defining a singleton method in the parent module called table_name_prefix which
      # returns your chosen prefix.
      class_attribute :table_name_prefix, instance_writer: false
      self.table_name_prefix = ""

      ##
      # :singleton-method:
      # Works like +table_name_prefix+, but appends instead of prepends (set to "_basecamp" gives "projects_basecamp",
      # "people_basecamp"). By default, the suffix is the empty string.
      class_attribute :table_name_suffix, instance_writer: false
      self.table_name_suffix = ""

      ##
      # :singleton-method:
      # Indicates whether table names should be the pluralized versions of the corresponding class names.
      # If true, the default table name for a Product class will be +products+. If false, it would just be +product+.
      # See table_name for the full rules on table/class naming. This is true, by default.
      class_attribute :pluralize_table_names, instance_writer: false
      self.pluralize_table_names = true

      self.inheritance_column = 'type'
    end

    module ClassMethods
      # Guesses the table name (in forced lower-case) based on the name of the class in the
      # inheritance hierarchy descending directly from ActiveRecord::Base. So if the hierarchy
      # looks like: Reply < Message < ActiveRecord::Base, then Message is used
      # to guess the table name even when called on Reply. The rules used to do the guess
      # are handled by the Inflector class in Active Support, which knows almost all common
      # English inflections. You can add new inflections in config/initializers/inflections.rb.
      #
      # Nested classes are given table names prefixed by the singular form of
      # the parent's table name. Enclosing modules are not considered.
      #
      # ==== Examples
      #
      #   class Invoice < ActiveRecord::Base
      #   end
      #
      #   file                  class               table_name
      #   invoice.rb            Invoice             invoices
      #
      #   class Invoice < ActiveRecord::Base
      #     class Lineitem < ActiveRecord::Base
      #     end
      #   end
      #
      #   file                  class               table_name
      #   invoice.rb            Invoice::Lineitem   invoice_lineitems
      #
      #   module Invoice
      #     class Lineitem < ActiveRecord::Base
      #     end
      #   end
      #
      #   file                  class               table_name
      #   invoice/lineitem.rb   Invoice::Lineitem   lineitems
      #
      # Additionally, the class-level +table_name_prefix+ is prepended and the
      # +table_name_suffix+ is appended. So if you have "myapp_" as a prefix,
      # the table name guess for an Invoice class becomes "myapp_invoices".
      # Invoice::Lineitem becomes "myapp_invoice_lineitems".
      #
      # You can also set your own table name explicitly:
      #
      #   class Mouse < ActiveRecord::Base
      #     self.table_name = "mice"
      #   end
      #
      # Alternatively, you can override the table_name method to define your
      # own computation. (Possibly using <tt>super</tt> to manipulate the default
      # table name.) Example:
      #
      #   class Post < ActiveRecord::Base
      #     def self.table_name
      #       "special_" + super
      #     end
      #   end
      #   Post.table_name # => "special_posts"
      def table_name
        reset_table_name unless defined?(@table_name)
        @table_name
      end

      # Sets the table name explicitly. Example:
      #
      #   class Project < ActiveRecord::Base
      #     self.table_name = "project"
      #   end
      #
      # You can also just define your own <tt>self.table_name</tt> method; see
      # the documentation for ActiveRecord::Base#table_name.
      def table_name=(value)
        value = value && value.to_s

        if defined?(@table_name)
          return if value == @table_name
          reset_column_information if connected?
        end

        @table_name        = value
        @quoted_table_name = nil
        @arel_table        = nil
        @sequence_name     = nil unless defined?(@explicit_sequence_name) && @explicit_sequence_name
        @relation          = Relation.new(self, arel_table)
      end

      # Returns a quoted version of the table name, used to construct SQL statements.
      def quoted_table_name
        @quoted_table_name ||= connection.quote_table_name(table_name)
      end

      # Computes the table name, (re)sets it internally, and returns it.
      def reset_table_name #:nodoc:
        self.table_name = if abstract_class?
          superclass == Base ? nil : superclass.table_name
        elsif superclass.abstract_class?
          superclass.table_name || compute_table_name
        else
          compute_table_name
        end
      end

      def full_table_name_prefix #:nodoc:
        (parents.detect{ |p| p.respond_to?(:table_name_prefix) } || self).table_name_prefix
      end

      # Defines the name of the table column which will store the class name on single-table
      # inheritance situations.
      #
      # The default inheritance column name is +type+, which means it's a
      # reserved word inside Active Record. To be able to use single-table
      # inheritance with another column name, or to use the column +type+ in
      # your own model for something else, you can set +inheritance_column+:
      #
      #     self.inheritance_column = 'zoink'
      def inheritance_column
        (@inheritance_column ||= nil) || superclass.inheritance_column
      end

      # Sets the value of inheritance_column
      def inheritance_column=(value)
        @inheritance_column = value.to_s
        @explicit_inheritance_column = true
      end

      def sequence_name
        if base_class == self
          @sequence_name ||= reset_sequence_name
        else
          (@sequence_name ||= nil) || base_class.sequence_name
        end
      end

      def reset_sequence_name #:nodoc:
        @explicit_sequence_name = false
        @sequence_name          = connection.default_sequence_name(table_name, primary_key)
      end

      # Sets the name of the sequence to use when generating ids to the given
      # value, or (if the value is nil or false) to the value returned by the
      # given block. This is required for Oracle and is useful for any
      # database which relies on sequences for primary key generation.
      #
      # If a sequence name is not explicitly set when using Oracle or Firebird,
      # it will default to the commonly used pattern of: #{table_name}_seq
      #
      # If a sequence name is not explicitly set when using PostgreSQL, it
      # will discover the sequence corresponding to your primary key for you.
      #
      #   class Project < ActiveRecord::Base
      #     self.sequence_name = "projectseq"   # default would have been "project_seq"
      #   end
      def sequence_name=(value)
        @sequence_name          = value.to_s
        @explicit_sequence_name = true
      end

      # Indicates whether the table associated with this class exists
      def table_exists?
        connection.schema_cache.table_exists?(table_name)
      end

      # Returns an array of column objects for the table associated with this class.
      def columns
        @columns ||= connection.schema_cache.columns(table_name).map do |col|
          col = col.dup
          col.primary = (col.name == primary_key)
          col
        end
      end

      # Returns a hash of column objects for the table associated with this class.
      def columns_hash
        @columns_hash ||= Hash[columns.map { |c| [c.name, c] }]
      end

      def column_types # :nodoc:
        @column_types ||= decorate_columns(columns_hash.dup)
      end

      def decorate_columns(columns_hash) # :nodoc:
        return if columns_hash.empty?

        @serialized_column_names ||= self.columns_hash.keys.find_all do |name|
          serialized_attributes.key?(name)
        end

        @serialized_column_names.each do |name|
          columns_hash[name] = AttributeMethods::Serialization::Type.new(columns_hash[name])
        end

        @time_zone_column_names ||= self.columns_hash.find_all do |name, col|
          create_time_zone_conversion_attribute?(name, col)
        end.map!(&:first)

        @time_zone_column_names.each do |name|
          columns_hash[name] = AttributeMethods::TimeZoneConversion::Type.new(columns_hash[name])
        end

        columns_hash
      end

      # Returns a hash where the keys are column names and the values are
      # default values when instantiating the AR object for this table.
      def column_defaults
        @column_defaults ||= Hash[columns.map { |c| [c.name, c.default] }]
      end

      # Returns an array of column names as strings.
      def column_names
        @column_names ||= columns.map { |column| column.name }
      end

      # Returns an array of column objects where the primary id, all columns ending in "_id" or "_count",
      # and columns used for single table inheritance have been removed.
      def content_columns
        @content_columns ||= columns.reject { |c| c.primary || c.name =~ /(_id|_count)$/ || c.name == inheritance_column }
      end

      # Returns a hash of all the methods added to query each of the columns in the table with the name of the method as the key
      # and true as the value. This makes it possible to do O(1) lookups in respond_to? to check if a given method for attribute
      # is available.
      def column_methods_hash #:nodoc:
        @dynamic_methods_hash ||= column_names.each_with_object(Hash.new(false)) do |attr, methods|
          attr_name = attr.to_s
          methods[attr.to_sym]       = attr_name
          methods["#{attr}=".to_sym] = attr_name
          methods["#{attr}?".to_sym] = attr_name
          methods["#{attr}_before_type_cast".to_sym] = attr_name
        end
      end

      # Resets all the cached information about columns, which will cause them
      # to be reloaded on the next request.
      #
      # The most common usage pattern for this method is probably in a migration,
      # when just after creating a table you want to populate it with some default
      # values, eg:
      #
      #  class CreateJobLevels < ActiveRecord::Migration
      #    def up
      #      create_table :job_levels do |t|
      #        t.integer :id
      #        t.string :name
      #
      #        t.timestamps
      #      end
      #
      #      JobLevel.reset_column_information
      #      %w{assistant executive manager director}.each do |type|
      #        JobLevel.create(name: type)
      #      end
      #    end
      #
      #    def down
      #      drop_table :job_levels
      #    end
      #  end
      def reset_column_information
        connection.clear_cache!
        undefine_attribute_methods
        connection.schema_cache.clear_table_cache!(table_name) if table_exists?

        @arel_engine             = nil
        @column_defaults         = nil
        @column_names            = nil
        @columns                 = nil
        @columns_hash            = nil
        @column_types            = nil
        @content_columns         = nil
        @dynamic_methods_hash    = nil
        @inheritance_column      = nil unless defined?(@explicit_inheritance_column) && @explicit_inheritance_column
        @relation                = nil
        @serialized_column_names = nil
        @time_zone_column_names  = nil
        @cached_time_zone        = nil
      end

      # This is a hook for use by modules that need to do extra stuff to
      # attributes when they are initialized. (e.g. attribute
      # serialization)
      def initialize_attributes(attributes, options = {}) #:nodoc:
        attributes
      end

      private

      # Guesses the table name, but does not decorate it with prefix and suffix information.
      def undecorated_table_name(class_name = base_class.name)
        table_name = class_name.to_s.demodulize.underscore
        pluralize_table_names ? table_name.pluralize : table_name
      end

      # Computes and returns a table name according to default conventions.
      def compute_table_name
        base = base_class
        if self == base
          # Nested classes are prefixed with singular parent table name.
          if parent < Base && !parent.abstract_class?
            contained = parent.table_name
            contained = contained.singularize if parent.pluralize_table_names
            contained += '_'
          end
          "#{full_table_name_prefix}#{contained}#{undecorated_table_name(name)}#{table_name_suffix}"
        else
          # STI subclasses always use their superclass' table.
          base.table_name
        end
      end
    end
  end
end
