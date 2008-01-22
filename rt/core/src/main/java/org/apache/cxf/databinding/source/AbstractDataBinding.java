/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.databinding.source;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;

public abstract class AbstractDataBinding implements DataBinding {
    private Collection<DOMSource> schemas;
    private Map<String, String> namespaceMap;
    private int mtomThreshold;

    public Collection<DOMSource> getSchemas() {
        return schemas;
    }

    public void setSchemas(Collection<DOMSource> schemas) {
        this.schemas = schemas;
    }

    public XmlSchema addSchemaDocument(ServiceInfo serviceInfo, SchemaCollection col, Document d,
                                          String systemId) {
        String ns = d.getDocumentElement().getAttribute("targetNamespace");
        if (StringUtils.isEmpty(ns)) {
            ns = serviceInfo.getInterface().getName().getNamespaceURI();
            d.getDocumentElement().setAttribute("targetNamespace", ns);
        }

        NodeList nodes = d.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element) {
                Element e = (Element)n;
                if (e.getLocalName().equals("import")) {
                    e.removeAttribute("schemaLocation");
                }
            }
        }
        SchemaInfo schema = new SchemaInfo(serviceInfo, ns);
        schema.setSystemId(systemId);
        XmlSchema xmlSchema = col.read(d, null);
        schema.setSchema(xmlSchema);
        serviceInfo.addSchema(schema);
        return xmlSchema;
    }
    
    /**
      * @return Returns the namespaceMap.
     */
    public Map<String, String> getNamespaceMap() {
        return namespaceMap;
    }

    /**
     * @param namespaceMap The namespaceMap to set.
     */
    public void setNamespaceMap(Map<String, String> namespaceMap) {
        // make some checks. This is a map from namespace to prefix, but we want unique prefixes.
        if (namespaceMap != null) {
            Set<String> prefixesSoFar = new HashSet<String>();
            for (Map.Entry<String, String> mapping : namespaceMap.entrySet()) {
                if (prefixesSoFar.contains(mapping.getValue())) {
                    throw new IllegalArgumentException("Duplicate prefix " + mapping.getValue());
                }
            }
        }
        this.namespaceMap = namespaceMap;
    }

    /** 
     * Provide explicit mappings to ReflectionServiceFactory.
     * {@inheritDoc}
     * */
    public Map<String, String> getDeclaredNamespaceMappings() {
        return this.namespaceMap;
    }

    protected static void checkNamespaceMap(Map<String, String> namespaceMap) {
        // make some checks. This is a map from namespace to prefix, but we want unique prefixes.
        if (namespaceMap != null) {
            Set<String> prefixesSoFar = new HashSet<String>();
            for (Map.Entry<String, String> mapping : namespaceMap.entrySet()) {
                if (prefixesSoFar.contains(mapping.getValue())) {
                    throw new IllegalArgumentException("Duplicate prefix " + mapping.getValue());
                }
            }
        }
    }

    public int getMtomThreshold() {
        return mtomThreshold;
    }

    public void setMtomThreshold(int threshold) {
        mtomThreshold = threshold;
    }
}
