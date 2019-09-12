/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import org.hibernate.HibernateException;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class GoVersionConverter implements UserType {
    @Override
    public int[] sqlTypes() {
        return new int[]{StringType.INSTANCE.sqlType()};
    }

    @Override
    public Class returnedClass() {
        return GoVersion.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
        String columnName = names[0];
        String columnValue = (String) rs.getObject(columnName);
        return columnValue == null ? null : new GoVersion(columnValue);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, value.toString());
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value instanceof GoVersion) {
            return new GoVersion(value.toString());
        } else if (value == null) {
            return null;
        } else {
            throw new UnsupportedOperationException("Unable to deep copy an instance of " + value.getClass());
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }


    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        Object o = deepCopy(value);
        if (o instanceof Serializable) {
            return (Serializable) o;
        } else {
            return null;
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
}
