package com.arr.truekey.utils.room.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {
    
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String label;
    public String username;
    public String password;
    public String otpSecret;
}
