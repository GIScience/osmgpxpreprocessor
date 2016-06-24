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
5. if necessary, adjust the properties file in resources/preprocessor.properties
6. run maven `$ mvn clean package`
7. start application `java -jar target/osmgpxpreprocessor-0.1.jar <args>`

### Usage
```
 -h,--help              displays help
Required Arguments:
 -D,--database          Name of database
 -PW,--password <arg>   Password of DB-User
 -U,--user <arg>        Name of DB-Username


Optional Arguments:
 -H,--host <arg>        Database host <default:localhost>
 -P,--port <arg>        Database port <default:5432>
 -s <arg>               Suffix of output table in database. <default:preprocessed>
 
Example java -jar target/osmgpxpreprocessor-0.1.jar -D gpx_db -U postgres -PW xxx


 ```
 
### Citation

When using this software for scientific purposes, please cite:

John, S., Hahmann, S., Rousell, A., Loewner, M., Zipf, A. (2016): Deriving incline values for street networks from voluntarily collected GPS traces. Cartography and Geographic Information Science (CaGIS). Taylor & Francis. http://dx.doi.org/10.1080/15230406.2016.1190300 (author manuscript: http://koenigstuhl.geog.uni-heidelberg.de/publications/2016/Hahmann/John_et_al_2016.pdf, version as accepted).
 
 
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
 