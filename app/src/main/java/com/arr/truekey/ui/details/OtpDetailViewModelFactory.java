package com.arr.truekey.ui.details;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class OtpDetailViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final int accountId;

    public OtpDetailViewModelFactory(Application application, int accountId) {
        this.application = application;
        this.accountId = accountId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new OtpDetailViewModel(application, accountId);
    }
}
