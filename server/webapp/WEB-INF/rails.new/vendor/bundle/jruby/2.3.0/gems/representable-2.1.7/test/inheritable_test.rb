require 'test_helper'

# tests Inheritable:: classes (the #inherit! method). This can be moved to uber if necessary.

class ConfigInheritableTest < MiniTest::Spec
  class CloneableObject
    include Representable::Cloneable

    # same instance returns same clone.
    def clone
      @clone ||= super
    end
  end


  # Inheritable::Array
  it do
    parent = Representable::Inheritable::Array.new([1,2,3])
    child  = Representable::Inheritable::Array.new([4])

    child.inherit!(parent).must_equal([4,1,2,3])
  end

  # Inheritable::Hash
  Inheritable = Representable::Inheritable
  describe "Inheritable::Hash" do
    it do
      parent = Inheritable::Hash[
        :volume => volume = Uber::Options::Value.new(9),
        :genre  => "Powermetal",
        :only_parent => only_parent = Representable::Inheritable::Array["Pumpkin Box"],
        :in_both     => in_both     = Representable::Inheritable::Array["Roxanne"],
        :hash => {:type => :parent},
        :clone => parent_clone = CloneableObject.new # cloneable is in both hashes.
      ]
      child  = Inheritable::Hash[
        :genre => "Metal",
        :pitch => 99,
        :in_both => Representable::Inheritable::Array["Generator"],
        :hash => {:type => :child},
        :clone => child_clone = CloneableObject.new
      ]

      child.inherit!(parent)

      # order:
      child.to_a.must_equal [
        [:genre, "Powermetal"], # parent overrides child
        [:pitch, 99],           # parent doesn't define pitch
        [:in_both, ["Generator", "Roxanne"]], # Inheritable array gets "merged".
        [:hash, {:type => :parent}], # normal hash merge: parent overwrites child value.
        [:clone, parent_clone.clone],
        [:volume, volume],
        [:only_parent, ["Pumpkin Box"]],
      ]

      # clone
      child[:only_parent].object_id.wont_equal parent[:only_parent].object_id
      child[:clone].object_id.wont_equal parent[:clone].object_id

      # still a hash:
      child.must_equal(
        :genre => "Powermetal",
        :pitch => 99,
        :in_both => ["Generator", "Roxanne"],
        :hash => {:type => :parent},
        :clone => parent_clone.clone,
        :volume => volume,
        :only_parent => ["Pumpkin Box"]
      )
    end

    # nested:
    it do
      parent = Inheritable::Hash[
        :details => Inheritable::Hash[
          :title  => title  = "Man Of Steel",
          :length => length = Representable::Definition.new(:length) # Cloneable.
      ]]

      child  = Inheritable::Hash[].inherit!(parent)
      child[:details][:track] = 1

      parent.must_equal({:details => {:title => "Man Of Steel", :length => length}})

      child.keys.must_equal [:details]
      child[:details].keys.must_equal [:title, :length, :track]
      child[:details][:title].must_equal "Man Of Steel"
      child[:details][:track].must_equal 1
      child[:details][:length].name.must_equal "length"

      # clone
      child[:details][:title].object_id.must_equal  title.object_id
      child[:details][:length].object_id.wont_equal length.object_id
    end
  end
end