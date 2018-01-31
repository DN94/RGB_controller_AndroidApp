package pl.polsl.dnkk.controlrgb;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.OnColorSelectedListener;

import java.io.IOException;
import java.util.UUID;


/**
 * Created by DawidN, Kordian K on 2017-12-28.
 */

public class LedActivity extends AppCompatActivity {
    Button butonSend, buttonDis;
    ColorPickerView colorPickerView;
    ToggleButton mixButton, waveButton;
    Switch switchOnOff;
    SeekBar seekSpec;
    TextView seekValue;
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private ProgressDialog progress;
    private boolean isBtConnected = false;
    int choosedColor = 0xFFFFFFFF;
    int choosedTime = 0;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);
        setContentView(R.layout.led_activity);
        butonSend = (Button) findViewById(R.id.buttonSend);
        buttonDis = (Button) findViewById(R.id.buttonDiscon);
        switchOnOff = (Switch) findViewById(R.id.switchOnOff);
        mixButton = (ToggleButton) findViewById(R.id.toggleButtonMix);
        waveButton = (ToggleButton) findViewById(R.id.toggleButtonWave);
        seekSpec = (SeekBar) findViewById(R.id.seekBar);
        seekValue = (TextView) findViewById(R.id.textViewSeek);
        new ConnectBT().execute();

        butonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSettings();
            }
        });
        buttonDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });

        seekSpec.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                seekValue.setText(String.valueOf(progress));
                choosedTime = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        colorPickerView = (ColorPickerView) findViewById(R.id.color_picker_view);
        colorPickerView.addOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int selectedColor) {
                Log.d("ColorSeekBar", "onColorChanged: 0x" + Integer.toHexString(selectedColor));
                choosedColor = selectedColor;
            }
        });
        colorPickerView.addOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int selectedColor) {
                Log.d("ColorPicker", "onColorChanged: 0x" + Integer.toHexString(selectedColor));
                choosedColor = selectedColor;
            }
        });
    }

    private void Disconnect() {
        if (btSocket != null) //jeśli zajęty
        {
            try {
                btSocket.close(); //zamkanij połączenie
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        finish(); //powrót do Main

    }

    private void sendSettings() {
        if (btSocket != null) {
            try {
                String toSend = "SET;T" + boolToStr(switchOnOff.isChecked()) + ";R" + String.format("%03d", (choosedColor & 0xFF0000) >> 16) +
                        ";G" + String.format("%03d", (choosedColor & 0xFF00) >> 8) + ";B" + String.format("%03d", (choosedColor & 0xFF)) +
                        ";F" + String.format("%04d",choosedTime)+";M"+ boolToStr(mixButton.isChecked())+";W"+ boolToStr(waveButton.isChecked())+";END";
                btSocket.getOutputStream().write(toSend.getBytes());
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private String boolToStr(boolean in) {
        if (in)
            return "1";
        else
            return "0";
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(LedActivity.this, "Łączenie...", "Proszę czekać!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) //w tle połącz z urządzenim
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            if (!ConnectSuccess) {
                Toast.makeText(getApplicationContext(), "Połączenie nieudane. Spróbuje ponownie.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Połączono", Toast.LENGTH_LONG).show();
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}
