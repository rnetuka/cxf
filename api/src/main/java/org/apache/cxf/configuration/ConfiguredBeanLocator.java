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

package org.apache.cxf.configuration;

import java.util.Collection;
import java.util.List;

/**
 * 
 */
public interface ConfiguredBeanLocator {
    
    List<String> getBeanNamesOfType(Class<?> type);
    
    <T> Collection<? extends T> getBeansOfType(Class<T> type);

    <T> T getBeanOfType(String name, Class<T> type);
    
    <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener);
    
    public interface BeanLoaderListener<T> {
        /**
         * Return true to have the loader go ahead and load the bean.  If false, 
         * the loader will just skip to the next bean
         * @param name
         * @param type
         * @return
         */
        boolean loadBean(String name, Class<? extends T> type);

        /**
         * Return true if the bean that was loaded meets the requirements at
         * which point, the loader will stop loading additional beans of the
         * given type
         * @param bean
         * @return
         */
        boolean beanLoaded(String name, T bean);
    }
}
