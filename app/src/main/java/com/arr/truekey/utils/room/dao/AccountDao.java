package com.arr.truekey.utils.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.arr.truekey.utils.room.entity.Account;
import java.util.List;

@Dao
public interface AccountDao {
    @Insert
    void insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM accounts")
    List<Account> getAll();

    @Query("SELECT * FROM accounts")
    LiveData<List<Account>> getAllLive();

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    Account getById(int accountId);

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    LiveData<Account> getByIdLive(int accountId);
}
