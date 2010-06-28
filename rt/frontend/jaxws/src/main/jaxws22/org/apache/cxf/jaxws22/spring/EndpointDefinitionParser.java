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
package org.apache.cxf.jaxws22.spring;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class EndpointDefinitionParser extends org.apache.cxf.jaxws.spring.EndpointDefinitionParser {
    public EndpointDefinitionParser() {
        setBeanClass(JAXWS22SpringEndpointImpl.class);
    }
    
    @NoJSR250Annotations
    public static class JAXWS22SpringEndpointImpl extends org.apache.cxf.jaxws22.EndpointImpl
        implements ApplicationContextAware {
    
        public JAXWS22SpringEndpointImpl(Object implementor) {
            super((Bus)null, implementor);
        }
        public JAXWS22SpringEndpointImpl(Bus bus, Object implementor) {
            super(bus, implementor);
        }
        
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (getBus() == null) {
                Bus bus = BusFactory.getThreadDefaultBus();
                BusWiringBeanFactoryPostProcessor.updateBusReferencesInContext(bus, ctx);
                setBus(bus);
            }
        }
    }

}
