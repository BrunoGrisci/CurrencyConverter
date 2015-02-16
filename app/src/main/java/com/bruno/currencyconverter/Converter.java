package com.bruno.currencyconverter;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Converter extends ActionBarActivity {

    EditTextCurrency fromCurrencyText;
    EditTextCurrency toCurrencyText;
    Button swapButton;

    EditText fromCurrencySymbol;
    EditText toCurrencySymbol;

    Spinner fromSpinner;
    Spinner toSpinner;

    TextView lastUpdateLabel;
    TextView lastUpdateText;
    TextView numberCreatedLabel;
    TextView numberCreated;
    TextView numberCreatedLocal;
    TextView numberResumedLabel;
    TextView numberResumed;
    TextView numberResumedLocal;

    ListView lastOperations;
    ArrayList<String> operations = new ArrayList<String>();
    ArrayAdapter operationsAdapter;

    int numCreated;
    int numResumed;

    int numCreatedLocal = 0;
    int numResumedLocal = 0;

    boolean swapedFrom = false;
    boolean swapedTo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converter);

        fromCurrencyText = (EditTextCurrency) findViewById(R.id.fromCurrencyText);
        toCurrencyText = (EditTextCurrency) findViewById(R.id.toCurrencyText);

        fromCurrencySymbol = (EditText) findViewById(R.id.fromCurrencySymbol);
        toCurrencySymbol = (EditText) findViewById(R.id.toCurrencySymbol);

        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.currency_name_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromSpinner = (Spinner) findViewById(R.id.fromSpinner);
        fromSpinner.setAdapter(adapter);

        toSpinner = (Spinner) findViewById(R.id.toSpinner);
        toSpinner.setAdapter(adapter);

        lastUpdateLabel = (TextView) findViewById(R.id.lastUpdateLabel);
        lastUpdateText = (TextView) findViewById(R.id.lastUpdateText);
        numberCreatedLabel = (TextView) findViewById(R.id.numberCreatedLabel);
        numberCreated = (TextView) findViewById(R.id.numberCreated);
        numberCreatedLocal = (TextView) findViewById(R.id.numberCreatedLocal);
        numberResumedLabel = (TextView) findViewById(R.id.numberResumedLabel);
        numberResumed = (TextView) findViewById(R.id.numberResumed);
        numberResumedLocal = (TextView) findViewById(R.id.numberResumedLocal);

        lastOperations = (ListView) findViewById(R.id.lastOperations);

        SharedPreferences savedRates = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        String lastUpdateTime = savedRates.getString(getResources().getString(R.string.lastUpdateTime), getResources().getString(R.string.time));
        lastUpdateText.setText(lastUpdateTime);

        fromCurrencyText.setText(savedRates.getString(getResources().getString(R.string.fromValue), "1.0"));
        fromSpinner.setSelection(adapter.getPosition(savedRates.getString(getResources().getString(R.string.fromCurrency), "GBP")));
        toSpinner.setSelection(adapter.getPosition(savedRates.getString(getResources().getString(R.string.toCurrency), "USD")));

        numCreated = Integer.parseInt(savedRates.getString(getResources().getString(R.string.numberCreated), "0")) + 1;
        numberCreated.setText(String.valueOf(numCreated));
        numberResumed.setText(savedRates.getString(getResources().getString(R.string.numberResumed), "0"));

        numCreatedLocal = numCreatedLocal + 1;
        numberCreatedLocal.setText(" (" + String.valueOf(numCreatedLocal) + ")");
        numberResumedLocal.setText(" (" + String.valueOf(numResumedLocal) + ")");

        operationsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, operations);
        lastOperations.setAdapter(operationsAdapter);

        SharedPreferences savedOperations = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        operations.clear();
        operations.add(0, savedOperations.getString(getResources().getString(R.string.lastOparation0), ""));
        operations.add(1, savedOperations.getString(getResources().getString(R.string.lastOparation1), ""));
        operations.add(2, savedOperations.getString(getResources().getString(R.string.lastOparation2), ""));
        operations.add(3, savedOperations.getString(getResources().getString(R.string.lastOparation3), ""));
        operations.add(4, savedOperations.getString(getResources().getString(R.string.lastOparation4), ""));
        operationsAdapter.notifyDataSetChanged();

        if (isOnline()) {
            new RatesUpdate().execute(getResources().getString(R.string.updateURL));
        }
        else {
            Context context = getApplicationContext();
            CharSequence text = "Unable to update currency rates.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        fromCurrencyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                toCurrencyText.setText(exchangeCurrency(fromSpinner.getSelectedItem().toString(), toSpinner.getSelectedItem().toString(), fromCurrencyText.getDoubleValue()));
                String fromValue = "0.0";
                if (!fromCurrencyText.isEmpty()) {
                    fromValue = fromCurrencyText.getText().toString();
                }

                String newOperation = fromSpinner.getSelectedItem().toString() + " " + fromValue + " = "+ toSpinner.getSelectedItem().toString() + " " + toCurrencyText.getText().toString();
                operations.add(0, newOperation);
                if (operations.size() > 5) {
                    operations.remove(operations.size() - 1);
                }
                operationsAdapter.notifyDataSetChanged();
                swapedFrom = false;
                swapedTo = false;
                System.out.println(newOperation);

            }
        });

        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String currencyName = fromSpinner.getSelectedItem().toString();
                fromCurrencySymbol.setText(getResources().getString(getResources().getIdentifier(currencyName, "string", "com.bruno.currencyconverter")));
                toCurrencyText.setText(exchangeCurrency(fromSpinner.getSelectedItem().toString(), toSpinner.getSelectedItem().toString(), fromCurrencyText.getDoubleValue()));

                if (!swapedFrom) {
                    String fromValue = "0.0";
                    if (!fromCurrencyText.isEmpty()) {
                        fromValue = fromCurrencyText.getText().toString();
                    }

                    String newOperation = fromSpinner.getSelectedItem().toString() + " " + fromValue + " = " + toSpinner.getSelectedItem().toString() + " " + toCurrencyText.getText().toString();
                    operations.add(0, newOperation);
                    if (operations.size() > 5) {
                        operations.remove(operations.size() - 1);
                    }
                    operationsAdapter.notifyDataSetChanged();
                    System.out.println(newOperation);
                }
                swapedFrom = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // code here
            }

        });

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String currencyName = toSpinner.getSelectedItem().toString();
                toCurrencySymbol.setText(getResources().getString(getResources().getIdentifier(currencyName, "string", "com.bruno.currencyconverter")));
                toCurrencyText.setText(exchangeCurrency(fromSpinner.getSelectedItem().toString(), toSpinner.getSelectedItem().toString(), fromCurrencyText.getDoubleValue()));

                if (!swapedTo) {
                    String toValue = "0.0";
                    if (!toCurrencyText.isEmpty()) {
                        toValue = toCurrencyText.getText().toString();
                    }

                    String newOperation = fromSpinner.getSelectedItem().toString() + " " + fromCurrencyText.getText().toString() + " = " + toSpinner.getSelectedItem().toString() + " " + toValue;
                    operations.add(0, newOperation);
                    if (operations.size() > 5) {
                        operations.remove(operations.size() - 1);
                    }
                    operationsAdapter.notifyDataSetChanged();
                    System.out.println(newOperation);
                }
                swapedTo = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // code here
            }

        });

        swapButton = (Button) findViewById(R.id.swapButton);
        swapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String currencyName = fromSpinner.getSelectedItem().toString();
                fromSpinner.setSelection(adapter.getPosition(toSpinner.getSelectedItem().toString()));
                toSpinner.setSelection(adapter.getPosition(currencyName));

                fromCurrencySymbol.setText(getResources().getString(getResources().getIdentifier(fromSpinner.getSelectedItem().toString(), "string", "com.bruno.currencyconverter")));
                toCurrencySymbol.setText(getResources().getString(getResources().getIdentifier(toSpinner.getSelectedItem().toString(), "string", "com.bruno.currencyconverter")));

                fromCurrencyText.setText("1.0");

                swapedFrom = true;
                swapedTo = true;
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.currency_name_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        SharedPreferences currentStatus = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        fromCurrencyText.setText(currentStatus.getString(getResources().getString(R.string.fromValue), "1.0"));
        fromSpinner.setSelection(adapter.getPosition(currentStatus.getString(getResources().getString(R.string.fromCurrency), "GBP")));
        toSpinner.setSelection(adapter.getPosition(currentStatus.getString(getResources().getString(R.string.toCurrency), "USD")));

        SharedPreferences savedOperations = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        operations.clear();
        operations.add(0, savedOperations.getString(getResources().getString(R.string.lastOparation0), ""));
        operations.add(1, savedOperations.getString(getResources().getString(R.string.lastOparation1), ""));
        operations.add(2, savedOperations.getString(getResources().getString(R.string.lastOparation2), ""));
        operations.add(3, savedOperations.getString(getResources().getString(R.string.lastOparation3), ""));
        operations.add(4, savedOperations.getString(getResources().getString(R.string.lastOparation4), ""));
        operationsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences currentStatus = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        SharedPreferences.Editor editorStatus = currentStatus.edit();

        editorStatus.putString(getResources().getString(R.string.fromValue), fromCurrencyText.getText().toString());
        editorStatus.putString(getResources().getString(R.string.fromCurrency), fromSpinner.getSelectedItem().toString());
        editorStatus.putString(getResources().getString(R.string.toCurrency), toSpinner.getSelectedItem().toString());
        editorStatus.putString(getResources().getString(R.string.numberCreated), String.valueOf(numCreated));
        editorStatus.putString(getResources().getString(R.string.numberResumed), String.valueOf(numResumed));
        editorStatus.commit();

        SharedPreferences savedList = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        SharedPreferences.Editor editorList = savedList.edit();
        editorList.putString(getResources().getString(R.string.lastOparation0), operations.get(0));
        editorList.putString(getResources().getString(R.string.lastOparation1), operations.get(1));
        editorList.putString(getResources().getString(R.string.lastOparation2), operations.get(2));
        editorList.putString(getResources().getString(R.string.lastOparation3), operations.get(3));
        editorList.putString(getResources().getString(R.string.lastOparation4), operations.get(4));
        editorList.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences currentStatus = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        numResumed = Integer.parseInt(currentStatus.getString(getResources().getString(R.string.numberResumed), "0")) + 1;
        numberResumed.setText(String.valueOf(numResumed));

        numResumedLocal = numResumedLocal + 1;
        numberResumedLocal.setText(" (" + String.valueOf(numResumedLocal) + ")");

        SharedPreferences savedOperations = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        operations.clear();
        operations.add(0, savedOperations.getString(getResources().getString(R.string.lastOparation0), ""));
        operations.add(1, savedOperations.getString(getResources().getString(R.string.lastOparation1), ""));
        operations.add(2, savedOperations.getString(getResources().getString(R.string.lastOparation2), ""));
        operations.add(3, savedOperations.getString(getResources().getString(R.string.lastOparation3), ""));
        operations.add(4, savedOperations.getString(getResources().getString(R.string.lastOparation4), ""));
        operationsAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_converter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String exchangeCurrency(String fromCurrency, String toCurrency, double fromValue) {

        String exchange = fromCurrency + toCurrency;

        SharedPreferences savedRates = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
        String  oldExchangeRate = getResources().getString(getResources().getIdentifier(exchange, "string", "com.bruno.currencyconverter")).trim();
        double exchangeRate = Double.valueOf(savedRates.getString(exchange, oldExchangeRate)).doubleValue();
        double exchangedValue = fromValue * exchangeRate;
        String newValue = Double.toString(exchangedValue);
        return newValue;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    private class RatesUpdate extends AsyncTask<String, String, String> {

        ArrayList newRates = new ArrayList();
        ArrayList rateIDs = new ArrayList();

        @Override
        protected String doInBackground(String... params) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            Document doc;
            String lastUpdateTime = "default";

            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            try {
                doc = db.parse(new URL(params[0]).openStream());
                lastUpdateTime = doc.getElementsByTagName("Time").item(doc.getElementsByTagName("Time").getLength() - 1).getTextContent() + " " + doc.getElementsByTagName("Date").item(doc.getElementsByTagName("Date").getLength() - 1).getTextContent();
                for (int i = 0; i < doc.getElementsByTagName("Rate").getLength(); i++) {
                    newRates.add(doc.getElementsByTagName("Rate").item(i).getTextContent());
                    String id = doc.getElementsByTagName("Name").item(i).getTextContent().replace(" to ", "");
                    rateIDs.add(id);
                    System.out.println(rateIDs.get(i));
                    System.out.println(newRates.get(i));

                }

                return lastUpdateTime;

            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            SharedPreferences savedRates = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
            lastUpdateTime = savedRates.getString(getResources().getString(R.string.lastUpdateTime), getResources().getString(R.string.time));
            return lastUpdateTime;

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... text) {

        }

        @Override
        protected void onPostExecute(String result) {
            SharedPreferences savedRates = getSharedPreferences(getResources().getString(R.string.PREFS), 0);
            SharedPreferences.Editor editor = savedRates.edit();

            editor.putString(getResources().getString(R.string.lastUpdateTime), result);

            for (int i = 0; i < rateIDs.size(); i++) {
                editor.putString(rateIDs.get(i).toString(), newRates.get(i).toString());
            }

            editor.commit();
            lastUpdateText.setText(result);

            Context context = getApplicationContext();
            CharSequence text = "Exchange rates updated!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

        }
    }

}