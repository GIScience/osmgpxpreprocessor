package osmgpxtool.osmgpxpreprocessor.writer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import osmgpxtool.osmgpxpreprocessor.gps.GpsTracePart;

import com.vividsolutions.jts.io.WKBWriter;

/**
 * Database writer for MapMatching results.
 *
 */
public class PGSqlWriter {

	private Connection con;
	private Properties p;
	private PreparedStatement insert;
	private PreparedStatement insert_profiles;

	private WKBWriter wkbWriter;
	int batchSize = 0;
	private int lastStreetWritten = 0;

	public PGSqlWriter(Connection con, Properties props) {
		this.con = con;
		this.p = props;
	}

	public void init() {

		Statement create = null;

		try {

			// for creation of databases
			String tableName = p.getProperty("t_gpxName") + "_" + p.getProperty("dbOutputSuffix");
			create = con.createStatement();
			create.addBatch("DROP TABLE IF EXISTS " + tableName + ";");
			create.addBatch("CREATE TABLE "
					+ tableName
					+ " (\"gpx_id\" integer NOT NULL, \"part_id\" integer NOT NULL, \"geom\" geometry NOT NULL, CONSTRAINT \""
					+ tableName + "_PK\" PRIMARY KEY (gpx_id, part_id));");
			create.executeBatch();

			insert = con.prepareStatement("INSERT INTO " + tableName
					+ " (\"gpx_id\",\"part_id\", \"geom\") VALUES(?,?,ST_GeomFromEWKB(?));");

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
				insert.setInt(2, t.getPartId());
				insert.setBytes(3, wkbWriter.write(t.getGeom()));
				insert.addBatch();
				batchSize++;
				// TODO: uncomment if you want to write the profile line to
				// database
				/*
				 * if (lastStreetWritten != street.getId()) { GeometryFactory
				 * geomF = new GeometryFactory(); MultiLineString profiles =
				 * geomF.createMultiLineString(street.getProfiles().toArray(new
				 * LineString[0])); insert_profiles.setInt(1, street.getId());
				 * insert_profiles.setBytes(2, wkbWriter.write(profiles));
				 * insert_profiles.addBatch(); }
				 * 
				 * lastStreetWritten = street.getId();
				 */
				if (batchSize == 10000) {
					batchSize = 0;
					insert.executeBatch();
					insert.clearBatch();
					// TODO: uncomment if you want to write the profile line to
					// database
					/*
					 * insert_profiles.executeBatch();
					 * insert_profiles.clearBatch();
					 */
				}
			} catch (SQLException e) {
				e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// don't close database connection, this is done, where is was
		// established
	}

}
