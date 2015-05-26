@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')


import org.apache.log4j.*
import groovy.sql.Sql
import java.util.Timer
import java.util.Date
import java.text.SimpleDateFormat
import org.json.JSONObject
import org.restlet.ext.json.JsonRepresentation

import static groovyx.net.http.ContentType.* 
import static groovyx.net.http.Method.POST 
import groovyx.net.http.AsyncHTTPBuilder
import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET

@Grab(group='log4j', module='log4j', version='1.2.17')

@Grab(group='org.restlet.jse', module='org.restlet', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.ext.json', version = '2.2.0')
//@Grab(group='org.restlet.jse', module='org.restlet.ext.simple', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.lib.org.json', version = '2.0') //org.restlet.jse:org.restlet.lib.org.json:2.0



class UpdateClient {
	
	static satoshi = 100000000
	
	static logger
	static log4j
	static db
	
	static serverAddress
	static hostAddress
	static updateInterval

	static http

	public UpdateClient(loggerA)
	{
		log4j = loggerA
		init()

	}
	
	public init() {

		serverAddress = "http://lazooz.org/wp2/inter/lazooz1.4/zooz_sale_interface/confirmation.php"

	
	}



	private getQueryResult(httpAsync,query) {
		def body =null
		def url = serverAddress
		def result = httpAsync.request( POST, HTML) { req ->
			uri.query = query

			response.success = { resp, json ->
				return json
			}
			response.failure = { resp ->
				// Check returned status'
				//assertResp(resp, expStatus);
			}
		}

		assert result instanceof java.util.concurrent.Future
		while ( ! result.done ) {
			Thread.sleep(100)
		}
		return result.get()

	}
	public sendUpdate(vBTCAddress,vBTCAmount,vZOOZAddress,vZOOZAmout,vTime) {


		def jsonBuilder = new groovy.json.JsonBuilder()

		jsonBuilder(
				BTCAddress :vBTCAddress,
				BTCAmout:vBTCAmount,
				ZOOZAddress :vZOOZAddress,
				ZOOZAmount :vZOOZAmout,
				Time:vTime)

		println(jsonBuilder)

		def result = [ BTCAddress :vBTCAddress ,
				BTCAmout  :vBTCAmount ,
				ZOOZAddress :vZOOZAddress,
				ZOOZAmount :vZOOZAmout ,
				Time:vTime
				]
		println(result)

		def httpAsync = new AsyncHTTPBuilder(
				poolSize : 10,
				uri : serverAddress,
				contentType : HTML  ,

		)



		def res = getQueryResult(httpAsync,result)
		println (res)
	}


	public static int main(String[] args) {


		logger = new Logger()
		PropertyConfigurator.configure("UpdateClient_log4j.properties")
		log4j = logger.getRootLogger()
		log4j.setLevel(Level.INFO)

		def updateClient = new UpdateClient(log4j)
		
		//updateClient.init()
		
		log4j.info("Update client started")		
		
		// Begin following blocks
		
		// TODO verify which amounts are satoshi and standardize!!!

		updateClient.sendUpdate("xxx",100,"yyy",1000,"now")
		sleep(10000)
	}
		

}

