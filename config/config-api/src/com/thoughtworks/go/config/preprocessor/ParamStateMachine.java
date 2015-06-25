/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.preprocessor;

public class ParamStateMachine {

    public ParamStateMachine() {
    }

    public String process(String preResolved, ParamHandler paramsHandler) {
        ReaderState state = ReaderState.NOT_IN_PATTERN;
        for (int i = 0; i < preResolved.length(); i++) {
            state = state.interpret(preResolved.charAt(i), paramsHandler);
        }
        paramsHandler.handleAfterResolution(state);
        return paramsHandler.getResult();
    }

    public enum ReaderState {
        IN_PATTERN {
            ReaderState interpret(char ch, ParamHandler paramsHandler) {
                if (ch == CHAR_CURL_CLOSE) {
                    try {
                        paramsHandler.handlePatternFound(pattern);
                    } finally {
                        pattern.setLength(0);
                    }
                    return NOT_IN_PATTERN;
                } else {
                    pattern.append(ch);
                    return IN_PATTERN;
                }
            }
        },

        NOT_IN_PATTERN {
            ReaderState interpret(char ch, ParamHandler paramsHandler) {
                if (ch == CHAR_HASH) {
                    return HASH_SEEN;
                }
                paramsHandler.handleNotInPattern(ch);
                return NOT_IN_PATTERN;
            }
        },

        HASH_SEEN {
            ReaderState interpret(char ch, ParamHandler paramsHandler) {
                if (ch == CHAR_HASH) {
                    paramsHandler.handlePatternStarted(ch);
                    return NOT_IN_PATTERN;
                } else if (ch == CHAR_CURL_OPEN) {
                    return IN_PATTERN;
                }
                return INVALID_PATTERN;
            }
        },
        INVALID_PATTERN {
            ReaderState interpret(char ch, ParamHandler paramsHandler) {
                return INVALID_PATTERN;
            }
        };

        private static final char CHAR_CURL_CLOSE = '}';
        private static final char CHAR_CURL_OPEN = '{';
        private static final char CHAR_HASH = '#';

        protected StringBuilder pattern = new StringBuilder();

        abstract ReaderState interpret(char ch, ParamHandler paramsHandler);

    }

}
