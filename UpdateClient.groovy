/**
* Created by jeremy on 1/04/14.
*/

@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')


import org.apache.log4j.*
import groovy.sql.Sql
import java.util.Timer
import java.util.Date
import java.text.SimpleDateFormat

import static groovyx.net.http.ContentType.* 
import static groovyx.net.http.Method.POST 
import groovyx.net.http.AsyncHTTPBuilder 


class UpdateClient {
	
	static satoshi = 100000000
	
	static logger
	static log4j
	static db
	
	static serverAddress
	static hostAddress
	static updateInterval
	
	// time of start - yyyy-MM-dd hh:mm:ss
	static saleStart
	// step length in seconds
	static stepLength
	// the number of steps allowed
	static maxSteps
	// Starting rate
	static startRate
	
	static plainFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
	static databaseName

	static http		
	
	public init() {
		// Set up some log4j stuff
		logger = new Logger()
		PropertyConfigurator.configure("UpdateClient_log4j.properties")
		log4j = logger.getRootLogger()
		log4j.setLevel(Level.INFO)
		
		def iniConfig = new ConfigSlurper().parse(new File("UpdateClient.ini").toURL())        
		databaseName = iniConfig.database.name
		
		def saleConfig = new ConfigSlurper().parse(new File("CrowdSale.ini").toURL())
		// The sale's starting time (YYYY-MM-DD hh:mm:ss)
		saleStart = plainFormatter.parse(saleConfig.saleStart)
		// We don't want to miss steps - don't make them too short 
		stepLength = saleConfig.stepLength
		maxSteps = saleConfig.maxSteps		
		startRate = saleConfig.startRate
		
		serverAddress = iniConfig.serverAddress
		hostAddress = iniConfig.hostAddress
		updateInterval = iniConfig.updateInterval
		
		// Init database
		db = Sql.newInstance("jdbc:sqlite:${databaseName}", "org.sqlite.JDBC")
		db.execute("PRAGMA busy_timeout = 2000")
		DBCreator.createDB(db)

		http = new AsyncHTTPBuilder(
                poolSize : 2,
                uri : hostAddress,
                contentType : JSON )

	
	}
	
	
	
	public sendUpdate() {
		def result = constructResult()
		log4j.info("About to send result: ${result}")
		
		http.request(POST) {
			uri.path = serverAddress
			body = result
		
			response.success = { resp ->
				println "Success! ${resp.status}"
			}
			
			response.failure = { resp ->
				println "Request failed with status ${resp.status}"			
			}
		}
	}

	
	// day should be YYYY-MM-DD
	private getRecord(date) {
		def row = db.firstRow("select * from crowdsale where dateString = ${date}")		
		return row
	}
	
	
	def getDateFromStep(numSteps) {
		return new Date(saleStart.time + stepLength * 1000 * numSteps)
	}
	
	def getStepFromDate(date) {
		return (int)Math.floor(date.time - saleStart.time) / (stepLength * 1000) 
	}
	
	private getRate(steps) {
		return startRate*Math.exp(steps*steps*1.0/(76*76))
	}
	
	
	
	private constructResult() {
		def now = new Date()
		def numSteps = getStepFromDate(now)
		def searchDate = getDateFromStep(numSteps)
		def parsed = plainFormatter.format(searchDate)		
		
		def soldPrev
		def soldCur
		def curPrice		
		
		// Select from database to see if it's there
		def cur = getRecord(parsed)
		if (cur == null) {
			soldCur = 0
			// if not - create new record and update
			def priceSteps = 0			
			if (numSteps > 0) {		
				def prevString = plainFormatter.format(getDateFromStep(numSteps-1))
				def prev = getRecord(prevString)
				soldPrev = prev.sold
				priceSteps = prev.steps + 1
			} else {
				priceSteps = 1
				soldPrev = 0
			}
			curPrice = getRate(priceSteps)
		} else {
			curPrice = cur.rate
			soldCur = cur.sold
			soldPrev = cur.prevSold
		}
		
		def timeToNext = (int) (getDateFromStep(numSteps + 1).time - now.time) / 1000
		
		return [ 	
			"sale_period" :"${maxSteps}" ,
			"sale_start_price" :"${startRate}",
  			"current_day": "${numSteps}" ,
  			"sold_yesterday":"${soldPrev}" ,
  			"sold_today"      :"${soldCur}" ,
  			"current_price"      :"${curPrice}" ,
  			"time_till_next_time_step": "${timeToNext}"
  		]
	}
	
	// We don't really use the current block... 
	// This is the major thing that needs to be fixed. We shall assume that we have different addresses,
	// so that we can discover... 
	public static int main(String[] args) {
		def updateClient = new UpdateClient()
		
		updateClient.init()
		
		log4j.info("Update client started")		
		
		// Begin following blocks
		
		// TODO verify which amounts are satoshi and standardize!!!
		while (true) {			
			updateClient.sendUpdate()
			
			sleep(updateInterval * 1000)
		}
		
	}
}

