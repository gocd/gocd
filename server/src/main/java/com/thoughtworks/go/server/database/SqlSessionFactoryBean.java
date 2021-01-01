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
package com.thoughtworks.go.server.database;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;

@Component
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {
    private final DataSource dataSource;
    private final Resource configLocation;
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    public SqlSessionFactoryBean(DataSource dataSource, @Value("classpath:/sql-map-config.xml") Resource configLocation) {
        this.dataSource = dataSource instanceof TransactionAwareDataSourceProxy ? dataSource : new TransactionAwareDataSourceProxy(dataSource);
        this.configLocation = configLocation;
    }

    @Override
    public SqlSessionFactory getObject() throws Exception {
        if (this.sqlSessionFactory == null) {
            afterPropertiesSet();
        }

        return this.sqlSessionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return SqlSessionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.sqlSessionFactory = buildSqlSessionFactory();
    }

    private SqlSessionFactory buildSqlSessionFactory() throws IOException {
        SqlSessionFactoryBuilder factoryBuilder = new SqlSessionFactoryBuilder();

        XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(configLocation.getInputStream());
        Configuration configuration = xmlConfigBuilder.getConfiguration();
        configuration.setEnvironment(new Environment(getClass().getSimpleName(), new SpringManagedTransactionFactory(), this.dataSource));
        xmlConfigBuilder.parse();
        return factoryBuilder.build(configuration);
    }

}
