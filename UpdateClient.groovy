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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


@Grab(group='log4j', module='log4j', version='1.2.17')

@Grab(group='org.restlet.jse', module='org.restlet', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.ext.json', version = '2.2.0')
//@Grab(group='org.restlet.jse', module='org.restlet.ext.simple', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.lib.org.json', version = '2.0') //org.restlet.jse:org.restlet.lib.org.json:2.0



class UpdateClient {
	
	static satoshi = 100000000
	/* just for testing dates..:)
	static plainFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

	static saleStart="2015-05-29 11:48:00"
	static stepLength
*/
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

		def result = httpAsync.request( POST, HTML) { req ->
			body = query

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
	public sendUpdate(vBTCAddress,vBTCAmount,vZOOZAddress,vZOOZAmout,vTime,vRate) {


		def jsonBuilder = new groovy.json.JsonBuilder()

		jsonBuilder(
				BTCInAddress :vBTCAddress,
				BTCAmout: vBTCAmount,
				ZOOZAddress :vZOOZAddress,
				ZOOZAmount :vZOOZAmout,
				Time: vTime,
		        Rate : vRate)

		println(jsonBuilder)

		def httpAsync = new AsyncHTTPBuilder(
				poolSize : 10,
				uri : serverAddress,
				contentType : HTML  ,
		)

		def res = getQueryResult(httpAsync,jsonBuilder)
		println (res)
	}

	def getDateFromStep(numSteps) {
		return new Date(saleStart.time + stepLength * 1000 * numSteps)
	}

	def getStepFromDate(date) {
		return (int)Math.floor(date.time - saleStart.time) / (stepLength * 1000)
	}

	public static int main(String[] args) {


		logger = new Logger()
		PropertyConfigurator.configure("UpdateClient_log4j.properties")
		log4j = logger.getRootLogger()
		log4j.setLevel(Level.INFO)
/*
		def updateClient = new UpdateClient(log4j)
		saleStart=plainFormatter.parse("2015-05-30 01:18:00")
		stepLength=60
		def now = new Date()
		def numSteps = updateClient.getStepFromDate(now)
		def searchDate = updateClient.getDateFromStep(numSteps)
		def parsed = updateClient.plainFormatter.format(searchDate)
		println ("now ${now}")
		println ("numSteps ${numSteps}")
		println ("parsed ${parsed}")

		return

*/

		def updateClient = new UpdateClient(log4j)
		
		//updateClient.init()
		
		log4j.info("Update client started")		
		
		// Begin following blocks
		
		// TODO verify which amounts are satoshi and standardize!!!
		// {"BTCInAddress":"1JCnJLqGmj8TY8A1HKtCxckQFWGFM5aHX2","BTCAmout":500000,"ZOOZAddress":"18MqJzJp7M7Hfqdv7F9PYxD7vLM3sSXDEF","ZOOZAmount":16660002666,"Time":"2015-05-27T08:49:49+0000"}

		updateClient.sendUpdate("1DUv21NTqkxagzkh7yLQTRgetKEXXmoQZo",121,"18MqJzJp7M7Hfqdv7F9PYxD7vLM3sSXDEF",1000,"2015-05-27T08:49:49+0000",0.7)
		sleep(10000)
	}
		

}

