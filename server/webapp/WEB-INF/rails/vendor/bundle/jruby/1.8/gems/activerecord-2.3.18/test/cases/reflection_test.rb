require "cases/helper"
require 'models/topic'
require 'models/customer'
require 'models/company'
require 'models/company_in_module'
require 'models/subscriber'
require 'models/ship'
require 'models/pirate'

class ReflectionTest < ActiveRecord::TestCase
  include ActiveRecord::Reflection

  fixtures :topics, :customers, :companies, :subscribers

  def setup
    @first = Topic.find(1)
  end

  def test_column_null_not_null
    subscriber = Subscriber.find(:first)
    assert subscriber.column_for_attribute("name").null
    assert !subscriber.column_for_attribute("nick").null
  end

  def test_read_attribute_names
    assert_equal(
      %w( id title author_name author_email_address bonus_time written_on last_read content group approved replies_count parent_id parent_title type ).sort,
      @first.attribute_names
    )
  end

  def test_columns
    assert_equal 14, Topic.columns.length
  end

  def test_columns_are_returned_in_the_order_they_were_declared
    column_names = Topic.columns.map { |column| column.name }
    assert_equal %w(id title author_name author_email_address written_on bonus_time last_read content approved replies_count parent_id parent_title type group), column_names
  end

  def test_content_columns
    content_columns        = Topic.content_columns
    content_column_names   = content_columns.map {|column| column.name}
    assert_equal 10, content_columns.length
    assert_equal %w(title author_name author_email_address written_on bonus_time last_read content group approved parent_title).sort, content_column_names.sort
  end

  def test_column_string_type_and_limit
    assert_equal :string, @first.column_for_attribute("title").type
    assert_equal 255, @first.column_for_attribute("title").limit
  end

  def test_column_null_not_null
    subscriber = Subscriber.find(:first)
    assert subscriber.column_for_attribute("name").null
    assert !subscriber.column_for_attribute("nick").null
  end

  def test_human_name_for_column
    assert_equal "Author name", @first.column_for_attribute("author_name").human_name
  end

  def test_integer_columns
    assert_equal :integer, @first.column_for_attribute("id").type
  end

  def test_reflection_klass_for_nested_class_name
    reflection = MacroReflection.new(nil, nil, { :class_name => 'MyApplication::Business::Company' }, nil)
    assert_nothing_raised do
      assert_equal MyApplication::Business::Company, reflection.klass
    end
  end

  def test_aggregation_reflection
    reflection_for_address = AggregateReflection.new(
      :composed_of, :address, { :mapping => [ %w(address_street street), %w(address_city city), %w(address_country country) ] }, Customer
    )

    reflection_for_balance = AggregateReflection.new(
      :composed_of, :balance, { :class_name => "Money", :mapping => %w(balance amount) }, Customer
    )

    reflection_for_gps_location = AggregateReflection.new(
      :composed_of, :gps_location, { }, Customer
    )

    assert Customer.reflect_on_all_aggregations.include?(reflection_for_gps_location)
    assert Customer.reflect_on_all_aggregations.include?(reflection_for_balance)
    assert Customer.reflect_on_all_aggregations.include?(reflection_for_address)

    assert_equal reflection_for_address, Customer.reflect_on_aggregation(:address)

    assert_equal Address, Customer.reflect_on_aggregation(:address).klass

    assert_equal Money, Customer.reflect_on_aggregation(:balance).klass
  end

  def test_reflect_on_all_autosave_associations
    expected = Pirate.reflect_on_all_associations.select { |r| r.options[:autosave] }
    received = Pirate.reflect_on_all_autosave_associations

    assert !received.empty?
    assert_not_equal Pirate.reflect_on_all_associations.length, received.length
    assert_equal expected, received
  end

  def test_has_many_reflection
    reflection_for_clients = AssociationReflection.new(:has_many, :clients, { :order => "id", :dependent => :destroy }, Firm)

    assert_equal reflection_for_clients, Firm.reflect_on_association(:clients)

    assert_equal Client, Firm.reflect_on_association(:clients).klass
    assert_equal 'companies', Firm.reflect_on_association(:clients).table_name

    assert_equal Client, Firm.reflect_on_association(:clients_of_firm).klass
    assert_equal 'companies', Firm.reflect_on_association(:clients_of_firm).table_name
  end

  def test_has_one_reflection
    reflection_for_account = AssociationReflection.new(:has_one, :account, { :foreign_key => "firm_id", :dependent => :destroy }, Firm)
    assert_equal reflection_for_account, Firm.reflect_on_association(:account)

    assert_equal Account, Firm.reflect_on_association(:account).klass
    assert_equal 'accounts', Firm.reflect_on_association(:account).table_name
  end

  def test_belongs_to_inferred_foreign_key_from_assoc_name
    Company.belongs_to :foo
    assert_equal "foo_id", Company.reflect_on_association(:foo).primary_key_name
    Company.belongs_to :bar, :class_name => "Xyzzy"
    assert_equal "bar_id", Company.reflect_on_association(:bar).primary_key_name
    Company.belongs_to :baz, :class_name => "Xyzzy", :foreign_key => "xyzzy_id"
    assert_equal "xyzzy_id", Company.reflect_on_association(:baz).primary_key_name
  end

  def test_association_reflection_in_modules
    assert_reflection MyApplication::Business::Firm,
      :clients_of_firm,
      :klass      => MyApplication::Business::Client,
      :class_name => 'Client',
      :table_name => 'companies'

    assert_reflection MyApplication::Billing::Account,
      :firm,
      :klass      => MyApplication::Business::Firm,
      :class_name => 'MyApplication::Business::Firm',
      :table_name => 'companies'

    assert_reflection MyApplication::Billing::Account,
      :qualified_billing_firm,
      :klass      => MyApplication::Billing::Firm,
      :class_name => 'MyApplication::Billing::Firm',
      :table_name => 'companies'

    assert_reflection MyApplication::Billing::Account,
      :unqualified_billing_firm,
      :klass      => MyApplication::Billing::Firm,
      :class_name => 'Firm',
      :table_name => 'companies'

    assert_reflection MyApplication::Billing::Account,
      :nested_qualified_billing_firm,
      :klass      => MyApplication::Billing::Nested::Firm,
      :class_name => 'MyApplication::Billing::Nested::Firm',
      :table_name => 'companies'

    assert_reflection MyApplication::Billing::Account,
      :nested_unqualified_billing_firm,
      :klass      => MyApplication::Billing::Nested::Firm,
      :class_name => 'Nested::Firm',
      :table_name => 'companies'
  end

  def test_reflection_of_all_associations
    # FIXME these assertions bust a lot
    assert_equal 36, Firm.reflect_on_all_associations.size
    assert_equal 26, Firm.reflect_on_all_associations(:has_many).size
    assert_equal 10, Firm.reflect_on_all_associations(:has_one).size
    assert_equal 0, Firm.reflect_on_all_associations(:belongs_to).size
  end

  def test_reflection_should_not_raise_error_when_compared_to_other_object
    assert_nothing_raised { Firm.reflections[:clients] == Object.new }
  end

  def test_has_many_through_reflection
    assert_kind_of ThroughReflection, Subscriber.reflect_on_association(:books)
  end

  def test_collection_association
    assert Pirate.reflect_on_association(:birds).collection?
    assert Pirate.reflect_on_association(:parrots).collection?

    assert !Pirate.reflect_on_association(:ship).collection?
    assert !Ship.reflect_on_association(:pirate).collection?
  end

  def test_default_association_validation
    assert AssociationReflection.new(:has_many, :clients, {}, Firm).validate?

    assert !AssociationReflection.new(:has_one, :client, {}, Firm).validate?
    assert !AssociationReflection.new(:belongs_to, :client, {}, Firm).validate?
    assert !AssociationReflection.new(:has_and_belongs_to_many, :clients, {}, Firm).validate?
  end

  def test_always_validate_association_if_explicit
    assert AssociationReflection.new(:has_one, :client, { :validate => true }, Firm).validate?
    assert AssociationReflection.new(:belongs_to, :client, { :validate => true }, Firm).validate?
    assert AssociationReflection.new(:has_many, :clients, { :validate => true }, Firm).validate?
    assert AssociationReflection.new(:has_and_belongs_to_many, :clients, { :validate => true }, Firm).validate?
  end

  def test_validate_association_if_autosave
    assert AssociationReflection.new(:has_one, :client, { :autosave => true }, Firm).validate?
    assert AssociationReflection.new(:belongs_to, :client, { :autosave => true }, Firm).validate?
    assert AssociationReflection.new(:has_many, :clients, { :autosave => true }, Firm).validate?
    assert AssociationReflection.new(:has_and_belongs_to_many, :clients, { :autosave => true }, Firm).validate?
  end

  def test_never_validate_association_if_explicit
    assert !AssociationReflection.new(:has_one, :client, { :autosave => true, :validate => false }, Firm).validate?
    assert !AssociationReflection.new(:belongs_to, :client, { :autosave => true, :validate => false }, Firm).validate?
    assert !AssociationReflection.new(:has_many, :clients, { :autosave => true, :validate => false }, Firm).validate?
    assert !AssociationReflection.new(:has_and_belongs_to_many, :clients, { :autosave => true, :validate => false }, Firm).validate?
  end

  private
    def assert_reflection(klass, association, options)
      assert reflection = klass.reflect_on_association(association)
      options.each do |method, value|
        assert_equal(value, reflection.send(method))
      end
    end
end
