# Active Record Deprecated Finders

This gem is a dependency of Rails 4.0 to provide deprecated finder
functionality.

It will be removed as a dependency in Rails 4.1, but users can manually include
it in their Gemfile and it will continue to be maintained until Rails 5.

```ruby
gem 'activerecord-deprecated_finders', require: 'active_record/deprecated_finders'
```

This gem is used to extract and deprecate old-style finder option hashes in
Active Record:

```ruby
Post.find(:all, conditions: { published_on: 2.weeks.ago }, limit: 5)
```

as well as the following dynamic finders:

* `find_all_by_...`
* `find_last_by_...`
* `scoped_by_...`
* `find_or_initialize_by_...`
* `find_or_create_by_...`

Note that `find(primary_key)`, `find_by...`, and `find_by...!` are not
deprecated.

To avoid reliance on this gem, you'll need to migrate your finder usage.

To migrate dynamic finders to Rails 4.1+:

* `find_all_by_...` should become `where(...)`.
* `find_last_by_...` should become `where(...).last`.
* `scoped_by_...` should become `where(...)`.
* `find_or_initialize_by_...` should become `find_or_initialize_by(...)`.
* `find_or_create_by_...` should become `find_or_create_by(...)`.

To migrate old-style finder option hashes and for additional information, 
please refer to:

* [ActiveRecord::FinderMethods][findermethods], 
  [ActiveRecord::Relation][relation], and 
  [ActiveRecord::QueryMethods][querymethods] docs.
* Rails Guide: Upgrading Ruby on Rails ([stable][stableguide] /
  [edge][edgeguide]).

[findermethods]: 
http://api.rubyonrails.org/classes/ActiveRecord/FinderMethods.html
[relation]: 
http://api.rubyonrails.org/classes/ActiveRecord/Relation.html
[querymethods]: 
http://api.rubyonrails.org/classes/ActiveRecord/QueryMethods.html
[stableguide]: 
http://guides.rubyonrails.org/upgrading_ruby_on_rails.html
[edgeguide]: 
http://edgeguides.rubyonrails.org/upgrading_ruby_on_rails.html
