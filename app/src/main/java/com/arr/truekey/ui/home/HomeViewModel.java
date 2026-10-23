package com.arr.truekey.ui.home;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.arr.truekey.utils.room.AppDatabase;
import com.arr.truekey.utils.room.entity.Account;
import com.arr.truekey.utils.security.OtpHelper;
import com.arr.truekey.utils.security.SecurityUtils;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final LiveData<List<Account>> accounts;
    private final MutableLiveData<Integer> secondsRemaining = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker =
            new Runnable() {
                @Override
                public void run() {
                    secondsRemaining.setValue(OtpHelper.secondsRemaining());
                    handler.postDelayed(this, 1000);
                }
            };

    public HomeViewModel(@NonNull Application application) {
        super(application);
        char[] passphrase = SecurityUtils.getOrCreatePassphrase(application);
        AppDatabase db = AppDatabase.getInstance(application, passphrase);
        accounts = db.accountDao().getAllLive();
    }

    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }

    public LiveData<Integer> getSecondsRemaining() {
        return secondsRemaining;
    }

    public void startTicking() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    public void stopTicking() {
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopTicking();
    }
}
