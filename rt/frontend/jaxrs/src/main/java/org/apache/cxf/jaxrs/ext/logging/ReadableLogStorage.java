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
package org.apache.cxf.jaxrs.ext.logging;

import java.util.List;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;

/**
 * Can be used by pull-style loggers to read the records from some external storage 
 *
 */
public interface ReadableLogStorage {

    
    /**
     * Read the records and load them into a provided list
     * @param list the list saved records should be added to
     * @param condition the condition loaded records must meet, can be null 
     * @param loadFrom the initial index of the storage to have records loaded from
     * @param int maxNumberOfRecords the max number of records to load from the storage
     */
    void load(List<LogRecord> list, 
              SearchCondition<LogRecord> condition,
              int loadFrom, 
              int maxNumberOfRecords);
    
    
    /**
     * Get the size of storage (in records)
     * @param the size, -1 if not known, for ex, when reading from an open file containing log entries
     */
    int getSize();
    
    
    /**
     * Close the storage
     */
    void close();
}
