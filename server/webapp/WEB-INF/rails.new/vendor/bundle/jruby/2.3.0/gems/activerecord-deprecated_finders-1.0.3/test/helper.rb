require 'bundler/setup'
require 'minitest/spec'
require 'minitest/autorun'
require 'active_record'
require 'active_record/deprecated_finders'

ActiveRecord::Base.establish_connection(adapter: 'sqlite3', database: ':memory:')

ActiveRecord::Schema.verbose = false
ActiveRecord::Schema.define do
  create_table :posts do |t|
    t.string :title
    t.string :category
  end

  create_table :comments do |t|
    t.string :title
    t.references :post
  end

  create_table :appointments do |t|
    t.integer :physician_id
    t.integer :patient_id
    t.string :week_day
    t.string :status
  end

  create_table :physicians do |t|
    t.string :name
  end

  create_table :patients do |t|
    t.string :name
  end
end

class Post < ActiveRecord::Base
  has_many :comments
end

class Comment < ActiveRecord::Base
  def self.lol
    "lol"
  end
end

class Appointment < ActiveRecord::Base
  belongs_to :physician
  belongs_to :patient
end

class Patient < ActiveRecord::Base
  def self.find_by_custom_name
    []
  end
end

class Physician < ActiveRecord::Base
  has_many :appointments
  has_many :patients, through: :appointments
end

require 'active_support/testing/deprecation'
ActiveSupport::Deprecation.debug = true

class MiniTest::Spec
  include ActiveSupport::Testing::Deprecation
end
