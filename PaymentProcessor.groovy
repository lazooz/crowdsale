/**
* Created by jeremy on 1/04/14.
*/

//@Grab(group=' //log4j.', module=' //log4j.', version='1.2.17')
//@Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2')

//import org.apache. //log4j..*
import groovy.sql.Sql
import java.util.Timer
import java.util.Date
import java.text.SimpleDateFormat

class PaymentProcessor {

	static mastercoinAPI
	static bitcoinAPI
	static bitcoinRateAPI
	static UpdateClientAPI
	static String walletPassphrase
	static int sleepIntervalms
	static String databaseName
	static String hotWalletAddress
	static int walletUnlockSeconds

	static assetConfig
	static satoshi = 100000000

//	static logger
	static  log4j
	static db

	static int currentBTCValueInUSD
	static int exchangeRateUpdateRate
	static plainFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	// time of start - yyyy-MM-dd hh:mm:ss
	static saleStart
	// time of End - yyyy-MM-dd hh:mm:ss
	static saleEnd

	// step length in seconds
	static stepLength
	// the number of steps allowed
	static maxSteps
	// Starting rate
	static startRate

	class Payment {
		def blockIdSource
		def txid
		def sourceAddress
		def destinationAddress
		def outAsset
		def outAssetType
		def status
		def lastUpdatedBlockId
		def inAssetType
		def inAmount

		public Payment(blockIsSourceValue, txidValue, sourceAddressValue, inAssetTypeValue, inAmountValue, destinationAddressValue, outAssetValue, outAssetTypeValue, statusValue, lastUpdatedBlockIdValue) {
			blockIdSource = blockIsSourceValue
			txid = txidValue
			sourceAddress = sourceAddressValue
			destinationAddress = destinationAddressValue
			outAsset = outAssetValue
			outAssetType = outAssetTypeValue
			inAssetType = inAssetTypeValue
			inAmount = inAmountValue
			status = statusValue
			lastUpdatedBlockId = lastUpdatedBlockIdValue
		}
	}




	public init(dbsqlite) {

		// Set up some  //log4j. stuff
/*
		logger = new Logger()
		PropertyConfigurator.configure("PaymentProcessor_ //log4j..properties")
		 //log4j. = logger.getRootLogger()
		 //log4j..setLevel(Level.INFO)
*/

		mastercoinAPI = new MastercoinAPI( log4j)
		bitcoinAPI = new BitcoinAPI(mastercoinAPI.getHttpAsync(), log4j)
		bitcoinRateAPI = new BitcoinRateAPI()
		UpdateClientAPI = new UpdateClient(log4j)

		// Read in ini file
		def iniConfig = new ConfigSlurper().parse(new File("PaymentProcessor.ini").toURL())
		walletPassphrase = iniConfig.bitcoin.walletPassphrase
		sleepIntervalms = iniConfig.sleepIntervalms
		databaseName = iniConfig.database.name
		hotWalletAddress = iniConfig.hotWalletAddress
		walletUnlockSeconds = iniConfig.walletUnlockSeconds

		def saleConfig = new ConfigSlurper().parse(new File("CrowdSale.ini").toURL())
		// The sale's starting time (YYYY-MM-DD hh:mm:ss)
		saleStart = plainFormatter.parse(saleConfig.saleStart)
		saleEnd = plainFormatter.parse(saleConfig.saleEnd)
		// We don't want to miss steps - don't make them too short
		stepLength = saleConfig.stepLength
		maxSteps = saleConfig.maxSteps
		startRate = saleConfig.startRate

		assetConfig = Asset.readAssets("AssetInformation.ini")

		// Init database
		/*
		db = Sql.newInstance("jdbc:sqlite:${databaseName}", "org.sqlite.JDBC")
		db.execute("PRAGMA busy_timeout = 2000")
		DBCreator.createDB(db)
		*/
		db = dbsqlite
		currentBTCValueInUSD = bitcoinRateAPI.getAveragedRate()

		 //log4j..info("Exchange rate is: ${currentBTCValueInUSD} USD for 1 BTC")

		// every 20 minutes
		/*
		def exchangeRateUpdateRate = 20 * 60 *  1000

		Timer timer = new Timer()
		timer.scheduleAtFixedRate(new BTCUSDRateUpdateTask(), exchangeRateUpdateRate, exchangeRateUpdateRate)
		*/
		PaymentProcessor.currentBTCValueInUSD = PaymentProcessor.bitcoinRateAPI.getAveragedRate()
		//PaymentProcessor. //log4j..info("Updated exchange rate is: ${PaymentProcessor.currentBTCValueInUSD} USD for 1 BTC")
         //log4j..info("Updated exchange rate is: ${PaymentProcessor.currentBTCValueInUSD} USD for 1 BTC")
	}


	public audit() {

	}

	static class BTCUSDRateUpdateTask extends TimerTask {
		public void run() {
            currentBTCValueInUSD =  bitcoinRateAPI.getAveragedRate()
            log4j.info("Updated exchange rate is: ${ currentBTCValueInUSD} USD for 1 BTC")
		}
	}

	public getLastPaymentBlock() {
		def row

		row = db.firstRow("select max(lastUpdatedBlockId) from payments where status in ('complete')")


		if (row == null || row[0] == null) {
			return 0
		} else {
			return row[0]
		}
	}


	public getNextPayment() {
		def Payment result
		def row

		row = db.firstRow("select * from payments where status='authorized' order by blockId")

		if (row == null || row[0] == null) {
			result = null
		} else {
			def blockIdSource = row.blockId
			def txid = row.SourceTxid
			def sourceAddress = row.sourceAddress
			def destinationAddress = row.destinationAddress
			def outAsset = row.outAsset
			def status = row.status
			def lastUpdated = row.lastUpdatedBlockId
			def outAssetType = row.outAssetType
			def inAssetType = row.inAssetType
			def inAmount = row.inAmount

			result = new Payment(blockIdSource, txid, sourceAddress, inAssetType, inAmount, destinationAddress, outAsset, outAssetType, status, lastUpdated)
		}

		return result
	}

	private findAssetConfig(Payment payment) {
		// Better to check by asset type
		for (assetRec in assetConfig) {
			//if (payment.sourceAddress == assetRec.nativeAddressCounterparty || payment.sourceAddress == assetRec.nativeAddressMastercoin || payment.sourceAddress == assetRec.counterpartyAddress || payment.sourceAddress == assetRec.mastercoinAddress) {
				return assetRec

		}
		return null
	}

	public pay(Long currentBlock, Payment payment, BigDecimal outAmount,rate) {
		// input in satoshis
		def sourceAddress = hotWalletAddress
		def blockIdSource = payment.blockIdSource
		def destinationAddress = payment.destinationAddress
		def asset = payment.outAsset
		 log4j.info("pay")
		def amount = Math.round(outAmount)
		if (amount < 0) {
			 log4j.info("Not enough left after paying fees...")
			 log4j.info("update payments set status='negative', lastUpdatedBlockId = ${currentBlock} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")
			db.execute("update payments set status='negative', lastUpdatedBlockId = ${currentBlock} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")
			return
		}


		 log4j.info("Processing payment ${payment.blockIdSource} ${payment.txid}. Sending ${amount} ${payment.outAsset} from ${sourceAddress} to ${payment.destinationAddress}")

        /*Comment this one...for the crowd sale --will send when the crowd sale will finish
		bitcoinAPI.lockBitcoinWallet() // Lock first to ensure the unlock doesn't fail
		bitcoinAPI.unlockBitcoinWallet(walletPassphrase, 30)

		// send transaction

		 log4j.info("Sending a mastercoin transaction!")
		try {
			mastercoinAPI.sendAsset(s/ourceAddress, destinationAddress, asset.toString(),1.0 * amount / satoshi)
			 log4j.info("update payments set status='complete', lastUpdatedBlockId = ${currentBlock} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")
			db.execute("update payments set status='complete', lastUpdatedBlockId = ${currentBlock} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")
		}
		catch (Throwable e) {
			 log4j.info(e)
			 log4j.info("update payments set status='error', lastUpdatedBlockId = ${currentBlock} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")
			db.execute("update payments set status='error', lastUpdatedBlockId = ${currentBlock} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")

			assert false
                }



		// Lock bitcoin wallet
		bitcoinAPI.lockBitcoinWallet()
		*/

        updateSold(amount,destinationAddress,payment.sourceAddress)
        db.execute("update payments set status='complete', lastUpdatedBlockId = ${currentBlock}, outAmount=${amount} where blockId = ${blockIdSource} and sourceTxid = ${payment.txid}")
		def now = new Date()
		UpdateClientAPI.sendUpdate(payment.sourceAddress ,payment.inAmount,destinationAddress,amount,now,rate)


		log4j.info("Payment ${sourceAddress} -> ${destinationAddress} ${amount} ${asset} complete")
	}

	// We don't really use the current block...
	// This is the major thing that needs to be fixed. We shall assume that we have different addresses,
	// so that we can discover...
	//public static int main(String[] args) {
	public PaymentProcessor(loggerA,dbsqlite) {

        //def paymentProcessor = new PaymentProcessor()
        log4j = loggerA
        init(dbsqlite)

        audit()
        log4j.info("Payment processor started")
        log4j.info("Last processed payment: " + getLastPaymentBlock())

    }



		// Begin following blocks

		// TODO verify which amounts are satoshi and standardize!!!
		//while (true) {
    public work() {
		    def now = new Date()
		    if (now.time < saleStart.time)
				{
					log4j.info("Sale start at ${saleStart} ")
					return
				}
		if (now.time >saleEnd.time)
		{
			log4j.info("Sale ended ! ")
			return

		}
			def blockHeight = bitcoinAPI.getBlockHeight()
			def lastPaymentBlock =  getLastPaymentBlock()
			def Payment payment =  getNextPayment()



			currentBTCValueInUSD = bitcoinRateAPI.getAveragedRate()
			log4j.info("Updated exchange rate is: ${currentBTCValueInUSD} USD for 1 BTC")



			assert lastPaymentBlock <= blockHeight


			// This will make sure rate is updated even if no payment is performed
			def currentRate =  computeExchangeRate()

			 log4j.info("Block ${blockHeight} rate is: ${currentRate}")

			if (payment != null) {
				def relevantAsset =  findAssetConfig(payment)

				// We only allow buy transactions in this crowdsale machine
				assert payment.inAssetType == Asset.NATIVE_TYPE

                log4j.info("--------------BUY TRANSACTION-------------")
				def baseRate = 	 getBaseExchangeRate(relevantAsset)
				def zoozAmount = (1.0 / (baseRate * currentRate))*(payment.inAmount -  getFee(relevantAsset))
				zoozAmount = Math.ceil(zoozAmount/satoshi)

				 pay(blockHeight, payment, zoozAmount,currentRate)
				 log4j.info("Payment complete")
			}
			else {
				 /*log4j.info("No payments to make. Sleeping..${sleepIntervalms}.")*/
                log4j.info("No payments to make.")

			}
			//sleep(sleepIntervalms)
	}

	// The amount of 1 BTC in USD
	private getCurrentBTCBalue() {
		return currentBTCValueInUSD
	}

	// Fee is in satoshhis
	private getFee(asset) {
		return asset.txFee * satoshi
	}

	// The pegged value in USD of the zooz
	private getValueInUSD(asset) {
		return asset.valueInUSD
	}

	// The amount of BTC each zooz is currently worth
	private getBaseExchangeRate(asset) {
		return 1.0 * asset.valueInUSD / getCurrentBTCBalue()
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

	private getRate(priceSteps) {
		return startRate*Math.exp(priceSteps*priceSteps*1.0/(76*76))
	}

	// crowdsale (saleDay integer, dateString string, sold integer, prevsold integer, jumped integer, steps integer, rate real)
	// The current factor
	private computeExchangeRate() {
		def now = new Date()
		def numSteps = getStepFromDate(now)
		def searchDate = getDateFromStep(numSteps)
		def parsed = plainFormatter.format(searchDate)

		// Select from database to see if it's there
		def cur = getRecord(parsed)
		if (cur == null) {
			// if not - create new record and update
			def priceSteps = 0
			def prevSold = 0
			if (numSteps > 0) {
				def prevString = plainFormatter.format(getDateFromStep(numSteps-1))
				def prev = getRecord(prevString)
				if (prev != null)
				{
				 if (numSteps <= maxSteps) {
				 	priceSteps = prev.steps + 1
				 } else {
				 	priceSteps = prev.steps
				 }
				 prevSold = prev.sold
				}
			}
			def newRate = getRate(priceSteps)
			try {
				 log4j.info("insert into crowdsale values(${numSteps},${parsed},0,${prevSold},0,${priceSteps},${newRate})")
				db.execute("insert into crowdsale values(${numSteps},${parsed},0,${prevSold},0,${priceSteps},${newRate})")
			} catch (Exception e) {
				 log4j.info(e)
				assert false
			} finally {
				return newRate
			}
		} else {
			// if passed yesteday's sales just now, update rate and return
			if (numSteps > 0 && numSteps <= maxSteps && cur.jumped == 0 && cur.sold > cur.prevsold ) {
				def steps = cur.steps + 1
				def rate = getRate(steps)
				try {
					 log4j.info("update crowdsale set jumped=1, steps=${steps}, rate=${rate} where dateString=${parsed}")
					db.execute("update crowdsale set jumped=1, steps=${steps}, rate=${rate} where dateString=${parsed}")
				} catch (Exception e) {
					 log4j.info(e)
					assert false
				} finally {
					return rate
				}
			} else {
				return cur.rate
			}
		}
	}

	private getCLRecord(address) {

		def row = db.firstRow("select * from crowdsalelist where destination = ${address}")
		return row
	}

	private updateSold(sold,destinationAddress,sourceAddress) {
		def now = new Date()
		def numSteps = getStepFromDate(now)
		def searchDate = getDateFromStep(numSteps)
		def parsed = plainFormatter.format(searchDate)

		// Select from database to see if it's there
		def cur = getRecord(parsed)
		assert cur != null
		try {
            log4j.info("update crowdsale set sold=${cur.sold + sold} where dateString=${parsed}")
            db.execute("update crowdsale set sold=${cur.sold + sold} where dateString=${parsed}")
        } catch (Exception e) {
			 log4j.info(e)
			assert false
		}
		//cur = getCLRecord(destinationAddress)
		try {

				log4j.info("insert into crowdsalelist values(${sold},${destinationAddress},${sourceAddress},${now})")
				db.execute("insert into crowdsalelist values(${sold},${destinationAddress},${sourceAddress},${now})")

		/*
			else
			{
				log4j.info("update crowdsalelist set sold=${cur.sold + sold},dateString=${parsed} where  destination = ${destinationAddress}")
				db.execute("update crowdsalelist set sold=${cur.sold + sold},dateString=${parsed} where  destination = ${destinationAddress}")
			}
			*/
		} catch (Exception e) {
			log4j.info(e)
			assert false
		}

	}
	
}
