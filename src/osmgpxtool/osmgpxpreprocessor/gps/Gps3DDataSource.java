package osmgpxtool.osmgpxpreprocessor.gps;

import java.sql.Connection;

public class Gps3DDataSource {
	private String tableName;
	private String geomCol;
	private Connection con;

	public Gps3DDataSource(Connection con, String tableName, String geomCol) {
		this.tableName = tableName;
		this.geomCol = geomCol;
		this.con=con;
	}

	public void loadData() {
		
		
		
		
	}

	public boolean next() {
		// TODO Auto-generated method stub
		return false;
	}

}
