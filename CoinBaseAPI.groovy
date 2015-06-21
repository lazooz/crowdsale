import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import groovyx.net.http.AsyncHTTPBuilder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

/**
 * Created by whoisjeremylam on 18/04/14.
 */
class CoinBaseAPI {

	private String API_KEY = "opiWx00NehnIDhgL"
	private String API_SECRET = "IUO2ORVPHczXQcI4QAyXHGyLgz0QGdgf"
	private String CoinBaseUrl = "https://api.coinbase.com/v1/addresses?limit=100"


	/**
	 * @param secretKey
	 * @param data
	 * @return HMAC/SHA256 representation of the given string
	 */
	def hmac_sha256(String secretKey, String data) {
		try {  SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
			Mac mac = Mac.getInstance("HmacSHA256")
			mac.init(secretKeySpec)
			byte[] digest = mac.doFinal(data.getBytes("UTF-8"))
			return byteArrayToString(digest)
		} catch (InvalidKeyException e) {  throw new RuntimeException("Invalid key exception while converting to HMac SHA256")
		}
	}

	private def byteArrayToString(byte[] data) {
		BigInteger bigInteger = new BigInteger(1, data)
		String hash = bigInteger.toString(16)
		//Zero pad it
		while (hash.length() < 64) {
			hash = "0" + hash
		}
		return hash
	}



	private getQueryResult(httpAsync,Uri) {
        def body =null
		def url = Uri
		def now = new Date()
		def nonce = now.time + 1432106396501116
		//def nonce = System.currentTimeMillis();
		def message = nonce.toString() + url + (body != null ? body : "")
		def signature = hmac_sha256(API_SECRET, message)
		def result = httpAsync.request( GET, JSON) { req ->

			headers.'ACCESS_KEY' = API_KEY
			headers.'ACCESS_SIGNATURE' = signature
			headers.'ACCESS_NONCE' = nonce
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

	private init(String iniFile) {

	}
	public getCoinBaseAddresses()
	{
		def Uri = CoinBaseUrl
		def httpAsync = new AsyncHTTPBuilder(
				poolSize : 10,
				uri : Uri,
				contentType : JSON ,
				)

		def result = getQueryResult(httpAsync,Uri)
		def Addressess = []
		if (result == null) {
			Addressess.push("1DomPnN69cJBiohjsH8GRUXQzjUGKySFQN")
			Addressess.push("16JQy5LEZayWucxg1RaAqaRp5Y5F2K5SwK")

			return Addressess;
		}
		def num_pages = result["num_pages"];
		for (def i = 0; i < result["addresses"].size(); i++)
		{
			def addressentry = result["addresses"][i]["address"]["address"]
			Addressess.push(addressentry)
		}
		for (def j=2;j <=num_pages;j++)
		{
			Uri ="${CoinBaseUrl}&page=${j}"
			println (Uri)
			httpAsync = new AsyncHTTPBuilder(
					poolSize : 10,
					uri:Uri,
					contentType : JSON ,
			)
			result = getQueryResult(httpAsync,Uri)
			if (result == null)
				return Addressess;
			for (def i = 0; i < result["addresses"].size(); i++)
			{
				def addressentry = result["addresses"][i]["address"]["address"]
				Addressess.push(addressentry)
			}
		}
        println(Addressess.size())
		return Addressess
	}


	public CoinBaseAPI() {
		init()
	}

	public static int main(String[] args) {
		def coinbaseAPI = new CoinBaseAPI()
		def result = coinbaseAPI.getCoinBaseAddresses()

		println(result)

		return 0
	}
}
