package com.arr.truekey.utils.room;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.arr.truekey.utils.room.dao.AccountDao;
import com.arr.truekey.utils.room.entity.Account;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

@Database(
        entities = {Account.class},
        version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AccountDao accountDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context, char[] passphrase) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    byte[] passphraseBytes = SQLiteDatabase.getBytes(passphrase);

                    SupportFactory factory = new SupportFactory(passphraseBytes);
                    INSTANCE =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            AppDatabase.class,
                                            "auth_db")
                                    .openHelperFactory(factory)
                                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
