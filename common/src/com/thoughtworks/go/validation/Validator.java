/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.validation;

import com.thoughtworks.go.domain.materials.ValidationBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import java.util.HashMap;
import java.util.Map;

public abstract class Validator<T> {
    public static final Validator<String> HOSTNAME = new HostnameValidator();
    public static final Validator<String> PIPELINEGROUP = new PipelineGroupValidator();
    public static final Validator<String> EMAIL = new EmailValidator();
    public static final Validator<String> PORT = new PortValidator();

    protected final String errorMessage;

    public Validator(String errorMessageKey) {
        this.errorMessage = errorMessageKey;
    }

    public abstract ValidationBean validate(T value);

    public ModelAndView validateForJson(T value, View jsonView) {
        Map map = new HashMap();
        map.put("json", validate(value));
        return new ModelAndView(jsonView, map);
    }

    public void assertValid(T value) {
        ValidationBean validationBean = this.validate(value);
        if (!validationBean.isValid()) {
            throw new RuntimeException(errorMessage);
        }
    }

    public static LengthValidator lengthValidator(int length) {
        return new LengthValidator(length);
    }

    public static Validator presenceValidator(String errorMessage) {
        return new PresenceValidator(errorMessage);
    }

}
