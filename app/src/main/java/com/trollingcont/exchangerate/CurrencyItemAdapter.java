package com.trollingcont.exchangerate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CurrencyItemAdapter extends RecyclerView.Adapter<CurrencyItemAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<CurrencyItem> currencies;

    CurrencyItemAdapter(Context context, List<CurrencyItem> currencies) {
        this.currencies = currencies;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public CurrencyItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.currency_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CurrencyItemAdapter.ViewHolder holder, int position) {
        CurrencyItem currencyItem = currencies.get(position);
        holder.itemText.setText(currencyItem.getText());
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView itemText;

        ViewHolder(View view){
            super(view);
            itemText = view.findViewById(R.id.currency_item_text);
        }
    }
}
