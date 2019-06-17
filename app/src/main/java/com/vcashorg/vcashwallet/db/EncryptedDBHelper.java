package com.vcashorg.vcashwallet.db;

import android.content.Context;

import com.tencent.wcdb.Cursor;
import com.tencent.wcdb.SQLException;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteOpenHelper;
import com.tencent.wcdb.repair.RepairKit;
import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashContext;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashWalletInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletNoParamCallBack;

import java.io.File;
import java.util.ArrayList;

public class EncryptedDBHelper extends SQLiteOpenHelper {
    public static final String PASSPHRASE = "vcash_wallet";

    private static final String DATABASE_NAME = "vcash_wallet_encrypted.db";
    private static final int DATABASE_VERSION = 1;

    static private Context mContext;
    private String mPassphrase;
    private ArrayList<WalletNoParamCallBack> txDataListener = new ArrayList<>();

    private static volatile EncryptedDBHelper sInstance;

    public static void setDbContext(Context con){
        mContext = con;
    }

    public static EncryptedDBHelper getsInstance() {
        if (sInstance == null) {
            synchronized (EncryptedDBHelper.class) {
                if (sInstance == null) {
                    sInstance = new EncryptedDBHelper(PASSPHRASE);
                }
            }
        }
        return sInstance;
    }

    public void clearAllData(){
        File dbFile = mContext.getDatabasePath(DATABASE_NAME);
        if (dbFile.exists()){
            dbFile.delete();
        }
        sInstance = null;
    }

    public boolean saveOutputData(ArrayList<VcashOutput> arr){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isSuc = true;
        try {
            db.delete("VcashOutput", null, null);
            for (VcashOutput item : arr) {
                db.execSQL("REPLACE INTO VcashOutput (" +
                        "commitment, keypath, mmr_index, value, height, lock_height, is_coinbase, status, tx_log_id)" +
                        "values(" +
                        "'" + item.commitment + "'," +
                        "'" + item.keyPath + "'," +
                        item.mmr_index + "," +
                        item.value + "," +
                        item.height + "," +
                        item.lock_height + "," +
                        (item.is_coinbase?1:0) + "," +
                        item.status.ordinal() + "," +
                        item.tx_log_id +
                        ")");
            }
        } catch (SQLException e){
            isSuc = false;
            e.printStackTrace();
        } finally {
        }

        return isSuc;
    }

    public ArrayList<VcashOutput> getActiveOutputData(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<VcashOutput> arr = null;
        Cursor cursor = db.rawQuery("SELECT * FROM VcashOutput WHERE status != ?", new String[] {Integer.toString(VcashOutput.OutputStatus.Spent.ordinal())});
        if (cursor != null && cursor.moveToFirst()) {
            arr = new ArrayList<VcashOutput>();
            do {
                VcashOutput item = new VcashOutput();
                item.commitment = cursor.getString(cursor.getColumnIndex("commitment"));
                item.keyPath = cursor.getString(cursor.getColumnIndex("keypath"));
                item.mmr_index = cursor.getLong(cursor.getColumnIndex("mmr_index"));
                item.value = cursor.getLong(cursor.getColumnIndex("value"));
                item.height = cursor.getLong(cursor.getColumnIndex("height"));
                item.lock_height = cursor.getLong(cursor.getColumnIndex("lock_height"));
                item.is_coinbase = (cursor.getInt(cursor.getColumnIndex("is_coinbase")) == 1);
                item.status = VcashOutput.OutputStatus.values()[cursor.getInt(cursor.getColumnIndex("status"))];
                item.tx_log_id = cursor.getShort(cursor.getColumnIndex("tx_log_id"));
                if (getActiveTxByTxId(item.tx_log_id) != null){
                    arr.add(item);
                }
            } while (cursor.moveToNext());
        }
        return arr;
    }

    public boolean saveTxDataArr(ArrayList<VcashTxLog> arr){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isSuc = true;
        try {
            for (VcashTxLog item : arr) {
                saveTx_imp(item, db);
            }
        } catch (SQLException e){
            isSuc = false;
            e.printStackTrace();
        } finally {
        }

        if (isSuc){
            notifyTxDataListener();
        }

        return isSuc;
    }

    public boolean saveTx(VcashTxLog txLog){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isSuc = true;
        try {
            saveTx_imp(txLog, db);
        }catch (SQLException e){
            isSuc = false;
            e.printStackTrace();
        }

        if (isSuc){
            notifyTxDataListener();
        }

        return isSuc;
    }

    private boolean saveTx_imp(VcashTxLog txLog, SQLiteDatabase db){
        StringBuilder inputStrBuilder = new StringBuilder();
        if (txLog.inputs != null){
            for (String str :txLog.inputs){
                inputStrBuilder.append(str);
            }
        }

        StringBuilder outputStrBuilder = new StringBuilder();
        if (txLog.outputs != null){
            for (String str :txLog.outputs){
                outputStrBuilder.append(str);
            }
        }

        db.execSQL("REPLACE INTO VcashTxLog (" +
                "tx_id, tx_slate_id, parter_id, tx_type, create_time, confirm_time, confirm_state, server_status, amount_credited, amount_debited, fee, inputs, outputs)" +
                "values(" +
                txLog.tx_id + "," +
                "'" + txLog.tx_slate_id + "'," +
                "'" + txLog.parter_id + "'," +
                txLog.tx_type.ordinal() + "," +
                txLog.create_time + "," +
                txLog.confirm_time + "," +
                txLog.confirm_state.ordinal() + "," +
                txLog.server_status.ordinal() + "," +
                txLog.amount_credited + "," +
                txLog.amount_debited + "," +
                txLog.fee + "," +
                "'" + inputStrBuilder.toString() + "'," +
                "'" + outputStrBuilder.toString() + "'" +
                ")");
        return true;
    }

    public ArrayList<VcashTxLog> getTxData(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<VcashTxLog> arr = null;
        Cursor cursor = db.rawQuery("SELECT * FROM VcashTxLog ORDER BY tx_id ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            arr = new ArrayList<VcashTxLog>();
            do {
                arr.add(parseTxLog(cursor));
            } while (cursor.moveToNext());
        }
        return arr;
    }

    public VcashTxLog getTxBySlateId(String slate_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM VcashTxLog WHERE tx_slate_id = ?", new String[] {slate_id});
        if (cursor != null && cursor.moveToFirst()){

            return parseTxLog(cursor);
        }
        return null;
    }

    public VcashTxLog getActiveTxByTxId(int tx_id){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = String.format("SELECT * FROM VcashTxLog WHERE tx_id = %s AND server_status <> 3", tx_id);
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()){

            return parseTxLog(cursor);
        }
        return null;
    }

    public boolean deleteTxBySlateId(String slate_id){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isSuc = true;
        try {
            db.delete("VcashTxLog", "tx_slate_id = ?", new String[] {slate_id});
        }catch (SQLException e){
            isSuc = false;
            e.printStackTrace();
        }

        return isSuc;
    }

    private VcashTxLog parseTxLog(Cursor cursor){
        if (cursor != null){
            VcashTxLog item = new VcashTxLog();
            item.tx_id = cursor.getShort(cursor.getColumnIndex("tx_id"));
            item.tx_slate_id = cursor.getString(cursor.getColumnIndex("tx_slate_id"));
            item.parter_id = cursor.getString(cursor.getColumnIndex("parter_id"));
            item.tx_type = VcashTxLog.TxLogEntryType.values()[cursor.getInt(cursor.getColumnIndex("tx_type"))];
            item.create_time = cursor.getLong(cursor.getColumnIndex("create_time"));
            item.confirm_time = cursor.getLong(cursor.getColumnIndex("confirm_time"));
            item.confirm_state = VcashTxLog.TxLogConfirmType.values()[cursor.getInt(cursor.getColumnIndex("confirm_state"))];
            item.server_status = ServerTxStatus.values()[cursor.getInt(cursor.getColumnIndex("server_status"))];
            item.amount_credited = cursor.getLong(cursor.getColumnIndex("amount_credited"));
            item.amount_debited = cursor.getLong(cursor.getColumnIndex("amount_debited"));
            item.fee = cursor.getLong(cursor.getColumnIndex("fee"));
            String inputStr = cursor.getString(cursor.getColumnIndex("inputs"));
            for (int startIndex=0; startIndex<inputStr.length(); startIndex+=66){
                item.appendInput(inputStr.substring(startIndex, startIndex+66));
            }
            String outputStr = cursor.getString(cursor.getColumnIndex("outputs"));
            for (int startIndex=0; startIndex<outputStr.length(); startIndex+=66){
                item.appendOutput(outputStr.substring(startIndex, startIndex+66));
            }
            return item;
        }

        return null;
    }

    public boolean saveWalletInfo(VcashWalletInfo info){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isSuc = true;
        try {
            db.execSQL("REPLACE INTO VcashWalletInfo (" +
                    "curKeyPath, curHeight, curTxLogId, infoid)" +
                    "values(" +
                    "'" + info.curKeyPath + "'," +
                    info.curHeight + "," +
                    info.curTxLogId + "," +
                    info.infoid +
                    ")");
        }catch (SQLException e){
            isSuc = false;
            e.printStackTrace();
        }

        return isSuc;
    }

    public VcashWalletInfo loadWalletInfo(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM VcashWalletInfo LIMIT 1", null);
        if (cursor != null && cursor.moveToFirst()){
            VcashWalletInfo info = new VcashWalletInfo();
            info.curKeyPath = cursor.getString(cursor.getColumnIndex("curKeyPath"));
            info.curHeight = cursor.getLong(cursor.getColumnIndex("curHeight"));
            info.curTxLogId = cursor.getShort(cursor.getColumnIndex("curTxLogId"));
            info.infoid = cursor.getShort(cursor.getColumnIndex("infoid"));
            return info;
        }
        return null;
    }

    public boolean saveContext(VcashContext context){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isSuc = true;
        try {
            db.execSQL("REPLACE INTO VcashContext (" +
                    "slate_id, sec_key, sec_nounce)" +
                    "values(" +
                    "'" + context.slate_id + "'," +
                    "'" + context.sec_key + "'," +
                    "'" + context.sec_nounce + "'" +
                    ")");
        }catch (SQLException e){
            isSuc = false;
            e.printStackTrace();
        }

        return isSuc;
    }

    public VcashContext getContextBySlateId(String slateid){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM VcashContext WHERE slate_id = ?", new String[] {slateid});
        if (cursor != null && cursor.moveToFirst()){
            VcashContext context = new VcashContext();
            context.sec_key = cursor.getString(cursor.getColumnIndex("sec_key"));
            context.sec_nounce = cursor.getString(cursor.getColumnIndex("sec_nounce"));
            context.slate_id = cursor.getString(cursor.getColumnIndex("slate_id"));
            return context;
        }
        return null;
    }

    public boolean beginDatabaseTransaction(){
        this.getReadableDatabase().beginTransaction();
        return true;
    }

    public boolean commitDatabaseTransaction(){
        this.getReadableDatabase().setTransactionSuccessful();
        this.getReadableDatabase().endTransaction();
        return true;
    }

    public boolean rollbackDataTransaction(){
        this.getReadableDatabase().endTransaction();
        return true;
    }

    public void addTxDataListener(WalletNoParamCallBack listener){
        txDataListener.add(listener);
    }

    private void notifyTxDataListener(){
        for (WalletNoParamCallBack listener:txDataListener){
            listener.onCall();
        }
    }

    private EncryptedDBHelper(String passphrase) {
        // Call "encrypted" version of the superclass constructor.
        super(mContext, DATABASE_NAME, passphrase.getBytes(), null, null, DATABASE_VERSION,
                null);
        mPassphrase = passphrase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE VcashOutput (" +
                "commitment     text primary key NOT NULL," +
                "keypath        text," +
                "mmr_index      integer," +
                "value          integer," +
                "height         integer," +
                "lock_height    integer," +
                "is_coinbase    integer," +
                "status         integer," +
                "tx_log_id      integer)");

        db.execSQL("CREATE TABLE VcashTxLog (" +
                "tx_id              integer primary key," +
                "tx_slate_id        text," +
                "parter_id          text," +
                "tx_type            integer," +
                "create_time        integer," +
                "confirm_time       integer," +
                "confirm_state      integer," +
                "server_status      integer," +
                "amount_credited    integer," +
                "amount_debited     integer," +
                "fee                integer," +
                "inputs             text," +
                "outputs            text)");

        db.execSQL("CREATE TABLE VcashWalletInfo (" +
                "infoid             integer primary key," +
                "curKeyPath         text," +
                "curHeight          integer," +
                "curTxLogId         integer)");

        db.execSQL("CREATE TABLE VcashContext (" +
                "slate_id       text primary key NOT NULL," +
                "sec_nounce     text," +
                "sec_key        text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        // OPTIONAL: backup master info for corruption recovery.
        RepairKit.MasterInfo.save(db, db.getPath() + "-mbak", mPassphrase.getBytes());
    }
}
