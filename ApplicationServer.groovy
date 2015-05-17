/**
 * Created by whoisjeremylam on 12/05/14.
 *
 * Thanks to https://bitbucket.org/jsumners/restlet-2.1-demo for a sane example using Restlet
 */
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.restlet.Component
import org.restlet.data.Protocol
import org.restlet.data.Status
import org.restlet.data.Form
import org.restlet.resource.Post
import org.restlet.resource.Get
import org.restlet.resource.ServerResource
import org.restlet.Application
import org.restlet.Restlet
import org.restlet.routing.Router
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import org.restlet.ext.json.JsonRepresentation
import groovy.sql.Sql
import java.util.Timer
import java.util.Date
import java.text.SimpleDateFormat
import org.restlet.engine.header.DispositionReader
import org.restlet.engine.header.HeaderConstants
import org.restlet.engine.header.Header
import org.restlet.util.Series

@Grab(group='log4j', module='log4j', version='1.2.17')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')

@Grab(group='org.restlet.jse', module='org.restlet', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.ext.json', version = '2.2.0')
@Grab(group='org.restlet.jse', module='org.restlet.lib.org.json', version = '2.0') //org.restlet.jse:org.restlet.lib.org.json:2.0

import org.apache.log4j.*
import groovy.sql.Sql


class ApplicationServer {

	static myLogger
	static log4j
	static satoshi = 100000000

	static logger
	static db

	static serverAddress
	static hostAddress
	static updateInterval

	// time of start - yyyy-MM-dd hh:mm:ss
	static saleStart
	static saleEnd
	// step length in seconds
	static stepLength
	// the number of steps allowed
	static maxSteps
	// Starting rate
	static startRate

	static plainFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
	static databaseName

    public static class GetStatus extends ServerResource {


        @Override
        public void doInit() {
	    }

		@Post("form:html")
        public String submit(Form form) {
			//ApplicationServer.checkSecret(form.getFirstValue("secret"))
			log4j.info("Application Server submit")

			result = ["message":"success oren"]

            response = this.getResponse()
            response.setStatus(Status.SUCCESS_CREATED)
            response.setEntity(new JsonBuilder(result))
        }
        @Get("txt")
	public String toString() {
	    // Print the requested URI path
	    def JSONObject jsonObject = new JSONObject()
	    def JsonRepresentation jsonRepresentation

	    def result = constructResult()

	   // jsonObject.put("message","success oren")

	    log4j.info("Application Server get")



            response = this.getResponse()

             Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers")
             response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS)
              if (responseHeaders == null) {
                  responseHeaders = new Series(Header.class)

                  response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,responseHeaders)
                  }
             responseHeaders.add(new Header("Access-Control-Allow-Origin", "*"))

           response.setStatus(Status.SUCCESS_CREATED)


            jsonRepresentation = new JsonRepresentation(result)

            response.setEntity(jsonRepresentation)

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
		if (numSteps > maxSteps)
			numSteps = maxSteps
		if (numSteps < 0)
			numSteps = 0

		def searchDate = getDateFromStep(numSteps)
		def parsed = plainFormatter.format(searchDate)

		def soldPrev = 0
		def soldCur
		def curPrice

		// Select from database to see if it's there
		def cur = getRecord(parsed)
		if (cur == null) {
			soldCur = 0
			// if not - create new record and update
			 log4j.info("Application Server get ${numSteps} ${maxSteps}" )
			def priceSteps = 0
			if (numSteps > 0) {
				def prevString = plainFormatter.format(getDateFromStep(numSteps-1))
				def prev = getRecord(prevString)
				if (prev != null)
				{
				 soldPrev = prev.sold
				 priceSteps = prev.steps + 1
				}
			} else {
				log4j.info("Application Server get curPrice priceSteps = 1" )
				priceSteps = 0
				soldPrev = 0
			}
			curPrice = getRate(priceSteps)
		} else {
			log4j.info("Application Server get curPrice = cur.rate" )
			curPrice = cur.rate
			soldCur = cur.sold
			soldPrev = cur.prevSold
		}

		def timeToNext = (int) (getDateFromStep(numSteps + 1).time - now.time) / 1000
		if (timeToNext < 0 )
			timeToNext = 0


		def  sale_end_in = Math.round((saleEnd.time - now.time)/1000)

		if (saleEnd.time < now.time) {
			sale_end_in = 0
		}

		def sale_start_price = startRate*0.01
		def current_price = curPrice*0.01






		return [
			"sale_end_in" :"${sale_end_in}" ,
			"sale_start_price" :"${sale_start_price}",
  			"current_step": "${numSteps}" ,
  			"sold_yesterday":"${soldPrev}" ,
  			"sold_today"      :"${soldCur}" ,
  			"current_price"      :"${current_price}" ,
  			"time_till_next_time_step": "${timeToNext}"
  		]
	}
	 }

	public static class GetBalance extends ServerResource {

		String userAddress;

		Object address;

		@Override
		public void doInit() {
			this.userAddress = (String) getRequestAttributes().get("address");
			this.address = null; // Could be a lookup to a domain object.
		}

		@Post("form:html")
		public String submit(Form form) {
			//ApplicationServer.checkSecret(form.getFirstValue("secret"))
			log4j.info("Application Server submit")

			result = ["message":"success oren"]

			response = this.getResponse()
			response.setStatus(Status.SUCCESS_CREATED)
			response.setEntity(new JsonBuilder(result))
		}
		@Get("txt")
		public String toString() {
			// Print the requested URI path
			def JSONObject jsonObject = new JSONObject()
			def JsonRepresentation jsonRepresentation

			def result = constructResult(this.userAddress)

			// jsonObject.put("message","success oren")

			log4j.info("Application Server get "+this.userAddress)



			response = this.getResponse()

			Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers")
			response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS)
			if (responseHeaders == null) {
				responseHeaders = new Series(Header.class)

				response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,responseHeaders)
			}
			responseHeaders.add(new Header("Access-Control-Allow-Origin", "*"))

			response.setStatus(Status.SUCCESS_CREATED)


			jsonRepresentation = new JsonRepresentation(result)

			response.setEntity(jsonRepresentation)

		}




		// day should be YYYY-MM-DD
		private getRecord(address) {

			def row = db.firstRow("select * from crowdsalelist where destination = ${address}")
			return row
		}


		private constructResult(address) {

			def cur = getRecord(address)

			def zooz_balance = 0
			if (cur != null)
				zooz_balance = cur.amount
			zooz_balance = zooz_balance/100000000

			return [
					"zooz_balance" :"${zooz_balance}"
			]
		}
	}

    public static class ApplicationServerApplication extends Application {

        /**
         * Creates a root Restlet that will receive all incoming calls.
         */
        @Override
        public synchronized Restlet createInboundRoot() {
            // Create a router Restlet that routes each call to a new instance of HelloWorldResource.
            Router router = new Router(getContext())

			//router.attach("/initiate_block_chain_report", InitiateBlockChainReportResource.class)
			router.attach("/get_zooz_balance/{address}", GetBalance.class)
			router.attach("/crowdsale_status", GetStatus.class)

            return router
        }

    }

    static init() {
			// Set up some log4j stuff
			myLogger = new Logger()
			PropertyConfigurator.configure("ApplicationServer_log4j.properties")
			log4j = myLogger.getRootLogger()
			log4j.setLevel(Level.INFO)

		def iniConfig = new ConfigSlurper().parse(new File("UpdateClient.ini").toURL())
		databaseName = iniConfig.database.name

		def saleConfig = new ConfigSlurper().parse(new File("CrowdSale.ini").toURL())
		// The sale's starting time (YYYY-MM-DD hh:mm:ss)
		saleStart = plainFormatter.parse(saleConfig.saleStart)
		saleEnd = plainFormatter.parse(saleConfig.saleEnd)
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

    }


    public static void main(String[] args) throws Exception {
        def serverApp = new ApplicationServerApplication()
        init()

        log4j.info("Application Server Start")

        // Create a new Component.
        Component component = new Component()

        component.getServers().add(Protocol.HTTP, 8080)

        // Attach the sample application.
        component.getDefaultHost().attach("", serverApp)

        // Start the component.
        component.start()
    }
}
