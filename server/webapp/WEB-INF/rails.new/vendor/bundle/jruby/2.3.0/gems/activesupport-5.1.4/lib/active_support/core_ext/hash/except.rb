class Hash
  # Returns a hash that includes everything except given keys.
  #   hash = { a: true, b: false, c: nil }
  #   hash.except(:c)     # => { a: true, b: false }
  #   hash.except(:a, :b) # => { c: nil }
  #   hash                # => { a: true, b: false, c: nil }
  #
  # This is useful for limiting a set of parameters to everything but a few known toggles:
  #   @person.update(params[:person].except(:admin))
  def except(*keys)
    dup.except!(*keys)
  end

  # Removes the given keys from hash and returns it.
  #   hash = { a: true, b: false, c: nil }
  #   hash.except!(:c) # => { a: true, b: false }
  #   hash             # => { a: true, b: false }
  def except!(*keys)
    keys.each { |key| delete(key) }
    self
  end
end
