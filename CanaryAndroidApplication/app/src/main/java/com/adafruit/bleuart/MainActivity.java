/*Simple Android application with a "login" and a main page. The main page uses an Adafruit BLUE
library that allows from communication using BLE to UART. The library is located at
https://github.com/adafruit/Adafruit_Android_BLE_UART. It then currently connects to Parse and
pushes the data to a Parse database. Bluetooth is a bit wonky, but overall works as stated.
NOTE: ASB.iml should be Canary.iml, as ASB was a previous version.
 */

package com.adafruit.bleuart;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseAnalytics;

import java.math.BigInteger;

public class MainActivity extends Activity implements BluetoothLeUart.Callback {

    // UI elements
    private TextView messages;
    private EditText input;

    // Bluetooth LE UART instance.  This is defined in BluetoothLeUart.java.
    private BluetoothLeUart uart;

    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.setText(text);
            }
        });
    }

    // Handler for mouse click on the send button.
    public void sendClick(View view) {
        String message = input.getText().toString();
        uart.send(message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab references to UI elements.
        messages = (TextView) findViewById(R.id.messages);
        input = (EditText) findViewById(R.id.input);

        // Initialize UART.
        uart = new BluetoothLeUart(getApplicationContext());
       // Parse.initialize(this, /*YOUR APP ID*/, /*YOUR CLIENT ID*/);
    }

    // OnCreate, called once to initialize the activity.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // OnResume, called right before UI is displayed.  Connect to the bluetooth device.
    @Override
    protected void onResume() {
        super.onResume();
        writeLine("Connecting...");
        uart.registerCallback(this);
        uart.connectFirstAvailable();
    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
        uart.unregisterCallback(this);
        uart.disconnect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // UART Callback event handlers.
    @Override
    public void onConnected(BluetoothLeUart uart) {
        // Called when UART device is connected and ready to send/receive data.
        writeLine("Connected!");
    }

    @Override
    public void onConnectFailed(BluetoothLeUart uart) {
        // Called when some error occured which prevented UART connection from completing.
        writeLine("Error connecting to device!");
    }

    @Override
    public void onDisconnected(BluetoothLeUart uart) {
        // Called when the UART device disconnected.
        writeLine("Disconnected!");
    }

    @Override
    public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        // Called when data is received by the UART.



        writeLine("Received: " + rx.getStringValue(0));
        String data = rx.getStringValue(0);
        String delim = "[,]";
        final String[] tokens = data.split(delim);

        runOnUiThread(new Runnable() {
            @Override
            public void run() { //Using the conversions in the Hardware README.
                TextView tempDoe = (TextView) findViewById(R.id.tempMonitor);
                TextView humid = (TextView) findViewById(R.id.humidMonitor);
                TextView noisePol = (TextView) findViewById(R.id.noisePolMonitor);
                TextView carMon = (TextView) findViewById(R.id.coMonitor);
                float value = new BigInteger(tokens[0], 16).longValue();
                //System.out.println(value);
                float temp = (value * 50) / 65535;
                //System.out.println(temp);
                double tempFah = temp * 1.8000 + 32.00;
                String tempFahStr = String.valueOf(tempFah);
                //System.out.println(tempFah);
                int humidity = (Integer.parseInt(tokens[1],16) * 100) / 65535;
                String humidityDoe = String.valueOf(humidity);
                double noiseLev = Integer.parseInt(tokens[2],16);
                noiseLev =  20*(Math.log10(noiseLev/1024));
                String noiseLevStr = String.valueOf(noiseLev);
                int methane = Integer.parseInt(tokens[3],16);
                String methaneStr = String.valueOf(methane);
                tempDoe.setText(tempFahStr);
                humid.setText(humidityDoe);
                noisePol.setText(noiseLevStr);
                carMon.setText(methaneStr);
            }
        });



        float value = new BigInteger(tokens[0], 16).longValue(); //Done twice because, well, time
        //System.out.println(value);                             //issues...
        float temp = (value * 50) / 65535;
        //System.out.println(temp);
        double tempFah = temp * 1.8000 + 32.00;
        //System.out.println(tempFah);
        int humidity = (Integer.parseInt(tokens[1],16) * 100) / 65535;
        double noiseLev = Integer.parseInt(tokens[2],16);
        noiseLev =  20*(Math.log10(noiseLev/1024));
        int methane = Integer.parseInt(tokens[3],16);



        System.out.println(tempFah + " " + humidity + " " + noiseLev + " " + methane);

        writeLine("Pushing to Parse...");
        //Parse.initialize(this, /*YOUR APP ID*/, /*YOUR CLIENT ID*/);
        ParseObject dataStuff = new ParseObject("SensorData");
        dataStuff.put("User","UserName");
        dataStuff.put("Latitude","Latitude Information");
        dataStuff.put("Longitude","Longitude Information");
        dataStuff.put("Temperature",tempFah);
        dataStuff.put("Humidity",humidity);
        dataStuff.put("NoiseLevel",noiseLev);
        dataStuff.put("Methane",methane);
        dataStuff.saveInBackground();
        writeLine("Finished pushing.");
        writeLine("Temperature (F): " + tempFah);
        writeLine("Humidity: " + humidity);
        writeLine("Noise Level: " + noiseLev);
        writeLine("Methane: " + methane);
        writeLine("Sensor Data Collected and pushed!");

    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // Called when a UART device is discovered (after calling startScan).
        writeLine("Found device: " + device.getAddress());
    }
}
