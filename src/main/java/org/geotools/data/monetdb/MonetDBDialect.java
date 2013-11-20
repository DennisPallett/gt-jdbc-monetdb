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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.ColumnMetadata;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 *
 * @source $URL$
 */
public class MonetDBDialect extends BasicSQLDialect {

	//geometry type to class map
    final static Map<String, Class> TYPE_TO_CLASS_MAP = new HashMap<String, Class>() {
        {
            put("GEOMETRY", Geometry.class);
            put("GEOGRAPHY", Geometry.class);
            put("POINT", Point.class);
            put("POINTM", Point.class);
            put("LINESTRING", LineString.class);
            put("LINESTRINGM", LineString.class);
            put("POLYGON", Polygon.class);
            put("POLYGONM", Polygon.class);
            put("MULTIPOINT", MultiPoint.class);
            put("MULTIPOINTM", MultiPoint.class);
            put("MULTILINESTRING", MultiLineString.class);
            put("MULTILINESTRINGM", MultiLineString.class);
            put("MULTIPOLYGON", MultiPolygon.class);
            put("MULTIPOLYGONM", MultiPolygon.class);
            put("GEOMETRYCOLLECTION", GeometryCollection.class);
            put("GEOMETRYCOLLECTIONM", GeometryCollection.class);
            put("BYTEA", byte[].class);
        }
    };

    //geometry class to type map
    final static Map<Class, String> CLASS_TO_TYPE_MAP = new HashMap<Class, String>() {
        {
            put(Geometry.class, "GEOMETRY");
            put(Point.class, "POINT");
            put(LineString.class, "LINESTRING");
            put(Polygon.class, "POLYGON");
            put(MultiPoint.class, "MULTIPOINT");
            put(MultiLineString.class, "MULTILINESTRING");
            put(MultiPolygon.class, "MULTIPOLYGON");
            put(GeometryCollection.class, "GEOMETRYCOLLECTION");
            put(byte[].class, "BYTEA");
        }
    };
    
    static final Version V_1_5_0 = new Version("1.5.0");

    static final Version V_2_0_0 = new Version("2.0.0");

    static final Version PGSQL_V_9_0 = new Version("9.0");
    
    static final Version PGSQL_V_9_1 = new Version("9.1");
    
    public static String quoteValue (String value) {
    	if (value == null) value = "";
		return "'" + value.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'") + "'";
	}
	
	public static String quoteIdentifier (String ident) {
		// prepare identifier
		ident = prepareIdentifier(ident);
		
		// make sure identifier is actually quoted
		ident = "\"" + ident + "\"";
		
		return ident;
	}
	
	public static String prepareIdentifier (String ident) {
		// MonetDB only supports lowercase identifiers
		ident = ident.toLowerCase();
		
		// MonetDB doesn't support any special characters so replace with underscore
		ident = ident.replaceAll("[^a-zA-Z0-9]+","_");
		
		return ident;
	}

    public MonetDBDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    boolean looseBBOXEnabled = false;

    boolean estimatedExtentsEnabled = false;
    
    boolean functionEncodingEnabled = false;
    
    Version version, pgsqlVersion;

    @Override
    public void initializeConnection(Connection cx) throws SQLException {
        super.initializeConnection(cx);
    }

    @Override
    public boolean includeTable(String schemaName, String tableName,
            Connection cx) throws SQLException {
        if (tableName.equals("geometry_columns")) {
            return false;
        } else if (tableName.startsWith("spatial_ref_sys")) {
            return false;
        } else if (tableName.equals("geography_columns")) {
            return false;
        } else if (tableName.equals("raster_columns")) {
            return false;
        } else if (tableName.equals("raster_overviews")) {
            return false;
        }

        if (schemaName != null && schemaName.equals("topology")) {
            return false;
        }
        // others?
        return true;
    }

    //ThreadLocal<WKBAttributeIO> wkbReader = new ThreadLocal<WKBAttributeIO>();

    @Override
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor,
            ResultSet rs, String column, GeometryFactory factory, Connection cx)
            throws IOException, SQLException {
        //WKBAttributeIO reader = getWKBReader(factory);
        
       // return (Geometry) reader.read(rs, column);
    	return null;
    }
    
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor,
            ResultSet rs, int column, GeometryFactory factory, Connection cx)
            throws IOException, SQLException {
        //WKBAttributeIO reader = getWKBReader(factory);
        
        //return (Geometry) reader.read(rs, column);
    	return null;
    }

    /*private WKBAttributeIO getWKBReader(GeometryFactory factory) {
        /*WKBAttributeIO reader = wkbReader.get();
        if(reader == null) {
            reader = new WKBAttributeIO(factory);
            wkbReader.set(reader);
        }  else {
            reader.setGeometryFactory(factory);
        }
        return reader;
        
    	return null;
    }*/

    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, String prefix, int srid,
            StringBuffer sql) {
        encodeGeometryColumn(gatt, prefix, srid, null, sql);
    }

    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, String prefix, int srid, Hints hints, 
        StringBuffer sql) {
    
        boolean geography = "geography".equals(gatt.getUserData().get(
                JDBCDataStore.JDBC_NATIVE_TYPENAME));
    
        if (geography) {
            sql.append("encode(ST_AsBinary(");
            encodeColumnName(prefix, gatt.getLocalName(), sql);
            sql.append("),'base64')");
        }
        else {
            boolean force2D = hints != null && hints.containsKey(Hints.FEATURE_2D) && 
                Boolean.TRUE.equals(hints.get(Hints.FEATURE_2D));

            if (force2D) {
                sql.append("encode(ST_AsBinary(ST_Force_2D(");
                encodeColumnName(prefix, gatt.getLocalName(), sql);
                sql.append(")),'base64')");
            } else {
                sql.append("encode(ST_AsEWKB(");
                encodeColumnName(prefix, gatt.getLocalName(), sql);
                sql.append("),'base64')");
            }
        }
    }

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn,
            StringBuffer sql) {
        sql.append("ST_AsText(ST_Force_2D(ST_Envelope(");
        sql.append("ST_Extent(\"" + geometryColumn + "\"::geometry))))");
    }
    
    @Override
    public List<ReferencedEnvelope> getOptimizedBounds(String schema, SimpleFeatureType featureType,
            Connection cx) throws SQLException, IOException {
        if (!estimatedExtentsEnabled)
            return null;

        String tableName = featureType.getTypeName();

        Statement st = null;
        ResultSet rs = null;

        List<ReferencedEnvelope> result = new ArrayList<ReferencedEnvelope>();
        Savepoint savePoint = null;
        try {
            st = cx.createStatement();
            if(!cx.getAutoCommit()) {
                savePoint = cx.setSavepoint();
            }

            for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                if (att instanceof GeometryDescriptor) {
                    // use estimated extent (optimizer statistics)
                    StringBuffer sql = new StringBuffer();
                    sql.append("select ST_AsText(ST_force_2d(ST_Envelope(ST_Estimated_Extent('");
                    if(schema != null) {
                        sql.append(schema);
                        sql.append("', '");
                    }
                    sql.append(tableName);
                    sql.append("', '");
                    sql.append(att.getName().getLocalPart());
                    sql.append("'))))");
                    rs = st.executeQuery(sql.toString());

                    if (rs.next()) {
                        // decode the geometry
                        Envelope env = decodeGeometryEnvelope(rs, 1, cx);

                        // reproject and merge
                        if (!env.isNull()) {
                            CoordinateReferenceSystem crs = ((GeometryDescriptor) att)
                                    .getCoordinateReferenceSystem();
                            result.add(new ReferencedEnvelope(env, crs));
                        }
                    }
                    rs.close();
                }
            }
        } catch(SQLException e) {
            if(savePoint != null) {
                cx.rollback(savePoint);
            }
            LOGGER.log(Level.WARNING, "Failed to use ST_Estimated_Extent, falling back on envelope aggregation", e);
            return null;
        } finally {
            if(savePoint != null) {
                cx.releaseSavepoint(savePoint);
            }
            dataStore.closeSafe(rs);
            dataStore.closeSafe(st);
        } 
        return result;
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column,
            Connection cx) throws SQLException, IOException {
        try {
            String envelope = rs.getString(column);
            if (envelope != null)
                return new WKTReader().read(envelope).getEnvelopeInternal();
            else
                // empty one
                return new Envelope();
        } catch (ParseException e) {
            throw (IOException) new IOException(
                    "Error occurred parsing the bounds WKT").initCause(e);
        }
    }

    @Override
    public void handleUserDefinedType(ResultSet columnMetaData, ColumnMetadata metadata,
            Connection cx) throws SQLException {

        String tableName = columnMetaData.getString("TABLE_NAME");
        String columnName = columnMetaData.getString("COLUMN_NAME");
        String schemaName = columnMetaData.getString("TABLE_SCHEM");
        
        String sql = "SELECT udt_name FROM information_schema.columns " + 
        " WHERE table_schema = '"+schemaName+"' " + 
        "   AND table_name = '"+tableName+"' " + 
        "   AND column_name = '"+columnName+"' ";
        LOGGER.fine(sql);
        
        Statement st = cx.createStatement();
        try {
            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    metadata.setTypeName(rs.getString(1));
                }
            }
            finally {
                dataStore.closeSafe(rs);
            }
        }
        finally {
            dataStore.closeSafe(st);
        }
    }
    
    @Override
    public Integer getGeometrySRID(String schemaName, String tableName,
            String columnName, Connection cx) throws SQLException {
   	
        // first attempt, try with the geometry metadata
        Statement statement = null;
        ResultSet result = null;
        Integer srid = null;
        try {
            if (schemaName == null)
                schemaName = "sys";
          
            // try geometry_columns
            try {
                String sqlStatement = "SELECT SRID FROM GEOMETRY_COLUMNS WHERE " //
                        + "F_TABLE_SCHEMA = '" + schemaName + "' " //
                        + "AND F_TABLE_NAME = '" + tableName + "' " //
                        + "AND F_GEOMETRY_COLUMN = '" + columnName + "'";
    
                LOGGER.log(Level.FINE, "Geometry srid check; {0} ", sqlStatement);
                statement = cx.createStatement();
                result = statement.executeQuery(sqlStatement);
    
                if (result.next()) {
                    srid = result.getInt(1);
                }
            } catch(SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve information about " 
                        + schemaName + "." + tableName + "."  + columnName 
                        + " from the geometry_columns table, checking the first geometry instead", e);
            } finally {
                dataStore.closeSafe(result);
            }
            
            // fall back on inspection of the first geometry, assuming uniform srid (fair assumption
            // an unpredictable srid makes the table un-queriable)      
            if(srid == null) {
                String sqlStatement = "SELECT SRID(\"" + columnName + "\") " +
                               "FROM \"" + schemaName + "\".\"" + tableName + "\" " +
                               "WHERE \"" + columnName + "\" IS NOT NULL " +
                               "LIMIT 1";
                result = statement.executeQuery(sqlStatement);
                if (result.next()) {
                    srid = result.getInt(1);
                }
            }
        } finally {
            dataStore.closeSafe(result);
            dataStore.closeSafe(statement);
        }

        return srid;
    }

    @Override
    public String getSequenceForColumn(String schemaName, String tableName,
            String columnName, Connection cx) throws SQLException {
        Statement st = cx.createStatement();
        try {
        	String sql = "SELECT " + quoteIdentifier("default") + " FROM \"sys\".\"_columns\" AS columns" +	
        				" INNER JOIN sys._tables AS tables ON columns.table_id = tables.id";
        	
        	if (schemaName != null && schemaName.length() > 0) {
        		sql += " INNER JOIN sys.schemas AS schemas ON tables.schema_id = schemas.id" +
        				" WHERE schemas.name = " + quoteValue(schemaName);
        	} else {
        		sql += " WHERE 1=1";
        	}
        	
        	sql += " AND tables.name = " + quoteValue(tableName) + 
        		   " AND columns.name = " + quoteValue(columnName);
        	
            dataStore.getLogger().fine(sql);
            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                	String defaultValue = rs.getString(1);
                	
                	Pattern regex = Pattern.compile("\"seq_(.*?)\"");
                	Matcher m = regex.matcher(defaultValue);
                	
                	String seqName = null;
                	while(m.find()) {
                		seqName = "seq_" + m.group(1);
                	}

                	return seqName;
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }

        return null;
    }

    @Override
    public Object getNextSequenceValue(String schemaName, String sequenceName,
            Connection cx) throws SQLException {
        Statement st = cx.createStatement();
        try {
            String sql = "SELECT nextval('" + sequenceName + "')";

            dataStore.getLogger().fine(sql);
            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }

        return null;
    }

    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return true;
    }
    
    @Override
    public Object getLastAutoGeneratedValue(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        
        Statement st = cx.createStatement();
        try {
            String sql = "SELECT lastval()";
            dataStore.getLogger().fine( sql);
            
            ResultSet rs = st.executeQuery( sql);
            try {
                if ( rs.next() ) {
                    return rs.getLong(1);
                }
            } 
            finally {
                dataStore.closeSafe(rs);
            }
        }
        finally {
            dataStore.closeSafe(st);
        }

        return null;
    }
    
    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        super.registerClassToSqlMappings(mappings);

        // jdbc metadata for geom columns reports DATA_TYPE=1111=Types.OTHER
        mappings.put(Geometry.class, Types.OTHER);
        mappings.put(UUID.class, Types.OTHER);
    }

    @Override
    public void registerSqlTypeNameToClassMappings(
            Map<String, Class<?>> mappings) {
        super.registerSqlTypeNameToClassMappings(mappings);

        mappings.put("multipolygon", MultiPolygon.class);
        mappings.put("geometry", Geometry.class);
        mappings.put("geography", Geometry.class);
        mappings.put("text", String.class);
        mappings.put("int8", Long.class);
        mappings.put("int4", Integer.class);
        mappings.put("bool", Boolean.class);
        mappings.put("character", String.class);
        mappings.put("float8", Double.class);
        mappings.put("int", Integer.class);
        mappings.put("float4", Float.class);
        mappings.put("int2", Short.class);
        mappings.put("time", Time.class);
        mappings.put("timetz", Time.class);
        mappings.put("timestamp", Timestamp.class);
        mappings.put("timestamptz", Timestamp.class);
        mappings.put("uuid", UUID.class);
    }
    
    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(
            Map<Integer, String> overrides) {
        overrides.put(Types.VARCHAR, "VARCHAR");
        overrides.put(Types.BOOLEAN, "BOOL");
    }

    @Override
    public String getGeometryTypeName(Integer type) {
        return "geometry";
    }

    @Override
    public void encodePrimaryKey(String column, StringBuffer sql) {
        encodeColumnName(column, sql);
        sql.append(" SERIAL PRIMARY KEY");
    }

    /**
     * Creates GEOMETRY_COLUMN registrations and spatial indexes for all
     * geometry columns
     */
    @Override
    public void postCreateTable(String schemaName,
            SimpleFeatureType featureType, Connection cx) throws SQLException {
        schemaName = schemaName != null ? schemaName : "public"; 
        String tableName = featureType.getName().getLocalPart();
        
        Statement st = null;
        try {
            st = cx.createStatement();

            // register all geometry columns in the database
            for (AttributeDescriptor att : featureType
                    .getAttributeDescriptors()) {
                if (att instanceof GeometryDescriptor) {
                    GeometryDescriptor gd = (GeometryDescriptor) att;

                    // lookup or reverse engineer the srid
                    int srid = -1;
                    if (gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID) != null) {
                        srid = (Integer) gd.getUserData().get(
                                JDBCDataStore.JDBC_NATIVE_SRID);
                    } else if (gd.getCoordinateReferenceSystem() != null) {
                        try {
                            Integer result = CRS.lookupEpsgCode(gd
                                    .getCoordinateReferenceSystem(), true);
                            if (result != null)
                                srid = result;
                        } catch (Exception e) {
                            LOGGER.log(Level.FINE, "Error looking up the "
                                    + "epsg code for metadata "
                                    + "insertion, assuming -1", e);
                        }
                    }

                    // assume 2 dimensions, but ease future customisation
                    int dimensions = 2;

                    // grab the geometry type
                    String geomType = CLASS_TO_TYPE_MAP.get(gd.getType()
                            .getBinding());
                    if (geomType == null)
                        geomType = "GEOMETRY";

                    String sql = null;
                    if (getVersion(cx).compareTo(V_2_0_0) >= 0) {
                        // postgis 2 and up we don't muck with geometry_columns, we just alter the
                        // type directly to set the geometry type and srid
                        //setup the geometry type
                        sql = 
                            "ALTER TABLE \"" + schemaName + "\".\"" + tableName + "\" " + 
                             "ALTER COLUMN \"" + gd.getLocalName() + "\" " + 
                             "TYPE geometry (" + geomType + ", " + srid + ");";
                        
                        LOGGER.fine( sql );
                        st.execute( sql );
                    }
                    else {
                        // register the geometry type, first remove and eventual
                        // leftover, then write out the real one
                        sql = 
                        "DELETE FROM GEOMETRY_COLUMNS"
                                + " WHERE f_table_catalog=''" //
                                + " AND f_table_schema = '" + schemaName + "'" //
                                + " AND f_table_name = '" + tableName + "'" // 
                                + " AND f_geometry_column = '" + gd.getLocalName() + "'";
                        
                        LOGGER.fine( sql );
                        st.execute( sql );
                        
                        sql = "INSERT INTO GEOMETRY_COLUMNS VALUES (''," //
                                + "'" + schemaName + "'," //
                                + "'" + tableName + "'," //
                                + "'" + gd.getLocalName() + "'," //
                                + dimensions + "," //
                                + srid + "," //
                                + "'" + geomType + "')";
                        LOGGER.fine( sql );
                        st.execute( sql );
                    }

                    // add srid checks
                    if (srid > -1) {
                        sql = "ALTER TABLE " //
                                + "\"" + schemaName + "\"" // 
                                + "." //
                                + "\"" + tableName + "\"" //
                                + " ADD CONSTRAINT \"enforce_srid_" // 
                                + gd.getLocalName() + "\""// 
                                + " CHECK (ST_SRID(" //
                                + "\"" + gd.getLocalName() + "\"" //
                                + ") = " + srid + ")";
                        LOGGER.fine( sql );
                        st.execute(sql);
                    }

                    // add dimension checks
                    sql = "ALTER TABLE " //
                            + "\"" + schemaName + "\"" // 
                            + "." //
                            + "\"" + tableName + "\"" //
                            + " ADD CONSTRAINT \"enforce_dims_" // 
                            + gd.getLocalName() + "\""// 
                            + " CHECK (st_ndims(\"" + gd.getLocalName() + "\")" //
                            + " = 2)";
                    LOGGER.fine(sql);
                    st.execute(sql);

                    // add geometry type checks
                    if (!geomType.equals("GEOMETRY")) {
                        sql = "ALTER TABLE " //
                                + "\"" + schemaName + "\"" // 
                                + "." //
                                + "\"" + tableName + "\"" //
                                + " ADD CONSTRAINT \"enforce_geotype_" //
                                + gd.getLocalName() + "\""//
                                + " CHECK (geometrytype(" //
                                + "\"" + gd.getLocalName() + "\"" //
                                + ") = '" + geomType + "'::text " + "OR \""
                                + gd.getLocalName() + "\"" //
                                + " IS NULL)";
                        LOGGER.fine(sql);
                        st.execute(sql);
                    }
                    
                    // add the spatial index
                    sql = 
                    "CREATE INDEX \"spatial_" + tableName // 
                            + "_" + gd.getLocalName().toLowerCase() + "\""// 
                            + " ON " //
                            + "\"" + schemaName + "\"" // 
                            + "." //
                            + "\"" + tableName + "\"" //
                            + " USING GIST (" //
                            + "\"" + gd.getLocalName() + "\"" //
                            + ")";
                    LOGGER.fine(sql);
                    st.execute(sql);
                }
            }
            if (!cx.getAutoCommit()) {
                cx.commit();
            }
         } finally {
            dataStore.closeSafe(st);
        }
    }

    @Override
    public void postDropTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException {
        Statement st = cx.createStatement();
        String tableName = featureType.getTypeName();

        try {
            //remove all the geometry_column entries
            String sql = 
                "DELETE FROM GEOMETRY_COLUMNS"
                    + " WHERE f_table_catalog=''" //
                    + " AND f_table_schema = '" + schemaName + "'" 
                    + " AND f_table_name = '" + tableName + "'";
            LOGGER.fine( sql );
            st.execute( sql );
        }
        finally {
            dataStore.closeSafe(st);
        }
    }

    @Override
    public void encodeGeometryValue(Geometry value, int srid, StringBuffer sql)
            throws IOException {
    	if (value == null || value.isEmpty()) {
            sql.append("NULL");
        } else {
            if (value instanceof LinearRing) {
                //postgis does not handle linear rings, convert to just a line string
                value = value.getFactory().createLineString(((LinearRing) value).getCoordinateSequence());
            }
            
            sql.append("ST_GeomFromText('" + value.toText() + "', " + srid + ")");
        }
    }

    @Override
    public FilterToSQL createFilterToSQL() {
    	MonetDBFilterToSQL sql = new MonetDBFilterToSQL(this);
    	return sql;
    }
    
    @Override
    public boolean isLimitOffsetSupported() {
        return true;
    }
    
    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        if(limit >= 0 && limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT " + limit);
            if(offset > 0) {
                sql.append(" OFFSET " + offset);
            }
        } else if(offset > 0) {
            sql.append(" OFFSET " + offset);
        }
    }
    
    @Override
    public void encodeValue(Object value, Class type, StringBuffer sql) {
        if(byte[].class.equals(type)) {
            byte[] input = (byte[]) value;
            //check postgres version, if > 9 default encoding is hex
            if (pgsqlVersion.compareTo(PGSQL_V_9_1) >= 0) {
                encodeByteArrayAsHex(input, sql);
            }
            else {
                encodeByteArrayAsEscape(input, sql);
            }

        } else {
            super.encodeValue(value, type, sql);
        }
    }

    void encodeByteArrayAsHex(byte[] input, StringBuffer sql) {
        StringBuffer sb = new StringBuffer("\\x");
        for (int i = 0; i < input.length; i++) {
            sb.append(String.format("%02x", input[i]));
        }
        super.encodeValue(sb.toString(), String.class, sql);
    }

    void encodeByteArrayAsEscape(byte[] input, StringBuffer sql) {
        // escape the into bytea representation
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            byte b = input[i];
            if(b == 0) {
                sb.append("\\\\000");
            } else if(b == 39) {
                sb.append("\\'");
            } else if(b == 92) {
                sb.append("\\\\134'");
            } else if(b < 31 || b >= 127) {
                sb.append("\\\\");
                String octal = Integer.toOctalString(b);
                if(octal.length() == 1) {
                    sb.append("00");
                } else if(octal.length() == 2) {
                    sb.append("0");
                }
                sb.append(octal);
            } else {
                sb.append((char) b);
            }
        }
        super.encodeValue(sb.toString(), String.class, sql);
    }
    @Override
    public int getDefaultVarcharSize(){
        return -1;
    }

    /**
     * Returns the PostGIS version
     * @return
     */
    public Version getVersion(Connection conn) throws SQLException {
        if(version == null) {
            Statement st = null;
            ResultSet rs = null;
            try {
                st = conn.createStatement();
                rs = st.executeQuery("select PostGIS_Lib_Version()");
                if(rs.next()) {
                    version = new Version(rs.getString(1));
                } 
            } finally {
                dataStore.closeSafe(rs);
                dataStore.closeSafe(st);
            }
        }
        
        return version;
    }

    /**
     * Returns the PostgreSQL version
     */
    public Version getPostgreSQLVersion(Connection conn) throws SQLException {
        if (pgsqlVersion == null) {
            DatabaseMetaData md = conn.getMetaData();
            pgsqlVersion = new Version(
                String.format("%d.%d", md.getDatabaseMajorVersion(), md.getDatabaseMinorVersion()));
        }
        return pgsqlVersion;
    }

    
}
