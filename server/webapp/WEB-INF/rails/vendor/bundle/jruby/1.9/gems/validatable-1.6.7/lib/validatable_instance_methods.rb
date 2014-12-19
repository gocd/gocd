module Validatable
  def self.included(klass) #:nodoc:
    klass.extend Validatable::ClassMethods
    klass.extend Validatable::Macros
  end
  
  # call-seq: valid?
  #
  # Returns true if no errors were added otherwise false. Only executes validations that have no :groups option specified
  def valid?
    valid_for_group?(nil)
  end
  
  # call-seq: errors
  #
  # Returns the Errors object that holds all information about attribute error messages.
  def errors
    @errors ||= Validatable::Errors.new
  end
  
  def valid_for_group?(group) #:nodoc:
    run_before_validations
    errors.clear
    self.class.validate_children(self, group)
    self.validate(group)
    errors.empty?
  end
  
  def times_validated(key) #:nodoc:
    times_validated_hash[key] || 0
  end
  
  def increment_times_validated_for(validation) #:nodoc:
    if validation.key != nil
      if times_validated_hash[validation.key].nil?
        times_validated_hash[validation.key] = 1
      else
        times_validated_hash[validation.key] += 1
      end
    end
  end

  # call-seq: validate_only(key)
  #
  # Only executes a specified validation. The argument should follow a pattern based on the key of the validation.
  #   Examples:
  #     * validates_presence_of :name can be run with obj.validate_only("presence_of/name")
  #     * validates_presence_of :birthday, :key => "a key" can be run with obj.validate_only("presence_of/a key")
  def validate_only(key)
    validation_name, attribute_name = key.split("/")
    validation_name = validation_name.split("_").collect{|word| word.capitalize}.join
    validation_key = "#{self.class.name}/Validatable::Validates#{validation_name}/#{attribute_name}"
    validation = self.class.all_validations.find { |validation| validation.key == validation_key }
    raise ArgumentError.new("validation with key #{validation_key} could not be found") if validation.nil?
    errors.clear
    run_validation(validation)
  end

  protected
  def times_validated_hash #:nodoc:
    @times_validated_hash ||= {}
  end

  def validate(group) #:nodoc:
    validation_levels.each do |level|
      validations_for_level_and_group(level, group).each do |validation|
        run_validation(validation) if validation.should_validate?(self)
      end
      return unless self.errors.empty?
    end
  end

  def run_validation(validation) #:nodoc:
    validation_result = validation.valid?(self)
    add_error(validation.attribute, validation.message(self)) unless validation_result
    increment_times_validated_for(validation)
    validation.run_after_validate(validation_result, self, validation.attribute)
  end
  
  def run_before_validations #:nodoc:
    self.class.all_before_validations.each do |block|
      instance_eval &block
    end
  end
  
  def add_error(attribute, message) #:nodoc:
    self.class.add_error(self, attribute, message)
  end
  
  def validations_for_level_and_group(level, group) #:nodoc:
    validations_for_level = self.all_validations.select { |validation| validation.level == level }
    return validations_for_level.select { |validation| validation.groups.empty? } if group.nil?
    validations_for_level.select { |validation| validation.groups.include?(group) }
  end
  
  def all_validations #:nodoc:
    res = self.class.validations_to_include.inject(self.class.all_validations) do |result, included_validation_class|
      result += self.send(included_validation_class.attribute).all_validations
      result
    end
  end
  
  def validation_levels #:nodoc:
    self.class.all_validations.inject([1]) { |result, validation| result << validation.level }.uniq.sort
  end
end