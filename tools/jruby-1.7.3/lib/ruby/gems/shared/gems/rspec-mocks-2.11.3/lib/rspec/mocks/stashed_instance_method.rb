# @private
class StashedInstanceMethod
  def initialize(klass, method)
    @klass = klass
    @method = method

    @method_is_stashed = false
  end

  # @private
  def stash
    return if !method_defined_directly_on_klass? || @method_is_stashed

    @klass.__send__(:alias_method, stashed_method_name, @method)
    @method_is_stashed = true
  end

  private

  # @private
  def method_defined_directly_on_klass?
    method_defined_on_klass? && method_owned_by_klass?
  end

  # @private
  def method_defined_on_klass?
    @klass.method_defined?(@method) || @klass.private_method_defined?(@method)
  end

  if ::UnboundMethod.method_defined?(:owner)
    # @private
    def method_owned_by_klass?
      @klass.instance_method(@method).owner == @klass
    end
  else
    # @private
    def method_owned_by_klass?
      # On 1.8.6, which does not support Method#owner, we have no choice but
      # to assume it's defined on the klass even if it may be defined on
      # a superclass.
      true
    end
  end

  # @private
  def stashed_method_name
    "obfuscated_by_rspec_mocks__#{@method}"
  end

  public

  # @private
  def restore
    return unless @method_is_stashed

    @klass.__send__(:alias_method, @method, stashed_method_name)
    @klass.__send__(:remove_method, stashed_method_name)
    @method_is_stashed = false
  end
end
