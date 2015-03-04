package osmgpxtool.osmgpxpreprocessor;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import osmgpxtool.osmgpxpreprocessor.gps.GpsTrace;
import osmgpxtool.osmgpxpreprocessor.gps.GpsTracePart;

public class TraceSplitter {
	static Logger LOGGER = LoggerFactory.getLogger(TraceSplitter.class);

	private Properties p;
	private GeometryFactory geomF = new GeometryFactory();
	private FileWriter writer;

	public TraceSplitter(Properties p) throws IOException {
		this.p = p;

		// TODO: uncomment if csv should be written
		/*
		 * writer = new FileWriter(
		 * "C:/Users/Steffen/Documents/_Studium/Master/5. Semester aka Thesis/playground/d_h__d_d_test.csv"
		 * ); writer.append("Gpx_id;lon;lat;d_h;distance\n");
		 */
	}

	/**
	 * This method splits a {@link GpsTrace} into {@link GpsTracePart}s. A trace
	 * is splitted when the distance between two points is bigger than given in
	 * the properties file or the height difference between two points is bigger
	 * than given in the properties file.
	 * 
	 * The returned ArrayList of GpsTraceParts may be empty if the splitting
	 * condition is met at all points in the GpsTrace.
	 * 
	 * @param trace
	 * @return
	 * @throws TransformException
	 * @throws IOException
	 */
	public List<GpsTracePart> splitTrace(GpsTrace trace) throws TransformException, IOException {
		// through points
		// cut the track where distance > x or heightdifference > x
		// Store cutted trace in multilinestringList
		List<GpsTracePart> parts = new ArrayList<GpsTracePart>();

		MultiLineString fullTrace = trace.getGeom();
		MultiLineString mLineToSplit = fullTrace;
		int partId = 0;
		boolean splittingConditionMet = false;
		for (int i = 0; i < fullTrace.getNumGeometries(); i++) {
			LineString line = (LineString) fullTrace.getGeometryN(i);
			for (int a = 0; a < line.getCoordinates().length - 1; a++) {
				// check distance between a and a+1
				double dis = JTS.orthodromicDistance(line.getCoordinates()[a], line.getCoordinates()[a + 1],
						DefaultGeographicCRS.WGS84_3D);
				// check height differece between a and a+1
				double deltaH = Math.abs(line.getCoordinates()[a].z - line.getCoordinates()[a + 1].z);
				// LOGGER.info("trace id:" + trace.getId() + " distance=" + dis
				// + " delta_h=" + deltaH);

				// write into csv: lon,lat,lon1,lat1, d_h, dis
				// TODO: uncomment if csv should be written
				/*
				 * DecimalFormat df = new DecimalFormat("####0.000");
				 * 
				 * writer.append(trace.getId() +";" + line.getCoordinates()[a].x
				 * +";"+ line.getCoordinates()[a].y +";"+ df.format(deltaH)
				 * +";"+ df.format(dis)+"\n");
				 */

				if (dis < 0.1d || dis > Double.valueOf(p.getProperty("splitTraceDis"))
						|| deltaH > Double.valueOf(p.getProperty("splitTraceHeight"))) {
					splittingConditionMet = true;
					// LOGGER.info("split trace id:" + trace.getId() +
					// " distance=" + dis + " delta_h=" + deltaH);
					// split MultiLineString
					MultiLineString[] splitted = splitMultiLineString(mLineToSplit, line.getCoordinates()[a]);

					// splitted[0] add to new GPStracePart
					// splitted[0] may be empty. if it is empty, don't add it to
					// parts
					if (splitted[0].getNumGeometries() > 0) {
						parts.add(new GpsTracePart(trace.getId(), partId, splitted[0]));
						partId++;
					}
					// splitted[1] assign to mLineTosplit
					mLineToSplit = splitted[1];

				} else {
					// save distance and deltaH for statistic reasons
				}
			}

		}// end for fulltrace
		/*
		 * There might be two reason why parts is empty: 1. Gps trace does not
		 * need to be splitted 2. the splitting condition is met after each
		 * point
		 */
		if (parts.isEmpty() && splittingConditionMet == false) {
			parts.add(new GpsTracePart(trace.getId(), 0, trace.getGeom()));
		}
		return parts;
	}

	private MultiLineString[] splitMultiLineString(MultiLineString mLine, Coordinate c) {
		List<LineString> lineArr1 = new ArrayList<LineString>();
		List<LineString> lineArr2 = new ArrayList<LineString>();
		List<Coordinate> coords1 = new ArrayList<Coordinate>();
		List<Coordinate> coords2 = new ArrayList<Coordinate>();

		boolean splittingPointReached = false;
		for (int i = 0; i < mLine.getNumGeometries(); i++) {
			LineString line = (LineString) mLine.getGeometryN(i);
			for (int a = 0; a < line.getCoordinates().length - 1; a++) {
				if (splittingPointReached == false) {
					coords1.add(line.getCoordinates()[a]);
				} else {
					coords2.add(line.getCoordinates()[a]);
					if (a == line.getCoordinates().length - 2) {
						// add last coordinate
						coords2.add(line.getCoordinates()[a + 1]);
					}
				}

				// check whether current coordinate is equals to the given
				// coordinate at which the MultiLineString has to be splitted
				if (c.equals3D(line.getCoordinates()[a])) {
					splittingPointReached = true;
				}
			}

			if (coords1.size() > 1) {
				lineArr1.add(geomF.createLineString(coords1.toArray(new Coordinate[coords1.size()])));
			}
			coords1 = new ArrayList<Coordinate>();

			if (coords2.size() > 1) {
				lineArr2.add(geomF.createLineString(coords2.toArray(new Coordinate[coords2.size()])));
			}
			coords2 = new ArrayList<Coordinate>();

		}

		// build Array

		MultiLineString[] mLineArr = new MultiLineString[2];
		MultiLineString mLine1 = geomF.createMultiLineString(lineArr1.toArray(new LineString[lineArr1.size()]));
		mLine1.setSRID(4326);
		MultiLineString mLine2 = geomF.createMultiLineString(lineArr2.toArray(new LineString[lineArr2.size()]));
		mLine2.setSRID(4326);

		mLineArr[0] = mLine1;
		mLineArr[1] = mLine2;

		return mLineArr;
	}

	public void closeCSVWriter() throws IOException {
		// TODO uncomment if csv is written
		/*
		 * writer.flush(); writer.close();
		 */
	}

}
