/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.monetdb;

import java.util.Collections;
import java.util.HashMap;

import junit.framework.TestCase;

import org.geotools.data.DataStore;
import org.geotools.data.jdbc.datasource.ManageableDataSource;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.h2.tools.Server;


/**
 * 
 *
 * @source $URL$
 */
public class MonetDBDataStoreFactoryTest extends TestCase {
    MonetDBDataStoreFactory factory;
    HashMap params;
    
    protected void setUp() throws Exception {
        factory = new MonetDBDataStoreFactory();
        
        params = new HashMap();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "monetdb");
        params.put(MonetDBDataStoreFactory.HOST.key, "localhost");
        params.put(MonetDBDataStoreFactory.DATABASE.key, "test");
        params.put(MonetDBDataStoreFactory.USER.key, "monetdb");
        params.put(MonetDBDataStoreFactory.PASSWD.key, "monetdb");
    }

    public void testCanProcess() throws Exception {
        assertFalse(factory.canProcess(Collections.EMPTY_MAP));
        assertTrue(factory.canProcess(params));
    }
    
    public void testCreateDataStore() throws Exception {
        JDBCDataStore ds = factory.createDataStore( params );
        assertNotNull( ds );
        assertTrue(ds.getDataSource() instanceof ManageableDataSource);
        ds.dispose();
    }
    
    public void testTCP() throws Exception {
        params.put(MonetDBDataStoreFactory.PASSWD.key, "monetdbFAIL");
        
        DataStore ds = factory.createDataStore(params);
        try {
            ds.getTypeNames();
            fail("Should not have made a connection.");
        }
        catch(Exception ok) {}
        
        params.put(MonetDBDataStoreFactory.PASSWD.key, "monetdb");
        ds = factory.createDataStore(params);
        try {
            ds.getTypeNames();
        }
        catch(Exception fail) {
        	fail("Should have made a connection.");
        }
                
        ds.dispose();
    }
}
