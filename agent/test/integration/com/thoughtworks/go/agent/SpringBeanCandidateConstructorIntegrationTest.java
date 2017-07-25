/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerWebSocketClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:applicationContext.xml",
})
public class SpringBeanCandidateConstructorIntegrationTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void shouldEnsureThatNoBeanHasMoreThanOneCandidateConstructors() throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        String[] allBeans = applicationContext.getBeanDefinitionNames();
        ConfigurableListableBeanFactory beanFactory = ((AbstractApplicationContext) applicationContext).getBeanFactory();
        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();

        //Beans which are instantiated by explicitly specifying a constructor and its args in the applicationContext xml files
        List<Class<?>> exclusions = Arrays.asList(GoAgentServerHttpClient.class, GoAgentServerHttpClientBuilder.class,
                AgentHTTPClientController.class);
        for (String name : allBeans) {
            Object bean = beanFactory.getSingleton(name);
            if (bean != null) {
                if (bean.getClass().getCanonicalName().startsWith("com.thoughtworks.go")) {
                    Constructor<?>[] candidateConstructors = autowiredAnnotationBeanPostProcessor.determineCandidateConstructors(bean.getClass(), name);
                    if (candidateConstructors == null) {
                        Constructor[] declaredConstructors = bean.getClass().getDeclaredConstructors();
                        Constructor<?> defaultConstructor = null;
                        for (Constructor<?> declaredConstructor : declaredConstructors) {
                            if (declaredConstructor.getParameterCount() == 0) {
                                defaultConstructor = declaredConstructor;
                                break;
                            }
                        }
                        if (!exclusions.contains(bean.getClass())) {
                            assertThat(String.format("Bean doesn't have a default or autowired constructor => name: %s, class: %s", name, bean.getClass()), defaultConstructor, is(notNullValue()));
                            try {
                                BeanUtils.instantiateClass(defaultConstructor);
                            } catch (Exception e) {
                                fail(String.format("Default constructor failed : %s. Stacktrace: %s", e.getMessage(), e.getStackTrace()));
                            }
                        }

                    } else {
                        assertThat(String.format("name: %s class: %s", name, bean.getClass()), candidateConstructors.length, is(1));
                    }
                }
            }
        }
    }
}
