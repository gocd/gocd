require "cases/helper"
require 'models/topic'
require 'models/reply'
require 'models/subscriber'
require 'models/movie'
require 'models/keyboard'
require 'models/mixed_case_monkey'

class PrimaryKeysTest < ActiveRecord::TestCase
  fixtures :topics, :subscribers, :movies, :mixed_case_monkeys

  def test_integer_key
    topic = Topic.find(1)
    assert_equal(topics(:first).author_name, topic.author_name)
    topic = Topic.find(2)
    assert_equal(topics(:second).author_name, topic.author_name)

    topic = Topic.new
    topic.title = "New Topic"
    assert_equal(nil, topic.id)
    assert_nothing_raised { topic.save! }
    id = topic.id

    topicReloaded = Topic.find(id)
    assert_equal("New Topic", topicReloaded.title)
  end

  def test_customized_primary_key_auto_assigns_on_save
    Keyboard.delete_all
    keyboard = Keyboard.new(:name => 'HHKB')
    assert_nothing_raised { keyboard.save! }
    assert_equal keyboard.id, Keyboard.find_by_name('HHKB').id
  end

  def test_customized_primary_key_can_be_get_before_saving
    keyboard = Keyboard.new
    assert_nil keyboard.id
    assert_nothing_raised { assert_nil keyboard.key_number }
  end

  def test_customized_string_primary_key_settable_before_save
    subscriber = Subscriber.new
    assert_nothing_raised { subscriber.id = 'webster123' }
    assert_equal 'webster123', subscriber.id
    assert_equal 'webster123', subscriber.nick
  end

  def test_string_key
    subscriber = Subscriber.find(subscribers(:first).nick)
    assert_equal(subscribers(:first).name, subscriber.name)
    subscriber = Subscriber.find(subscribers(:second).nick)
    assert_equal(subscribers(:second).name, subscriber.name)

    subscriber = Subscriber.new
    subscriber.id = "jdoe"
    assert_equal("jdoe", subscriber.id)
    subscriber.name = "John Doe"
    assert_nothing_raised { subscriber.save! }
    assert_equal("jdoe", subscriber.id)

    subscriberReloaded = Subscriber.find("jdoe")
    assert_equal("John Doe", subscriberReloaded.name)
  end

  def test_find_with_more_than_one_string_key
    assert_equal 2, Subscriber.find(subscribers(:first).nick, subscribers(:second).nick).length
  end

  def test_primary_key_prefix
    ActiveRecord::Base.primary_key_prefix_type = :table_name
    Topic.reset_primary_key
    assert_equal "topicid", Topic.primary_key

    ActiveRecord::Base.primary_key_prefix_type = :table_name_with_underscore
    Topic.reset_primary_key
    assert_equal "topic_id", Topic.primary_key

    ActiveRecord::Base.primary_key_prefix_type = nil
    Topic.reset_primary_key
    assert_equal "id", Topic.primary_key
  end

  def test_delete_should_quote_pkey
    assert_nothing_raised { MixedCaseMonkey.delete(1) }
  end
  def test_update_counters_should_quote_pkey_and_quote_counter_columns
    assert_nothing_raised { MixedCaseMonkey.update_counters(1, :fleaCount => 99) }
  end
  def test_find_with_one_id_should_quote_pkey
    assert_nothing_raised { MixedCaseMonkey.find(1) }
  end
  def test_find_with_multiple_ids_should_quote_pkey
    assert_nothing_raised { MixedCaseMonkey.find([1,2]) }
  end
  def test_instance_update_should_quote_pkey
    assert_nothing_raised { MixedCaseMonkey.find(1).save }
  end
  def test_instance_destroy_should_quote_pkey
    assert_nothing_raised { MixedCaseMonkey.find(1).destroy }
  end

  def test_supports_primary_key
    assert_nothing_raised NoMethodError do
      ActiveRecord::Base.connection.supports_primary_key?
    end
  end

  def test_primary_key_returns_value_if_it_exists
    if ActiveRecord::Base.connection.supports_primary_key?
      assert_equal 'id', ActiveRecord::Base.connection.primary_key('developers')
    end
  end

  def test_primary_key_returns_nil_if_it_does_not_exist
    if ActiveRecord::Base.connection.supports_primary_key?
      assert_nil ActiveRecord::Base.connection.primary_key('developers_projects')
    end
  end
end
