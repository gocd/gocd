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

package com.thoughtworks.studios.shine.semweb.sesame;

import com.thoughtworks.studios.shine.semweb.URIReference;
import org.openrdf.model.URI;

public class SesameURIReference extends AbstractSesameResource implements URIReference {
    SesameURIReference(URI sesameNativeURI) {
        super(sesameNativeURI);
    }

    public String getURIText() {
        return getSesameNativeResource().stringValue();
    }

    public String getSPARQLForm() {
        return "<" + getURIText() + ">";
    }

    public String toString() {
        return "[" + this.getClass().getSimpleName() + "(" + getSPARQLForm() + ")]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof SesameURIReference) {
            return getURIText().equals(((SesameURIReference) o).getURIText());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getURIText().hashCode();
    }
}
