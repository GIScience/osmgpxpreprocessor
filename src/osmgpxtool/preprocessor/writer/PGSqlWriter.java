package osmgpxtool.preprocessor.writer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import osmgpxtool.preprocessor.gps.GpsTracePart;

import com.vividsolutions.jts.io.WKBWriter;

/**
 * Database writer for MapMatching results.
 *
 */
public class PGSqlWriter {

	private Connection con;
	private Properties p;
	private PreparedStatement insert;

	private WKBWriter wkbWriter;
	int batchSize = 0;


	public PGSqlWriter(Connection con, Properties props) {
		this.con = con;
		this.p = props;
	}

	public void init() {

		Statement create = null;

		try {

			// for creation of databases
			String tableName = p.getProperty("t_gpxrawName") + "_" + p.getProperty("dbPreProOutputSuffix");
			create = con.createStatement();
			create.addBatch("DROP TABLE IF EXISTS " + tableName + ";");
			create.addBatch("CREATE TABLE "
					+ tableName
					+ " (\"gpx_id\" integer NOT NULL,\"trk_id\" integer NOT NULL, \"part_id\" integer NOT NULL, \"geom\" geometry NOT NULL, \"geom_smoothed\" geometry , CONSTRAINT \""
					+ tableName + "_PK\" PRIMARY KEY (gpx_id, trk_id,part_id));");
			
			
			create.addBatch("CREATE INDEX "+tableName+"_geomsmoothed_index ON "+tableName+" USING gist (geom_smoothed);");
			create.addBatch("CREATE INDEX "+tableName+"_geom_index ON "+tableName+" USING gist (geom);");

			
			
			create.executeBatch();

			insert = con.prepareStatement("INSERT INTO " + tableName
					+ " (\"gpx_id\",\"trk_id\", \"part_id\", \"geom\", \"geom_smoothed\") VALUES(?,?,?,ST_GeomFromEWKB(?),ST_GeomFromEWKB(?));");

		} catch (SQLException e) {
			e.printStackTrace();
			SQLException e2 = e.getNextException();
			e2.printStackTrace();
			System.exit(1);
		}
		wkbWriter = new WKBWriter(3, true);
	}

	public void write(List<GpsTracePart> tracepartList) {
		for (GpsTracePart t : tracepartList) {
			try {
				insert.setInt(1, t.getId());
				insert.setInt(2, t.getTrkId());
				insert.setInt(3, t.getPartId());
				insert.setBytes(4, wkbWriter.write(t.getGeom()));
				if (t.getGeomSmoothed()!=null){
					insert.setBytes(5, wkbWriter.write(t.getGeomSmoothed()));
				}else{
					insert.setNull(5, java.sql.Types.BINARY);
				}
				insert.addBatch();
				batchSize++;
				
				if (batchSize == 10000) {
					batchSize = 0;
					insert.executeBatch();
					insert.clearBatch();
	
				}
			} catch (SQLException e) {
				e.printStackTrace();
				e.getNextException().printStackTrace();
				System.exit(1);
			}
		}

	}

	public void close() {

		// close insert statement
		try {
			if (insert != null) {
				insert.executeBatch();
				insert.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	
	}

}
