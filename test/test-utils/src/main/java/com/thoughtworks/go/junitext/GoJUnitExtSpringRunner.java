/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.junitext;

import com.googlecode.junit.ext.JunitExtSpringRunner;
import org.junit.internal.runners.InitializationError;
import org.junit.runners.model.FrameworkMethod;

/*
    Workaround as spring upgrade to 3.1.3 has broken junit-ext
    - Sachin Sudheendra
 */
public class GoJUnitExtSpringRunner extends JunitExtSpringRunner {

    public GoJUnitExtSpringRunner(Class<?> aClass) throws InitializationError {
        super(aClass);
    }

    @Override
    protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
        if (!isPrereuisitSatisfied(frameworkMethod.getMethod())) {
            return true;
        }
        return super.isTestMethodIgnored(frameworkMethod);
    }
}
