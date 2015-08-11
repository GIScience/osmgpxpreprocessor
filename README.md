# OsmGpxPreprocessor
OsmGpxPreprocessor is a tool to preprocess OSM gpx traces. It does following processing:
	-	split trace at trackpoints with long distance to consecutive track point
	-	split trace at trackpoints with high change in elevation to consecutive track point
	-   smooth elevation profile of trace
	
Ideally, the traces have been imported to the PostgresQL/PostGIS database, using the [OsmGpxFilter](https://github.com/GIScience/osmgpxfilter)


### Getting started

1. install maven
2. install git
3. clone project `$ git clone https://github.com/GIScience/osmgpxpreprocessor`
4. go into project directory `$ cd osmgpxpreprocessor/`
5. run maven `$ mvn clean package`
6. start application `java -jar target/osmgpxpreprocessor-0.1.jar <args>`

### Usage
```
 -h,--help              displays help
 -D,--database          Name of database
 -H,--host <arg>        Database host <default:localhost>
 -P,--port <arg>        Database port <default:5432>
 -PW,--password <arg>   Password of DB-User
 -U,--user <arg>        Name of DB-Username
 -s <arg>               Suffix of output table in database. <default:preprocessed>


Example java -jar target/osmgpxpreprocessor-0.1.jar -D gpx_db -U postgres -PW xxx


 ```
 
### Citation

When using this software for scientific purposes, please cite:

John, S., Hahmann, S., Zipf, A., Bakillah, M., Mobasheri, A., Rousell, A. (2015): [Towards deriving incline values for street networks from voluntarily collected GPS data] (http://koenigstuhl.geog.uni-heidelberg.de/publications/2015/Hahmann/GI_Forum_GPS.pdf). Poster session, GI Forum. Salzburg, Austria.
 
 ```
 /*|----------------------------------------------------------------------------------------------
 *|														Heidelberg University
 *|	  _____ _____  _____      _                     	Department of Geography		
 *|	 / ____|_   _|/ ____|    (_)                    	Chair of GIScience
 *|	| |  __  | | | (___   ___ _  ___ _ __   ___ ___ 	(C) 2014
 *|	| | |_ | | |  \___ \ / __| |/ _ \ '_ \ / __/ _ \	
 *|	| |__| |_| |_ ____) | (__| |  __/ | | | (_|  __/	Berliner Strasse 48								
 *|	 \_____|_____|_____/ \___|_|\___|_| |_|\___\___|	D-69120 Heidelberg, Germany	
 *|	        	                                       	http://www.giscience.uni-hd.de
 *|								
 *|----------------------------------------------------------------------------------------------*/
 ```
 