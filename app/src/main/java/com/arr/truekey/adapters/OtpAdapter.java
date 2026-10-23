package com.arr.truekey.adapters;

import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.arr.truekey.databinding.ItemOtpBinding;
import com.arr.truekey.utils.ClipboardUtils;
import com.arr.truekey.utils.room.entity.Account;
import com.arr.truekey.utils.security.OtpHelper;
import java.util.HashMap;
import java.util.List;
import com.arr.truekey.R;
import java.util.Map;

public class OtpAdapter extends RecyclerView.Adapter<OtpAdapter.OtpViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    private final List<Account> accounts;
    private final OnAccountClickListener listener;
    private static final Object PAYLOAD_TICK = new Object();

    private int secondsRemaining = OtpHelper.secondsRemaining();
    private final Map<Integer, String> codeCache = new HashMap<>();
    private long cachedWindow = -1;

    private static final int[] AVATAR_COLORS = {
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB,
        0xFF1E88E5, 0xFF00897B, 0xFF43A047, 0xFFF4511E, 0xFFFB8C00
    };

    public OtpAdapter(List<Account> accounts, OnAccountClickListener listener) {
        this.accounts = accounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OtpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOtpBinding binding =
                ItemOtpBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new OtpViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull OtpViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            bindOtp(holder, accounts.get(position)); // solo código + progreso
        } else {
            super.onBindViewHolder(holder, position, payloads); // llama al bind completo
        }
    }

    @Override
    public void onBindViewHolder(@NonNull OtpViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.binding.tvName.setText(account.label);
        holder.binding.tvAlias.setText(account.username);

        String label = account.label == null ? "" : account.label.trim();
        String initial = label.isEmpty() ? "?" : label.substring(0, 1).toUpperCase();
        int strongColor =
                AVATAR_COLORS[Math.abs(label.toLowerCase().hashCode() % AVATAR_COLORS.length)];
        int softColor = ColorUtils.setAlphaComponent(strongColor, 40); // ~16% de opacidad

        holder.binding.tvInitial.setText(initial);
        holder.binding.tvInitial.setTextColor(strongColor);
        holder.binding.tvInitial.setBackgroundTintList(ColorStateList.valueOf(softColor));

        holder.binding.itemRoot.setOnClickListener(v -> listener.onAccountClick(account));
        holder.binding.tvOtp.setOnClickListener(
                v ->
                        ClipboardUtils.copyOtp(
                                v.getContext(), holder.binding.tvOtp.getText().toString()));

        bindOtp(holder, account);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    private void bindOtp(OtpViewHolder holder, Account account) {
        boolean hasOtp = account.otpSecret != null && !account.otpSecret.trim().isEmpty();
        holder.binding.tvOtp.setVisibility(hasOtp ? View.VISIBLE : View.GONE);
        holder.binding.progressTimer.setVisibility(hasOtp ? View.VISIBLE : View.GONE);
        if (!hasOtp) return;

        holder.binding.tvOtp.setText(getCode(account));
        holder.binding.progressTimer.setMax(30);
        holder.binding.progressTimer.setProgressCompat(secondsRemaining, true);
    }

    private String getCode(Account account) {
        String cached = codeCache.get(account.id);
        if (cached != null) return cached;

        String formatted;
        try {
            String raw = OtpHelper.generateCode(account.otpSecret.trim());
            int mid = raw.length() / 2;
            formatted = raw.substring(0, mid) + " • " + raw.substring(mid);
        } catch (Exception e) {
            formatted = "------";
        }
        codeCache.put(account.id, formatted);
        return formatted;
    }

    public void updateTick(int seconds) {
        secondsRemaining = seconds;
        long window = System.currentTimeMillis() / 1000L / 30L;
        if (window != cachedWindow) {
            codeCache.clear();
            cachedWindow = window;
        }
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_TICK);
    }


    public void updateData(List<Account> newAccounts) {
        accounts.clear();
        accounts.addAll(newAccounts);
        codeCache.clear();
        notifyDataSetChanged();
    }

    static class OtpViewHolder extends RecyclerView.ViewHolder {
        final ItemOtpBinding binding;

        OtpViewHolder(@NonNull ItemOtpBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

/*


private static final Object PAYLOAD_TICK = new Object();

private int secondsRemaining = OtpHelper.secondsRemaining();
private final Map<Integer, String> codeCache = new HashMap<>();
private long cachedWindow = -1;

public void updateTick(int seconds) {
    secondsRemaining = seconds;
    long window = System.currentTimeMillis() / 1000L / 30L;
    if (window != cachedWindow) {      // cambió la ventana de 30s: códigos nuevos
        codeCache.clear();
        cachedWindow = window;
    }
    notifyItemRangeChanged(0, getItemCount(), PAYLOAD_TICK);
}

public void updateData(List<Account> newAccounts) {
    accounts.clear();
    accounts.addAll(newAccounts);
    codeCache.clear();                 // por si se editó un secreto
    notifyDataSetChanged();
}

@Override
public void onBindViewHolder(@NonNull OtpViewHolder holder, int position,
                             @NonNull List<Object> payloads) {
    if (!payloads.isEmpty()) {
        bindOtp(holder, accounts.get(position));   // solo código + progreso
    } else {
        super.onBindViewHolder(holder, position, payloads); // llama al bind completo
    }
}

@Override
public void onBindViewHolder(@NonNull OtpViewHolder holder, int position) {
    Account account = accounts.get(position);
    // ... tu código actual: name, alias, avatar, click ...
    bindOtp(holder, account);
}

private void bindOtp(OtpViewHolder holder, Account account) {
    boolean hasOtp = account.otpSecret != null && !account.otpSecret.trim().isEmpty();
    holder.binding.layoutOtp.setVisibility(hasOtp ? View.VISIBLE : View.GONE);
    if (!hasOtp) return;

    holder.binding.tvCode.setText(getCode(account));
    holder.binding.progressTimer.setProgressCompat(secondsRemaining, true);
}

private String getCode(Account account) {
    String cached = codeCache.get(account.id);
    if (cached != null) return cached;

    String formatted;
    try {
        String raw = OtpHelper.generateCode(account.otpSecret.trim());
        int mid = raw.length() / 2;
        formatted = raw.substring(0, mid) + " " + raw.substring(mid);
    } catch (Exception e) {
        formatted = "------";
    }
    codeCache.put(account.id, formatted);
    return formatted;
}

*/
