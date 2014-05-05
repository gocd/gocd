require "cases/helper"
require 'models/developer'
require 'models/project'
require 'models/company'
require 'models/topic'
require 'models/reply'
require 'models/computer'
require 'models/customer'
require 'models/order'
require 'models/post'
require 'models/author'
require 'models/tag'
require 'models/tagging'
require 'models/comment'
require 'models/sponsor'
require 'models/member'
require 'models/essay'

class BelongsToAssociationsTest < ActiveRecord::TestCase
  fixtures :accounts, :companies, :developers, :projects, :topics,
           :developers_projects, :computers, :authors, :posts, :tags, :taggings, :comments

  def test_belongs_to
    Client.find(3).firm.name
    assert_equal companies(:first_firm).name, Client.find(3).firm.name
    assert !Client.find(3).firm.nil?, "Microsoft should have a firm"
  end

  def test_belongs_to_with_primary_key
    client = Client.create(:name => "Primary key client", :firm_name => companies(:first_firm).name)
    assert_equal companies(:first_firm).name, client.firm_with_primary_key.name
  end

  def test_belongs_to_with_primary_key_joins_on_correct_column
    sql = Client.send(:construct_finder_sql, :joins => :firm_with_primary_key)
    assert sql !~ /\.id/
    assert sql =~ /\.name/
  end

  def test_proxy_assignment
    account = Account.find(1)
    assert_nothing_raised { account.firm = account.firm }
  end

  def test_triple_equality
    assert Client.find(3).firm === Firm
    assert Firm === Client.find(3).firm
  end

  def test_type_mismatch
    assert_raise(ActiveRecord::AssociationTypeMismatch) { Account.find(1).firm = 1 }
    assert_raise(ActiveRecord::AssociationTypeMismatch) { Account.find(1).firm = Project.find(1) }
  end

  def test_natural_assignment
    apple = Firm.create("name" => "Apple")
    citibank = Account.create("credit_limit" => 10)
    citibank.firm = apple
    assert_equal apple.id, citibank.firm_id
  end

  def test_natural_assignment_with_primary_key
    apple = Firm.create("name" => "Apple")
    citibank = Client.create("name" => "Primary key client")
    citibank.firm_with_primary_key = apple
    assert_equal apple.name, citibank.firm_name
  end

  def test_eager_loading_with_primary_key
    apple = Firm.create("name" => "Apple")
    citibank = Client.create("name" => "Citibank", :firm_name => "Apple")
    citibank_result = Client.find(:first, :conditions => {:name => "Citibank"}, :include => :firm_with_primary_key)
    assert_not_nil citibank_result.instance_variable_get("@firm_with_primary_key")
  end

  def test_no_unexpected_aliasing
    first_firm = companies(:first_firm)
    another_firm = companies(:another_firm)

    citibank = Account.create("credit_limit" => 10)
    citibank.firm = first_firm
    original_proxy = citibank.firm
    citibank.firm = another_firm

    assert_equal first_firm.object_id, original_proxy.target.object_id
    assert_equal another_firm.object_id, citibank.firm.target.object_id
  end

  def test_creating_the_belonging_object
    citibank = Account.create("credit_limit" => 10)
    apple    = citibank.create_firm("name" => "Apple")
    assert_equal apple, citibank.firm
    citibank.save
    citibank.reload
    assert_equal apple, citibank.firm
  end

  def test_creating_the_belonging_object_with_primary_key
    client = Client.create(:name => "Primary key client")
    apple  = client.create_firm_with_primary_key("name" => "Apple")
    assert_equal apple, client.firm_with_primary_key
    client.save
    client.reload
    assert_equal apple, client.firm_with_primary_key
  end

  def test_building_the_belonging_object
    citibank = Account.create("credit_limit" => 10)
    apple    = citibank.build_firm("name" => "Apple")
    citibank.save
    assert_equal apple.id, citibank.firm_id
  end

  def test_building_the_belonging_object_with_primary_key
    client = Client.create(:name => "Primary key client")
    apple  = client.build_firm_with_primary_key("name" => "Apple")
    client.save
    assert_equal apple.name, client.firm_name
  end

  def test_natural_assignment_to_nil
    client = Client.find(3)
    client.firm = nil
    client.save
    assert_nil client.firm(true)
    assert_nil client.client_of
  end

  def test_natural_assignment_to_nil_with_primary_key
    client = Client.create(:name => "Primary key client", :firm_name => companies(:first_firm).name)
    client.firm_with_primary_key = nil
    client.save
    assert_nil client.firm_with_primary_key(true)
    assert_nil client.client_of
  end

  def test_with_different_class_name
    assert_equal Company.find(1).name, Company.find(3).firm_with_other_name.name
    assert_not_nil Company.find(3).firm_with_other_name, "Microsoft should have a firm"
  end

  def test_with_condition
    assert_equal Company.find(1).name, Company.find(3).firm_with_condition.name
    assert_not_nil Company.find(3).firm_with_condition, "Microsoft should have a firm"
  end

  def test_with_select
    assert_equal Company.find(2).firm_with_select.attributes.size, 1
    assert_equal Company.find(2, :include => :firm_with_select ).firm_with_select.attributes.size, 1
  end

  def test_belongs_to_counter
    debate = Topic.create("title" => "debate")
    assert_equal 0, debate.send(:read_attribute, "replies_count"), "No replies yet"

    trash = debate.replies.create("title" => "blah!", "content" => "world around!")
    assert_equal 1, Topic.find(debate.id).send(:read_attribute, "replies_count"), "First reply created"

    trash.destroy
    assert_equal 0, Topic.find(debate.id).send(:read_attribute, "replies_count"), "First reply deleted"
  end

  def test_belongs_to_with_primary_key_counter
    debate = Topic.create("title" => "debate")
    assert_equal 0, debate.send(:read_attribute, "replies_count"), "No replies yet"

    trash = debate.replies_with_primary_key.create("title" => "blah!", "content" => "world around!")
    assert_equal 1, Topic.find(debate.id).send(:read_attribute, "replies_count"), "First reply created"

    trash.destroy
    assert_equal 0, Topic.find(debate.id).send(:read_attribute, "replies_count"), "First reply deleted"
  end

  def test_belongs_to_counter_with_assigning_nil
    p = Post.find(1)
    c = Comment.find(1)

    assert_equal p.id, c.post_id
    assert_equal 2, Post.find(p.id).comments.size

    c.post = nil

    assert_equal 1, Post.find(p.id).comments.size
  end

  def test_belongs_to_with_primary_key_counter_with_assigning_nil
    debate = Topic.create("title" => "debate")
    reply  = Reply.create("title" => "blah!", "content" => "world around!", "parent_title" => "debate")

    assert_equal debate.title, reply.parent_title
    assert_equal 1, Topic.find(debate.id).send(:read_attribute, "replies_count")

    reply.topic_with_primary_key = nil

    assert_equal 0, Topic.find(debate.id).send(:read_attribute, "replies_count")
  end

  def test_belongs_to_counter_with_reassigning
    t1 = Topic.create("title" => "t1")
    t2 = Topic.create("title" => "t2")
    r1 = Reply.new("title" => "r1", "content" => "r1")
    r1.topic = t1

    assert r1.save
    assert_equal 1, Topic.find(t1.id).replies.size
    assert_equal 0, Topic.find(t2.id).replies.size

    r1.topic = Topic.find(t2.id)

    assert r1.save
    assert_equal 0, Topic.find(t1.id).replies.size
    assert_equal 1, Topic.find(t2.id).replies.size

    r1.topic = nil

    assert_equal 0, Topic.find(t1.id).replies.size
    assert_equal 0, Topic.find(t2.id).replies.size

    r1.topic = t1

    assert_equal 1, Topic.find(t1.id).replies.size
    assert_equal 0, Topic.find(t2.id).replies.size

    r1.destroy

    assert_equal 0, Topic.find(t1.id).replies.size
    assert_equal 0, Topic.find(t2.id).replies.size
  end

  def test_belongs_to_reassign_with_namespaced_models_and_counters
    t1 = Web::Topic.create("title" => "t1")
    t2 = Web::Topic.create("title" => "t2")
    r1 = Web::Reply.new("title" => "r1", "content" => "r1")
    r1.topic = t1

    assert r1.save
    assert_equal 1, Web::Topic.find(t1.id).replies.size
    assert_equal 0, Web::Topic.find(t2.id).replies.size

    r1.topic = Web::Topic.find(t2.id)

    assert r1.save
    assert_equal 0, Web::Topic.find(t1.id).replies.size
    assert_equal 1, Web::Topic.find(t2.id).replies.size
  end

  def test_belongs_to_counter_after_save
    topic = Topic.create!(:title => "monday night")
    topic.replies.create!(:title => "re: monday night", :content => "football")
    assert_equal 1, Topic.find(topic.id)[:replies_count]

    topic.save!
    assert_equal 1, Topic.find(topic.id)[:replies_count]
  end

  def test_belongs_to_counter_after_update_attributes
    topic = Topic.create!(:title => "37s")
    topic.replies.create!(:title => "re: 37s", :content => "rails")
    assert_equal 1, Topic.find(topic.id)[:replies_count]

    topic.update_attributes(:title => "37signals")
    assert_equal 1, Topic.find(topic.id)[:replies_count]
  end

  def test_assignment_before_child_saved
    final_cut = Client.new("name" => "Final Cut")
    firm = Firm.find(1)
    final_cut.firm = firm
    assert final_cut.new_record?
    assert final_cut.save
    assert !final_cut.new_record?
    assert !firm.new_record?
    assert_equal firm, final_cut.firm
    assert_equal firm, final_cut.firm(true)
  end

  def test_assignment_before_child_saved_with_primary_key
    final_cut = Client.new("name" => "Final Cut")
    firm = Firm.find(1)
    final_cut.firm_with_primary_key = firm
    assert final_cut.new_record?
    assert final_cut.save
    assert !final_cut.new_record?
    assert !firm.new_record?
    assert_equal firm, final_cut.firm_with_primary_key
    assert_equal firm, final_cut.firm_with_primary_key(true)
  end

  def test_new_record_with_foreign_key_but_no_object
    c = Client.new("firm_id" => 1)
    assert_equal Firm.find(:first), c.firm_with_basic_id
  end

  def test_forgetting_the_load_when_foreign_key_enters_late
    c = Client.new
    assert_nil c.firm_with_basic_id

    c.firm_id = 1
    assert_equal Firm.find(:first), c.firm_with_basic_id
  end

  def test_field_name_same_as_foreign_key
    computer = Computer.find(1)
    assert_not_nil computer.developer, ":foreign key == attribute didn't lock up" # '
  end

  def test_counter_cache
    topic = Topic.create :title => "Zoom-zoom-zoom"
    assert_equal 0, topic[:replies_count]

    reply = Reply.create(:title => "re: zoom", :content => "speedy quick!")
    reply.topic = topic

    assert_equal 1, topic.reload[:replies_count]
    assert_equal 1, topic.replies.size

    topic[:replies_count] = 15
    assert_equal 15, topic.replies.size
  end

  def test_custom_counter_cache
    reply = Reply.create(:title => "re: zoom", :content => "speedy quick!")
    assert_equal 0, reply[:replies_count]

    silly = SillyReply.create(:title => "gaga", :content => "boo-boo")
    silly.reply = reply

    assert_equal 1, reply.reload[:replies_count]
    assert_equal 1, reply.replies.size

    reply[:replies_count] = 17
    assert_equal 17, reply.replies.size
  end

  def test_association_assignment_sticks
    post = Post.find(:first)

    author1, author2 = Author.find(:all, :limit => 2)
    assert_not_nil author1
    assert_not_nil author2

    # make sure the association is loaded
    post.author

    # set the association by id, directly
    post.author_id = author2.id

    # save and reload
    post.save!
    post.reload

    # the author id of the post should be the id we set
    assert_equal post.author_id, author2.id
  end

  def test_cant_save_readonly_association
    assert_raise(ActiveRecord::ReadOnlyRecord) { companies(:first_client).readonly_firm.save! }
    assert companies(:first_client).readonly_firm.readonly?
  end
  
  def test_polymorphic_assignment_foreign_type_field_updating
    # should update when assigning a saved record
    sponsor = Sponsor.new
    member = Member.create
    sponsor.sponsorable = member
    assert_equal "Member", sponsor.sponsorable_type

    # should update when assigning a new record
    sponsor = Sponsor.new
    member = Member.new
    sponsor.sponsorable = member
    assert_equal "Member", sponsor.sponsorable_type
  end

  def test_polymorphic_assignment_with_primary_key_foreign_type_field_updating
    # should update when assigning a saved record
    essay = Essay.new
    writer = Author.create(:name => "David")
    essay.writer = writer
    assert_equal "Author", essay.writer_type

    # should update when assigning a new record
    essay = Essay.new
    writer = Author.new
    essay.writer = writer
    assert_equal "Author", essay.writer_type
  end

  def test_polymorphic_assignment_updates_foreign_id_field_for_new_and_saved_records
    sponsor = Sponsor.new
    saved_member = Member.create
    new_member = Member.new

    sponsor.sponsorable = saved_member
    assert_equal saved_member.id, sponsor.sponsorable_id

    sponsor.sponsorable = new_member
    assert_equal nil, sponsor.sponsorable_id
  end

  def test_polymorphic_assignment_with_primary_key_updates_foreign_id_field_for_new_and_saved_records
    essay = Essay.new
    saved_writer = Author.create(:name => "David")
    new_writer = Author.new

    essay.writer = saved_writer
    assert_equal saved_writer.name, essay.writer_id

    essay.writer = new_writer
    assert_equal nil, essay.writer_id
  end

  def test_belongs_to_proxy_should_not_respond_to_private_methods
    assert_raise(NoMethodError) { companies(:first_firm).private_method }
    assert_raise(NoMethodError) { companies(:second_client).firm.private_method }
  end

  def test_belongs_to_proxy_should_respond_to_private_methods_via_send
    companies(:first_firm).send(:private_method)
    companies(:second_client).firm.send(:private_method)
  end

  def test_save_of_record_with_loaded_belongs_to
    @account = companies(:first_firm).account

    assert_nothing_raised do
      Account.find(@account.id).save!
      Account.find(@account.id, :include => :firm).save!
    end

    @account.firm.delete

    assert_nothing_raised do
      Account.find(@account.id).save!
      Account.find(@account.id, :include => :firm).save!
    end
  end
end
