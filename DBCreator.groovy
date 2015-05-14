/**
 * Created by amirza on 19/07/14.
 */

public class DBCreator {
	public static createDB(db) { 
	
	db.execute("create table if not exists blocks (blockId integer, status string, duration integer)")		        
        db.execute("create table if not exists fees(blockId string, txid string, feeAsset string, feeAmount integer)")
        db.execute("create table if not exists payments(blockId integer, sourceTxid string, sourceAddress string, inAssetType string, inAmount integer, destinationAddress string, outAsset string, outAssetType string, outAmount integer, status string, lastUpdatedBlockId integer)")
        
        db.execute("create table if not exists crowdsale (saleDay integer, dateString string, sold integer,prevsold integer, jumped integer, steps integer, rate real)")

        db.execute("create table if not exists crowdsalelist (amount integer,destination string ,dateString string)")

        db.execute("create unique index if not exists blocks1 on blocks(blockId)")       

        db.execute("create index if not exists fees1 on fees(blockId, txid)")
        db.execute("create index if not exists payments1 on payments(blockId)")
        db.execute("create index if not exists payments1 on payments(sourceTxid)")
		
	def row
	// Check vital tables exist
        row = db.firstRow("select name from sqlite_master where type='table' and name='blocks'")
        assert row != null
        row = db.firstRow("select name from sqlite_master where type='table' and name='fees'")
       assert row != null	
	row = db.firstRow("select name from sqlite_master where type='table' and name='payments'")
        assert row != null
        row = db.firstRow("select name from sqlite_master where type='table' and name='crowdsale'")
        assert row != null
        row = db.firstRow("select name from sqlite_master where type='table' and name='crowdsalelist'")
        assert row != null
	}
}
