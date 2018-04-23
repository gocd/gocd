module ActiveRecord
  module ConnectionAdapters #:nodoc:
    # Abstract representation of an index definition on a table. Instances of
    # this type are typically created and returned by methods in database
    # adapters. e.g. ActiveRecord::ConnectionAdapters::AbstractMysqlAdapter#indexes
    IndexDefinition = Struct.new(:table, :name, :unique, :columns, :lengths, :orders, :where, :type, :using, :comment) #:nodoc:

    # Abstract representation of a column definition. Instances of this type
    # are typically created by methods in TableDefinition, and added to the
    # +columns+ attribute of said TableDefinition object, in order to be used
    # for generating a number of table creation or table changing SQL statements.
    ColumnDefinition = Struct.new(:name, :type, :options, :sql_type) do # :nodoc:
      def primary_key?
        options[:primary_key]
      end

      [:limit, :precision, :scale, :default, :null, :collation, :comment].each do |option_name|
        module_eval <<-CODE, __FILE__, __LINE__ + 1
          def #{option_name}
            options[:#{option_name}]
          end

          def #{option_name}=(value)
            options[:#{option_name}] = value
          end
        CODE
      end
    end

    AddColumnDefinition = Struct.new(:column) # :nodoc:

    ChangeColumnDefinition = Struct.new(:column, :name) #:nodoc:

    PrimaryKeyDefinition = Struct.new(:name) # :nodoc:

    ForeignKeyDefinition = Struct.new(:from_table, :to_table, :options) do #:nodoc:
      def name
        options[:name]
      end

      def column
        options[:column]
      end

      def primary_key
        options[:primary_key] || default_primary_key
      end

      def on_delete
        options[:on_delete]
      end

      def on_update
        options[:on_update]
      end

      def custom_primary_key?
        options[:primary_key] != default_primary_key
      end

      def defined_for?(to_table_ord = nil, to_table: nil, **options)
        if to_table_ord
          self.to_table == to_table_ord.to_s
        else
          (to_table.nil? || to_table.to_s == self.to_table) &&
            options.all? { |k, v| self.options[k].to_s == v.to_s }
        end
      end

      private
        def default_primary_key
          "id"
        end
    end

    class ReferenceDefinition # :nodoc:
      def initialize(
        name,
        polymorphic: false,
        index: true,
        foreign_key: false,
        type: :bigint,
        **options
      )
        @name = name
        @polymorphic = polymorphic
        @index = index
        @foreign_key = foreign_key
        @type = type
        @options = options

        if polymorphic && foreign_key
          raise ArgumentError, "Cannot add a foreign key to a polymorphic relation"
        end
      end

      def add_to(table)
        columns.each do |column_options|
          table.column(*column_options)
        end

        if index
          table.index(column_names, index_options)
        end

        if foreign_key
          table.foreign_key(foreign_table_name, foreign_key_options)
        end
      end

      # TODO Change this to private once we've dropped Ruby 2.2 support.
      # Workaround for Ruby 2.2 "private attribute?" warning.
      protected

        attr_reader :name, :polymorphic, :index, :foreign_key, :type, :options

      private

        def as_options(value)
          value.is_a?(Hash) ? value : {}
        end

        def polymorphic_options
          as_options(polymorphic).merge(null: options[:null])
        end

        def index_options
          as_options(index)
        end

        def foreign_key_options
          as_options(foreign_key).merge(column: column_name)
        end

        def columns
          result = [[column_name, type, options]]
          if polymorphic
            result.unshift(["#{name}_type", :string, polymorphic_options])
          end
          result
        end

        def column_name
          "#{name}_id"
        end

        def column_names
          columns.map(&:first)
        end

        def foreign_table_name
          foreign_key_options.fetch(:to_table) do
            Base.pluralize_table_names ? name.to_s.pluralize : name
          end
        end
    end

    module ColumnMethods
      # Appends a primary key definition to the table definition.
      # Can be called multiple times, but this is probably not a good idea.
      def primary_key(name, type = :primary_key, **options)
        column(name, type, options.merge(primary_key: true))
      end

      # Appends a column or columns of a specified type.
      #
      #  t.string(:goat)
      #  t.string(:goat, :sheep)
      #
      # See TableDefinition#column
      [
        :bigint,
        :binary,
        :boolean,
        :date,
        :datetime,
        :decimal,
        :float,
        :integer,
        :string,
        :text,
        :time,
        :timestamp,
        :virtual,
      ].each do |column_type|
        module_eval <<-CODE, __FILE__, __LINE__ + 1
          def #{column_type}(*args, **options)
            args.each { |name| column(name, :#{column_type}, options) }
          end
        CODE
      end
      alias_method :numeric, :decimal
    end

    # Represents the schema of an SQL table in an abstract way. This class
    # provides methods for manipulating the schema representation.
    #
    # Inside migration files, the +t+ object in {create_table}[rdoc-ref:SchemaStatements#create_table]
    # is actually of this type:
    #
    #   class SomeMigration < ActiveRecord::Migration[5.0]
    #     def up
    #       create_table :foo do |t|
    #         puts t.class  # => "ActiveRecord::ConnectionAdapters::TableDefinition"
    #       end
    #     end
    #
    #     def down
    #       ...
    #     end
    #   end
    #
    class TableDefinition
      include ColumnMethods

      attr_accessor :indexes
      attr_reader :name, :temporary, :options, :as, :foreign_keys, :comment

      def initialize(name, temporary = false, options = nil, as = nil, comment: nil)
        @columns_hash = {}
        @indexes = []
        @foreign_keys = []
        @primary_keys = nil
        @temporary = temporary
        @options = options
        @as = as
        @name = name
        @comment = comment
      end

      def primary_keys(name = nil) # :nodoc:
        @primary_keys = PrimaryKeyDefinition.new(name) if name
        @primary_keys
      end

      # Returns an array of ColumnDefinition objects for the columns of the table.
      def columns; @columns_hash.values; end

      # Returns a ColumnDefinition for the column with name +name+.
      def [](name)
        @columns_hash[name.to_s]
      end

      # Instantiates a new column for the table.
      # See {connection.add_column}[rdoc-ref:ConnectionAdapters::SchemaStatements#add_column]
      # for available options.
      #
      # Additional options are:
      # * <tt>:index</tt> -
      #   Create an index for the column. Can be either <tt>true</tt> or an options hash.
      #
      # This method returns <tt>self</tt>.
      #
      # == Examples
      #
      #  # Assuming +td+ is an instance of TableDefinition
      #  td.column(:granted, :boolean, index: true)
      #
      # == Short-hand examples
      #
      # Instead of calling #column directly, you can also work with the short-hand definitions for the default types.
      # They use the type as the method name instead of as a parameter and allow for multiple columns to be defined
      # in a single statement.
      #
      # What can be written like this with the regular calls to column:
      #
      #   create_table :products do |t|
      #     t.column :shop_id,     :integer
      #     t.column :creator_id,  :integer
      #     t.column :item_number, :string
      #     t.column :name,        :string, default: "Untitled"
      #     t.column :value,       :string, default: "Untitled"
      #     t.column :created_at,  :datetime
      #     t.column :updated_at,  :datetime
      #   end
      #   add_index :products, :item_number
      #
      # can also be written as follows using the short-hand:
      #
      #   create_table :products do |t|
      #     t.integer :shop_id, :creator_id
      #     t.string  :item_number, index: true
      #     t.string  :name, :value, default: "Untitled"
      #     t.timestamps null: false
      #   end
      #
      # There's a short-hand method for each of the type values declared at the top. And then there's
      # TableDefinition#timestamps that'll add +created_at+ and +updated_at+ as datetimes.
      #
      # TableDefinition#references will add an appropriately-named _id column, plus a corresponding _type
      # column if the <tt>:polymorphic</tt> option is supplied. If <tt>:polymorphic</tt> is a hash of
      # options, these will be used when creating the <tt>_type</tt> column. The <tt>:index</tt> option
      # will also create an index, similar to calling {add_index}[rdoc-ref:ConnectionAdapters::SchemaStatements#add_index].
      # So what can be written like this:
      #
      #   create_table :taggings do |t|
      #     t.integer :tag_id, :tagger_id, :taggable_id
      #     t.string  :tagger_type
      #     t.string  :taggable_type, default: 'Photo'
      #   end
      #   add_index :taggings, :tag_id, name: 'index_taggings_on_tag_id'
      #   add_index :taggings, [:tagger_id, :tagger_type]
      #
      # Can also be written as follows using references:
      #
      #   create_table :taggings do |t|
      #     t.references :tag, index: { name: 'index_taggings_on_tag_id' }
      #     t.references :tagger, polymorphic: true, index: true
      #     t.references :taggable, polymorphic: { default: 'Photo' }
      #   end
      def column(name, type, options = {})
        name = name.to_s
        type = type.to_sym if type
        options = options.dup

        if @columns_hash[name] && @columns_hash[name].primary_key?
          raise ArgumentError, "you can't redefine the primary key column '#{name}'. To define a custom primary key, pass { id: false } to create_table."
        end

        index_options = options.delete(:index)
        index(name, index_options.is_a?(Hash) ? index_options : {}) if index_options
        @columns_hash[name] = new_column_definition(name, type, options)
        self
      end

      # remove the column +name+ from the table.
      #   remove_column(:account_id)
      def remove_column(name)
        @columns_hash.delete name.to_s
      end

      # Adds index options to the indexes hash, keyed by column name
      # This is primarily used to track indexes that need to be created after the table
      #
      #   index(:account_id, name: 'index_projects_on_account_id')
      def index(column_name, options = {})
        indexes << [column_name, options]
      end

      def foreign_key(table_name, options = {}) # :nodoc:
        table_name_prefix = ActiveRecord::Base.table_name_prefix
        table_name_suffix = ActiveRecord::Base.table_name_suffix
        table_name = "#{table_name_prefix}#{table_name}#{table_name_suffix}"
        foreign_keys.push([table_name, options])
      end

      # Appends <tt>:datetime</tt> columns <tt>:created_at</tt> and
      # <tt>:updated_at</tt> to the table. See {connection.add_timestamps}[rdoc-ref:SchemaStatements#add_timestamps]
      #
      #   t.timestamps null: false
      def timestamps(**options)
        options[:null] = false if options[:null].nil?

        column(:created_at, :datetime, options)
        column(:updated_at, :datetime, options)
      end

      # Adds a reference.
      #
      #  t.references(:user)
      #  t.belongs_to(:supplier, foreign_key: true)
      #
      # See {connection.add_reference}[rdoc-ref:SchemaStatements#add_reference] for details of the options you can use.
      def references(*args, **options)
        args.each do |ref_name|
          ReferenceDefinition.new(ref_name, options).add_to(self)
        end
      end
      alias :belongs_to :references

      def new_column_definition(name, type, **options) # :nodoc:
        type = aliased_types(type.to_s, type)
        options[:primary_key] ||= type == :primary_key
        options[:null] = false if options[:primary_key]
        create_column_definition(name, type, options)
      end

      private
        def create_column_definition(name, type, options)
          ColumnDefinition.new(name, type, options)
        end

        def aliased_types(name, fallback)
          "timestamp" == name ? :datetime : fallback
        end
    end

    class AlterTable # :nodoc:
      attr_reader :adds
      attr_reader :foreign_key_adds
      attr_reader :foreign_key_drops

      def initialize(td)
        @td   = td
        @adds = []
        @foreign_key_adds = []
        @foreign_key_drops = []
      end

      def name; @td.name; end

      def add_foreign_key(to_table, options)
        @foreign_key_adds << ForeignKeyDefinition.new(name, to_table, options)
      end

      def drop_foreign_key(name)
        @foreign_key_drops << name
      end

      def add_column(name, type, options)
        name = name.to_s
        type = type.to_sym
        @adds << AddColumnDefinition.new(@td.new_column_definition(name, type, options))
      end
    end

    # Represents an SQL table in an abstract way for updating a table.
    # Also see TableDefinition and {connection.create_table}[rdoc-ref:SchemaStatements#create_table]
    #
    # Available transformations are:
    #
    #   change_table :table do |t|
    #     t.primary_key
    #     t.column
    #     t.index
    #     t.rename_index
    #     t.timestamps
    #     t.change
    #     t.change_default
    #     t.rename
    #     t.references
    #     t.belongs_to
    #     t.string
    #     t.text
    #     t.integer
    #     t.bigint
    #     t.float
    #     t.decimal
    #     t.numeric
    #     t.datetime
    #     t.timestamp
    #     t.time
    #     t.date
    #     t.binary
    #     t.boolean
    #     t.remove
    #     t.remove_references
    #     t.remove_belongs_to
    #     t.remove_index
    #     t.remove_timestamps
    #   end
    #
    class Table
      include ColumnMethods

      attr_reader :name

      def initialize(table_name, base)
        @name = table_name
        @base = base
      end

      # Adds a new column to the named table.
      #
      #  t.column(:name, :string)
      #
      # See TableDefinition#column for details of the options you can use.
      def column(column_name, type, options = {})
        @base.add_column(name, column_name, type, options)
      end

      # Checks to see if a column exists.
      #
      #  t.string(:name) unless t.column_exists?(:name, :string)
      #
      # See {connection.column_exists?}[rdoc-ref:SchemaStatements#column_exists?]
      def column_exists?(column_name, type = nil, options = {})
        @base.column_exists?(name, column_name, type, options)
      end

      # Adds a new index to the table. +column_name+ can be a single Symbol, or
      # an Array of Symbols.
      #
      #  t.index(:name)
      #  t.index([:branch_id, :party_id], unique: true)
      #  t.index([:branch_id, :party_id], unique: true, name: 'by_branch_party')
      #
      # See {connection.add_index}[rdoc-ref:SchemaStatements#add_index] for details of the options you can use.
      def index(column_name, options = {})
        @base.add_index(name, column_name, options)
      end

      # Checks to see if an index exists.
      #
      #  unless t.index_exists?(:branch_id)
      #    t.index(:branch_id)
      #  end
      #
      # See {connection.index_exists?}[rdoc-ref:SchemaStatements#index_exists?]
      def index_exists?(column_name, options = {})
        @base.index_exists?(name, column_name, options)
      end

      # Renames the given index on the table.
      #
      #  t.rename_index(:user_id, :account_id)
      #
      # See {connection.rename_index}[rdoc-ref:SchemaStatements#rename_index]
      def rename_index(index_name, new_index_name)
        @base.rename_index(name, index_name, new_index_name)
      end

      # Adds timestamps (+created_at+ and +updated_at+) columns to the table.
      #
      #  t.timestamps(null: false)
      #
      # See {connection.add_timestamps}[rdoc-ref:SchemaStatements#add_timestamps]
      def timestamps(options = {})
        @base.add_timestamps(name, options)
      end

      # Changes the column's definition according to the new options.
      #
      #  t.change(:name, :string, limit: 80)
      #  t.change(:description, :text)
      #
      # See TableDefinition#column for details of the options you can use.
      def change(column_name, type, options = {})
        @base.change_column(name, column_name, type, options)
      end

      # Sets a new default value for a column.
      #
      #  t.change_default(:qualification, 'new')
      #  t.change_default(:authorized, 1)
      #  t.change_default(:status, from: nil, to: "draft")
      #
      # See {connection.change_column_default}[rdoc-ref:SchemaStatements#change_column_default]
      def change_default(column_name, default_or_changes)
        @base.change_column_default(name, column_name, default_or_changes)
      end

      # Removes the column(s) from the table definition.
      #
      #  t.remove(:qualification)
      #  t.remove(:qualification, :experience)
      #
      # See {connection.remove_columns}[rdoc-ref:SchemaStatements#remove_columns]
      def remove(*column_names)
        @base.remove_columns(name, *column_names)
      end

      # Removes the given index from the table.
      #
      #   t.remove_index(:branch_id)
      #   t.remove_index(column: [:branch_id, :party_id])
      #   t.remove_index(name: :by_branch_party)
      #
      # See {connection.remove_index}[rdoc-ref:SchemaStatements#remove_index]
      def remove_index(options = {})
        @base.remove_index(name, options)
      end

      # Removes the timestamp columns (+created_at+ and +updated_at+) from the table.
      #
      #  t.remove_timestamps
      #
      # See {connection.remove_timestamps}[rdoc-ref:SchemaStatements#remove_timestamps]
      def remove_timestamps(options = {})
        @base.remove_timestamps(name, options)
      end

      # Renames a column.
      #
      #  t.rename(:description, :name)
      #
      # See {connection.rename_column}[rdoc-ref:SchemaStatements#rename_column]
      def rename(column_name, new_column_name)
        @base.rename_column(name, column_name, new_column_name)
      end

      # Adds a reference.
      #
      #  t.references(:user)
      #  t.belongs_to(:supplier, foreign_key: true)
      #
      # See {connection.add_reference}[rdoc-ref:SchemaStatements#add_reference] for details of the options you can use.
      def references(*args, **options)
        args.each do |ref_name|
          @base.add_reference(name, ref_name, options)
        end
      end
      alias :belongs_to :references

      # Removes a reference. Optionally removes a +type+ column.
      #
      #  t.remove_references(:user)
      #  t.remove_belongs_to(:supplier, polymorphic: true)
      #
      # See {connection.remove_reference}[rdoc-ref:SchemaStatements#remove_reference]
      def remove_references(*args, **options)
        args.each do |ref_name|
          @base.remove_reference(name, ref_name, options)
        end
      end
      alias :remove_belongs_to :remove_references

      # Adds a foreign key.
      #
      # t.foreign_key(:authors)
      #
      # See {connection.add_foreign_key}[rdoc-ref:SchemaStatements#add_foreign_key]
      def foreign_key(*args) # :nodoc:
        @base.add_foreign_key(name, *args)
      end

      # Checks to see if a foreign key exists.
      #
      # t.foreign_key(:authors) unless t.foreign_key_exists?(:authors)
      #
      # See {connection.foreign_key_exists?}[rdoc-ref:SchemaStatements#foreign_key_exists?]
      def foreign_key_exists?(*args) # :nodoc:
        @base.foreign_key_exists?(name, *args)
      end
    end
  end
end
