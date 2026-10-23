package com.arr.truekey.ui.details;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.arr.truekey.utils.room.AppDatabase;
import com.arr.truekey.utils.room.entity.Account;
import com.arr.truekey.utils.security.OtpHelper;
import com.arr.truekey.utils.security.SecurityUtils;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OtpDetailViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final LiveData<Account> account;
    private final MutableLiveData<String> code = new MutableLiveData<>();
    private final MutableLiveData<Integer> secondsRemaining = new MutableLiveData<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Runnable tickRunnable;
    private String currentSecret;

    private final Observer<Account> accountObserver = this::onAccountChanged;

    public OtpDetailViewModel(@NonNull Application application, int accountId) {
        super(application);
        char[] passphrase = SecurityUtils.getOrCreatePassphrase(application);
        db = AppDatabase.getInstance(application, passphrase);

        account = db.accountDao().getByIdLive(accountId);
        account.observeForever(accountObserver);
    }

    public LiveData<Account> getAccount() {
        return account;
    }

    public LiveData<String> getCode() {
        return code;
    }

    public LiveData<Integer> getSecondsRemaining() {
        return secondsRemaining;
    }

    private void onAccountChanged(Account acc) {
        if (acc == null) return;
        String secret = acc.otpSecret == null ? "" : acc.otpSecret.trim();
        if (secret.equals(currentSecret)) return;

        currentSecret = secret;
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        if (!secret.isEmpty()) startTicking();
    }

    private void startTicking() {
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        tickRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String raw = OtpHelper.generateCode(currentSecret);
                            code.setValue(formatGrouped(raw));
                        } catch (CodeGenerationException e) {
                            code.setValue("------");
                        }
                        secondsRemaining.setValue(OtpHelper.secondsRemaining());
                        handler.postDelayed(this, 1000);
                    }
                };
        handler.post(tickRunnable);
    }

    /** Convierte "123456" en "123 456". Funciona con 6 u 8 dígitos. */
    private String formatGrouped(String raw) {
        int mid = raw.length() / 2;
        return raw.substring(0, mid) + " • " + raw.substring(mid);
    }

    public void deleteAccount(Account acc, Runnable onDeleted) {
        executor.execute(
                () -> {
                    db.accountDao().delete(acc);
                    handler.post(onDeleted);
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        account.removeObserver(accountObserver);
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
    }
}
