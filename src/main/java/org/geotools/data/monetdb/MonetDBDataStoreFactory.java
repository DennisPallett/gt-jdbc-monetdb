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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.jdbc.datasource.DBCPDataSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeatureType;


/**
 * DataStoreFacotry for MonetDB database
 *
 * @author Dennis Pallett
 *
 * @source $URL$
 */
public class MonetDBDataStoreFactory extends JDBCDataStoreFactory {
    /** parameter for database type */
    public static final Param DBTYPE = new Param("dbtype", String.class, "Type", true, "monetdb");
    
    /** optional user parameter */
    public static final Param USER = new Param(JDBCDataStoreFactory.USER.key, JDBCDataStoreFactory.USER.type, 
            JDBCDataStoreFactory.USER.description, false, "monetdb");

    /** optional host parameter */
    public static final Param HOST = new Param(JDBCDataStoreFactory.HOST.key, JDBCDataStoreFactory.HOST.type, 
            JDBCDataStoreFactory.HOST.description, false, JDBCDataStoreFactory.HOST.sample);

    /** optional port parameter */
    public static final Param PORT = new Param(JDBCDataStoreFactory.PORT.key, JDBCDataStoreFactory.PORT.type, 
            JDBCDataStoreFactory.PORT.description, false, 50000);
    
    /** parameter for database schema */
    public static final Param SCHEMA = new Param(JDBCDataStoreFactory.SCHEMA.key, String.class, "Schema", false, "sys");

   
    protected void setupParameters(Map parameters) {
    	// NOTE: when adding parameters here remember to add them to MonetDBJNDIDataStoreFactory
    	
        super.setupParameters(parameters);
        
        parameters.remove(JDBCDataStoreFactory.PORT.key);
        parameters.put(PORT.key, PORT);
        
        parameters.remove(JDBCDataStoreFactory.SCHEMA.key);
        parameters.put(SCHEMA.key, SCHEMA);
        
        parameters.remove(JDBCDataStoreFactory.FETCHSIZE.key);
    }

    public String getDisplayName() {
        return "MonetDB";
    }

    public String getDescription() {
        return "MonetDB/GeoSpatial database";
    }

    protected String getDatabaseID() {
        return (String) DBTYPE.sample;
    }

    protected String getDriverClassName() {
        return "nl.cwi.monetdb.jdbc.MonetDriver";
    }

    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
    	 return new MonetDBDialect(dataStore);
    }
    
    @Override
    protected JDBCDataStore createDataStoreInternal(JDBCDataStore dataStore, Map params)
            throws IOException {
    		// disable fetch size
    		// not fully supported by MonetDB 	
    		dataStore.setFetchSize(0);
    	
            return dataStore;
        }

     @Override
    protected String getValidationQuery() {
    	return "select now()";
    }
   
    @Override
    protected String getJDBCUrl(Map params) throws IOException {
        String host = (String) HOST.lookUp(params);
        String db = (String) DATABASE.lookUp(params);
        
        Object portObj = PORT.lookUp(params);
        if (portObj == null) {
        	portObj = PORT.sample;
        }
        
        int port = (Integer) portObj;
        return "jdbc:monetdb" + "://" + host + ":" + port + "/" + db;
    }
    
    public static void main (String[] args) throws Exception {
    	Map<String,Object> params = new HashMap<String,Object>();
        params.put( "dbtype", "monetdb");
        params.put( "host", "localhost");
        params.put( "database", "test");
        params.put( "user", "monetdb");
        params.put( "passwd", "monetdb");
        
        DataStore dataStore = (new MonetDBDataStoreFactory()).createDataStore( params );
        if (dataStore == null) {
        	throw new Exception("Unable to create datastore!");
        }        
        System.out.println("DataStore created!");
        
        FeatureSource fsBuildings = dataStore.getFeatureSource("nyc_buildings");
        
        //System.out.println("bc count: " + fsBC.getCount(Query.ALL));
        
        System.out.println(fsBuildings.getSchema().getGeometryDescriptor());
        
        Query query = Query.ALL;
        query.setMaxFeatures(10);
        FeatureCollection collection = fsBuildings.getFeatures(query);
        
        System.out.println(collection.getBounds());
        
        FeatureIterator iter = collection.features();
        int counter = 0;
        while(iter.hasNext()) {
        	Feature feature = iter.next();
        	
        	//System.out.println(feature);
        	
        	GeometryAttribute geom = feature.getDefaultGeometryProperty();
        	
        	counter++;
        	if (counter >= 10) break;
        }
        
        
        
        dataStore.dispose();
    }
}
