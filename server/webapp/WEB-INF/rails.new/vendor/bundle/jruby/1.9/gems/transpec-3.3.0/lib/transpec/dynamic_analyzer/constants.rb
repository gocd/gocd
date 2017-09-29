# coding: utf-8

module Transpec
  class DynamicAnalyzer
    ANALYSIS_MODULE = 'Transpec'
    ANALYSIS_METHOD = 'analyze'
    RUNTIME_DATA_ERROR_MESSAGE_KEY = :error_message
    HELPER_TEMPLATE_FILE = 'transpec_analysis_helper.rb.erb'
    RESULT_FILE = 'transpec_analysis_result.json'
  end
end
