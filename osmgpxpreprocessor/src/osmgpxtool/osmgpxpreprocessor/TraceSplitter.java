package osmgpxtool.osmgpxpreprocessor;

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
	GeometryFactory geomF = new GeometryFactory();

	public TraceSplitter(Properties p) {
		this.p = p;
	}

	public List<GpsTracePart> splitTrace(GpsTrace trace) throws TransformException {
		// through points
		// cut the track where distance > x or heightdifference > x
		// Store cutted trace in multilinestringList
		List<GpsTracePart> parts = new ArrayList<GpsTracePart>();

		MultiLineString fullTrace = trace.getGeom();
		MultiLineString mLineToSplit = fullTrace;
		int partId = 0;
		for (int i = 0; i < fullTrace.getNumGeometries(); i++) {
			LineString line = (LineString) fullTrace.getGeometryN(i);
			for (int a = 0; a < line.getCoordinates().length - 1; a++) {
				// check distance between a and a+1
				double dis = JTS.orthodromicDistance(line.getCoordinates()[a], line.getCoordinates()[a + 1],
						DefaultGeographicCRS.WGS84_3D);
				// check height differece between a and a+1
				double deltaH = Math.abs(line.getCoordinates()[a].z - line.getCoordinates()[a + 1].z);
				//LOGGER.info("trace id:" + trace.getId() + " distance=" + dis + " delta_h=" + deltaH);
				if (dis > Double.valueOf(p.getProperty("splitTraceDis"))
						|| deltaH > Double.valueOf(p.getProperty("splitTraceHeight"))) {
					LOGGER.info("split trace id:" + trace.getId() + " distance=" + dis + " delta_h=" + deltaH);
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

		if (parts.isEmpty()) {
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
					if (a==line.getCoordinates().length-2){
						//add last coordinate
						coords2.add(line.getCoordinates()[a+1]);
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
		mLineArr[0] = geomF.createMultiLineString(lineArr1.toArray(new LineString[lineArr1.size()]));
		mLineArr[1] = geomF.createMultiLineString(lineArr2.toArray(new LineString[lineArr2.size()]));

		return mLineArr;
	}

}
