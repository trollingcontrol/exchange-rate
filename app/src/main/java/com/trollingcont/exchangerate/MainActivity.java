package com.trollingcont.exchangerate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Debug;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    String url = "https://www.cbr-xml-daily.ru/daily_json.js";

    TextView currentDateView;
    TextView previousDateView;

    SimpleDateFormat inputDateFormat;
    SimpleDateFormat outputDateFormat;

    RequestQueue queue;

    RecyclerView currenciesListView;
    ArrayList<CurrencyItem> currenciesList = new ArrayList<CurrencyItem>();
    CurrencyItemAdapter adapter;

    Button refreshButton;

    Button selectCurrencyButton;

    EditText rublesAmountToConvertEdit;
    EditText destCurrencyAmountEdit;

    JSONObject jsonResponse = null;
    float selectedCurrencyRate;

    PopupMenu selectCurrencyMenu;
    Menu popupMenuView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentDateView = findViewById(R.id.current_date);
        previousDateView = findViewById(R.id.previous_date);
        currenciesListView = findViewById(R.id.currencies_list);

        adapter = new CurrencyItemAdapter(this, currenciesList);
        currenciesListView.setAdapter(adapter);

        destCurrencyAmountEdit = findViewById(R.id.dest_currency_amount_edit);
        rublesAmountToConvertEdit = findViewById(R.id.rubles_amount_edit);

        rublesAmountToConvertEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertRublesToSelectedCurrency(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        refreshButton = findViewById(R.id.refresh_button);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rublesAmountToConvertEdit.setText("");
                destCurrencyAmountEdit.setText("");
                currenciesList.clear();
                popupMenuView.clear();
                makeRequest();
            }
        });

        selectCurrencyButton = findViewById(R.id.select_currency_button);

        selectCurrencyMenu = new PopupMenu(MainActivity.this, selectCurrencyButton);
        popupMenuView = selectCurrencyMenu.getMenu();

        selectCurrencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCurrencyMenu.show();
            }
        });

        selectCurrencyMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectCurrencyToConvert((String)item.getTitle());
                convertRublesToSelectedCurrency(rublesAmountToConvertEdit.getText().toString());
                return true;
            }
        });

        inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        outputDateFormat = new SimpleDateFormat("dd.MM.yyyy', 'HH:mm:ss' 'XXX");

        queue = Volley.newRequestQueue(this);

        if (savedInstanceState != null) {
            try {
                jsonResponse = new JSONObject(savedInstanceState.getString("jsonResponse"));
                selectCurrencyToConvert(savedInstanceState.getString("selectedCurrencyCode"));
                showExchangeRates(jsonResponse);
            }
            catch (JSONException jsonExc) {
                currentDateView.setText(jsonExc.toString());
            }
            catch (NullPointerException nullPtr) {}
        }
        else {
            makeRequest();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (jsonResponse != null) {
            outState.putString("jsonResponse", jsonResponse.toString());
            outState.putString("selectedCurrencyCode", (String) selectCurrencyButton.getText());
        }

        super.onSaveInstanceState(outState);
    }

    void showExchangeRates(JSONObject exchangeRates) {
        try {
            String currentDateStr = exchangeRates.getString("Date");
            Date currentDate = inputDateFormat.parse(currentDateStr);
            currentDateView.setText(getString(R.string.current_date_format, outputDateFormat.format(currentDate)));

            String previousDateStr = exchangeRates.getString("PreviousDate");
            Date previousDate = inputDateFormat.parse(previousDateStr);
            previousDateView.setText(getString(R.string.previous_date_format, outputDateFormat.format(previousDate)));

            JSONObject jsonCurrenciesList = exchangeRates.getJSONObject("Valute");
            JSONArray currencyCodes = jsonCurrenciesList.names();

            for (int i = 0; i < currencyCodes.length(); ++i) {
                String currencyCharCode = currencyCodes.getString(i);

                popupMenuView.add(currencyCharCode);

                JSONObject currencyInfo = jsonCurrenciesList.getJSONObject(currencyCharCode);
                int currencyNominal = currencyInfo.getInt("Nominal");

                currenciesList.add(new CurrencyItem(getString(R.string.currency_item_template,
                        currencyInfo.getString("Name"),
                        currencyCharCode,
                        currencyInfo.getDouble("Value") / currencyNominal,
                        currencyInfo.getDouble("Previous") / currencyNominal
                )));

                adapter.notifyItemChanged(i);
            }

        }
        catch (JSONException jsonExc) {
            currentDateView.setText("Can't find Date field");
        }
        catch (ParseException prsExc) {
            currentDateView.setText("Parse exception");
        }
    }

    void makeRequest() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                jsonResponse = response;
                showExchangeRates(response);
                selectCurrencyToConvert("USD");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                currentDateView.setText(error.getMessage());
            }
        });

        queue.add(jsonObjectRequest);
    }

    void selectCurrencyToConvert(String currencyCharCode) {
        try {
            JSONObject selectedCurrencyData = jsonResponse.getJSONObject("Valute").getJSONObject(currencyCharCode);

            selectCurrencyButton.setText(currencyCharCode);
            selectedCurrencyRate = (float)selectedCurrencyData.getDouble("Value") / selectedCurrencyData.getInt("Nominal");
        }
        catch (JSONException jsonExc) {

        }
    }

    void convertRublesToSelectedCurrency(String rublesAmountStr) {
        if (selectedCurrencyRate == 0.0) return;

        try {
            float rublesAmount = Float.parseFloat(rublesAmountStr);

            if (rublesAmount > 0) {
                destCurrencyAmountEdit.setText(String.format("%f", rublesAmount / selectedCurrencyRate));
            }
            else {
                destCurrencyAmountEdit.setText("");
            }
        }
        catch (NumberFormatException numFormatExc) {
            destCurrencyAmountEdit.setText("");
        }
    }
}