require 'test_helper'

# tests defining representers in modules, decorators and classes and the inheritance when combined.

class ConfigInheritTest < MiniTest::Spec
  def assert_cloned(child, parent, property)
    child_def  = child.representable_attrs.get(property)
    parent_def = parent.representable_attrs.get(property)

    child_def.merge!(:alias => property)

    child_def[:alias].wont_equal parent_def[:alias]
    child_def.object_id.wont_equal parent_def.object_id
  end
  # class Object

  # end
  module GenreModule
    include Representable
    property :genre
  end


  # in Decorator ------------------------------------------------
  class Decorator < Representable::Decorator
    property :title
  end

  it { Decorator.representable_attrs[:definitions].keys.must_equal ["title"] }

  # in inheriting Decorator

  class InheritingDecorator < Decorator
    property :location
  end

  it { InheritingDecorator.representable_attrs[:definitions].keys.must_equal ["title", "location"] }
  it { assert_cloned(InheritingDecorator, Decorator, "title") }

  # in inheriting and including Decorator

  class InheritingAndIncludingDecorator < Decorator
    include GenreModule
    property :location
  end

  it { InheritingAndIncludingDecorator.representable_attrs[:definitions].keys.must_equal ["title", "genre", "location"] }
  it { assert_cloned(InheritingAndIncludingDecorator, GenreModule, :genre) }


  # in module ---------------------------------------------------
  module Module
    include Representable
    property :title
  end

  it { Module.representable_attrs[:definitions].keys.must_equal ["title"] }


  # in module including module
  module SubModule
    include Representable
    include Module

    property :location
  end

  it { SubModule.representable_attrs[:definitions].keys.must_equal ["title", "location"] }
  it { assert_cloned(SubModule, Module, :title) }

  # including preserves order
  module IncludingModule
    include Representable
    property :genre
    include Module

    property :location
  end

  it { IncludingModule.representable_attrs[:definitions].keys.must_equal ["genre", "title", "location"] }


  # included in class -------------------------------------------
  class Class
    include Representable
    include IncludingModule
  end

  it { Class.representable_attrs[:definitions].keys.must_equal ["genre", "title", "location"] }
  it { assert_cloned(Class, IncludingModule, :title) }
  it { assert_cloned(Class, IncludingModule, :location) }
  it { assert_cloned(Class, IncludingModule, :genre) }

  # included in class with order
  class DefiningClass
    include Representable
    property :street_cred
    include IncludingModule
  end

  it { DefiningClass.representable_attrs[:definitions].keys.must_equal ["street_cred", "genre", "title", "location"] }

  # in class
  class RepresenterClass
    include Representable
    property :title
  end

  it { RepresenterClass.representable_attrs[:definitions].keys.must_equal ["title"] }


  # in inheriting class
  class InheritingClass < RepresenterClass
    include Representable
    property :location
  end

  it { InheritingClass.representable_attrs[:definitions].keys.must_equal ["title", "location"] }
  it { assert_cloned(InheritingClass, RepresenterClass, :title) }

  # in inheriting class and including
  class InheritingAndIncludingClass < RepresenterClass
    property :location
    include GenreModule
  end

  it { InheritingAndIncludingClass.representable_attrs[:definitions].keys.must_equal ["title", "location", "genre"] }
end