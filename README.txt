Searcher performance comparison suite between Solr, ElasticSearch and Sensei

1. Setup
At first execute mvn install -DskipTests=true
Secondly launch the bin.prepare.sh script to generate the data/cars3m.json file that would contain 3 mln docs

By default the search-perf will load and query the data based on the data/cars.json, data/cars3m.json datasets

The following setup steps are required to prepare the Sensei DB for testing:
Clean all the indexes, copy configs/sensei/schema.xml into the Sensei config folder

The following setup steps are required to prepare the Solr for testing:
Clean all the indexes in the data folder, copy configs/solr/schema.xml into the Solr conf folder
Configure Solr to use json updates http://wiki.apache.org/solr/UpdateJSON
Elastic search doesn't require any setup steps

2. Loading docs into the DB:
It does make sense to do the performance testing when the documents are kept adding to the db

For the Sensei 
 Just use com.senseidb.gateway.file.LinedFileDataProvider in the sensei.properties to load docs from the generated big file data/cars3m.json
Add the following line to sensei.properties
Add sensei.index.manager.default.eventsPerMin = 600000

For Solr execute  bin/loadIndex script. Please uncomment the section specific to Solr

For Elastic Search execute  bin/loadIndex script. Please uncomment the section specific to Elastic Search

The script launches the java program with the following command line params:
type   url documentFilePath numOfThreads
eg 
- elastic http://localhost:9200/cars/car data/cars3m.json 5
- solr http://localhost:8983/solr/update/json/?commit=true data/cars3m.json 5

numOfThreads - number of threads, that are used to post documents to the DB


3. Do the performance test

For Sensei :
bin/perfTest configs/test-sensei.properties
For Solr
bin/perfTest configs/test-solr.properties
For Elastic search
bin/perfTest configs/test-elastic.properties

configs/test-xxx.properties file contains the configuration needed to launch the performance test. The majority of the parameters are self-explaining. 
schemaPath and dataFilePath specify the sensei schema and the file containing documents in the json format. These are needed to generate dynamic search queries

The result of the test will be printed on the console. Essential logs are located in the ./report.log file
