package osmgpxtool.preprocessor.gps;

import com.vividsolutions.jts.geom.MultiLineString;


public class GpsTracePart extends GpsTrace {
	private int partId;
	private MultiLineString geomSmoothed;

	public MultiLineString getGeomSmoothed() {
		return geomSmoothed;
	}


	public void setGeomSmoothed(MultiLineString geomSmoothed) {
		this.geomSmoothed = geomSmoothed;
	}

	public GpsTracePart(int gpsId, int trkId, int partId, MultiLineString geom) {
		super(gpsId,trkId, geom);
		this.partId = partId;
	}

	public int getPartId() {
		return partId;
	}

	public void setPartId(int partId) {
		this.partId = partId;
	}

}
