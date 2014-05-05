class InnocentJointable < ActiveRecord::Migration
  def self.up
    create_table("people_reminders", :id => false) do |t|
      t.column :reminder_id, :integer
      t.column :person_id, :integer
    end
  end

  def self.down
    drop_table "people_reminders"
  end
end