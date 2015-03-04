package osmgpxtool.osmgpxpreprocessor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.geotools.geojson.geom.GeometryJSON;
import org.hsqldb.lib.StringInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

import osmgpxtool.osmgpxpreprocessor.gps.GpsTrace;
import osmgpxtool.osmgpxpreprocessor.gps.GpsTracePart;
import osmgpxtool.osmgpxpreprocessor.writer.PGSqlWriter;

public class GpxPreprocessor {
	static Logger LOGGER = LoggerFactory.getLogger(GpxPreprocessor.class);

	private Connection con;
	private Properties p;
	private PGSqlWriter writer;

	public GpxPreprocessor(Connection dbConnection, Properties props, PGSqlWriter writer) {
		super();
		this.con = dbConnection;
		this.p = props;
		this.writer = writer;
	}

	public void init() {
		// check data
		checkInputdata();

	}

	public void run() throws IOException {
		// get GPS traces
		// Gps3DDataSource gpsData = new Gps3DDataSource(con,
		// p.getProperty("t_gpxName"),p.getProperty("t_gpxGeomCol"));
		Statement s;
		ResultSet rs = null;
		TraceSplitter splitter = new TraceSplitter(p);
		try {
			s = con.createStatement();
			rs = s.executeQuery("SELECT " + p.getProperty("t_gpxIdCol") + "," + "ST_ASGEOJSON("
					+ p.getProperty("t_gpxGeomCol") + ") as " + p.getProperty("t_gpxGeomCol") + " FROM "
					+ p.getProperty("t_gpxName") + ";");
			int counter = 0;
			while (rs.next()) {
				GpsTrace trace = new GpsTrace(rs.getInt(1), parseJson(rs.getString(2)));
				// split Track at points with long distance or big changes in
				// height.
				// if (trace.getGeom().getNumGeometries() > 1) {
				// LOGGER.info(trace.getId() + ", Num Geom: " +
				// trace.getGeom().getNumGeometries());
				// }
				List<GpsTracePart> tracepartList = splitter.splitTrace(trace);
				for (GpsTracePart part : tracepartList) {
					double[] eleMeasurements = getZValuesAsArray(part);
					double[] weights = parseWeights(p.getProperty("smoothingWeights"));
					// smooth track
					WeightedMovingAverageTask wmat = new WeightedMovingAverageTask(eleMeasurements, weights);
					Double[] eleSmoothed = wmat.smoothMeasurements();
					addSmoothedZValues(part, eleSmoothed);

				}

				writer.write(tracepartList);
				LOGGER.info(trace.getId() + " Done. Count: " + counter);
				counter++;
			}
			splitter.closeCSVWriter();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not create Statement");
		} catch (TransformException e) {
			try {
				LOGGER.error("Coordinates may not be in CRS WGS84. GpsTrace id " + rs.getInt(1) + "will be skipped.");
			} catch (SQLException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Could not create Statement");
			}
			e.printStackTrace();
		}

	}

	private void addSmoothedZValues(GpsTracePart part, Double[] eleSmoothed) {

		// create copy of geometry and exchange the z-values

		MultiLineString geomSmoothed = (MultiLineString) part.getGeom().clone();

		// exchange z-Values

		for (int i = 0; i < geomSmoothed.getNumPoints(); i++) {
			geomSmoothed.getCoordinates()[i].z = eleSmoothed[i];
		}

		part.setGeomSmoothed(geomSmoothed);
	}

	private double[] parseWeights(String weights) {
		String[] str = weights.split(",");
		double[] w = new double[str.length];
		for (int i = 0; i < str.length; i++) {
			w[i] = Double.valueOf(str[i].replaceAll(" ", ""));
		}
		return w;
	}

	private double[] getZValuesAsArray(GpsTracePart part) {
		double[] zValues = new double[part.getGeom().getNumPoints()];

		for (int i = 0; i < zValues.length; i++) {
			zValues[i] = part.getGeom().getCoordinates()[i].z;
		}

		return zValues;
	}

	private MultiLineString parseJson(String json) {
		JSONObject obj = null;
		MultiLineString multiLine = null;
		GeometryFactory geomF = new GeometryFactory(new PrecisionModel(), 4326);
		try {
			obj = new JSONObject(json);
			String type = obj.getString("type");

			if (!type.equals("MultiLineString")) {
				return null;
			} else {
				List<LineString> lineList = new ArrayList<LineString>();
				// get lines of MultiLineString
				JSONArray lines = obj.getJSONArray("coordinates");
				for (int i = 0; i < lines.length(); i++) {
					// get points of line
					JSONArray points = lines.getJSONArray(i);
					List<Coordinate> pointList = new ArrayList<Coordinate>();
					for (int a = 0; a < points.length(); a++) {
						// get coordinate
						JSONArray coord = points.getJSONArray(a);
						pointList.add(new Coordinate(coord.getDouble(0), coord.getDouble(1), coord.getDouble(2)));
					}
					lineList.add(geomF.createLineString(pointList.toArray(new Coordinate[pointList.size()])));
				}
				multiLine = geomF.createMultiLineString(lineList.toArray(new LineString[lineList.size()]));
				multiLine.setSRID(4326);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return multiLine;

	}



	public void close() {

	}

	/**
	 * This method checks, if all table and columns name, given in
	 * matching.properties exist in the database. If not all given names are
	 * found, the program will exit.
	 */
	private void checkInputdata() {

		try {
			Statement s = con.createStatement();

			ResultSet rs = s.executeQuery("SELECT * FROM " + p.getProperty("t_gpxName") + " WHERE false");
			try {
				rs.findColumn(p.getProperty("t_gpxIdCol"));
				rs.findColumn(p.getProperty("t_gpxGeomCol"));
			} catch (SQLException e) {
				LOGGER.error("Coloumn is missing in gpx table.");
				e.printStackTrace();
				System.exit(1);
			}

			s.close();
		} catch (SQLException e) {
			LOGGER.error("Could not find table.");
			e.printStackTrace();
			System.exit(1);
		}

	}

}
