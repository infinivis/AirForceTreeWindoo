package ch.sykwatch.windooapidemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import ch.skywatch.windoo.api.JDCWindooEvent;
import ch.skywatch.windoo.api.JDCWindooManager;
import ch.skywatch.windoo.api.JDCWindooMeasurement;
import ch.sykwatch.windooapidemo.net.SendToWindooTask;





public class WindooApiDemo extends Activity implements Observer {


    static final String TAG = "WindooApiDemo";

    private final int SELECT_FILE = 0;

    private JDCWindooManager jdcWindooManager;
    private NumberFormat formatter;

    private TextView log;

    private TextView wind;
    private TextView temperature;
    private TextView humidity;
    private TextView pressure;

    private Button liveMeasurement;
    private Button btn_test;

    private ProgressDialog calibratingDialog;
    private AlertDialog volumeDialog;
    private AlertDialog plugDialog;

    private JDCWindooMeasurement currentMeasure;

    private  WebSocketClient mWebSocketClient;
//variables ajoutées par Romain

// function websocket

private void connectWebSocket() {
    URI uri;

    try {
       // uri = new URI("ws://10.192.114.203:8100");
       // uri = new URI("ws://192.168.0.10:8100");
        uri = new URI("ws://192.168.0.10:8080");

    } catch (URISyntaxException e) {
        e.printStackTrace();
        return;
    }

    mWebSocketClient = new WebSocketClient(uri) {
        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Log.i("Websocket", "Opened");
            //mWebSocketClient.send("Hello from heig-vd");
        }

        @Override
        public void onMessage(String s) {
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            Log.i("Websocket", "Closed " + s);
        }

        @Override
        public void onError(Exception e) {
            Log.i("Websocket", "Error " + e.getMessage());
        }
    };
    mWebSocketClient.connect();
}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        connectWebSocket();


        setContentView(R.layout.activity_windoo_api_demo);

        jdcWindooManager = JDCWindooManager.getInstance();
        jdcWindooManager.setToken("bbe5808d‐18bf‐4290‐af7e‐26a31d86bde9");



        currentMeasure = new JDCWindooMeasurement();



        log = (TextView) findViewById(R.id.log);
        wind = (TextView) findViewById(R.id.wind);
        temperature = (TextView) findViewById(R.id.temperature);
        humidity = (TextView) findViewById(R.id.humidity);
        pressure = (TextView) findViewById(R.id.pressure);





        liveMeasurement = (Button) findViewById(R.id.showLiveMeasurement);
        liveMeasurement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Show live measurement");

                JDCWindooMeasurement measurement = jdcWindooManager.getLive();
                Toast.makeText(WindooApiDemo.this, measurement.toString(), Toast.LENGTH_LONG).show();
            }
        });

        btn_test = (Button) findViewById(R.id.btn_test);
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Test");

                JDCWindooMeasurement measurement = jdcWindooManager.getLive();
                //websocket//////////////////////
              // mWebSocketClient.send(measurement.toString());
                ///////////////////////////////////


                Log.i(TAG, measurement.toString());
            }
        });

        formatter = NumberFormat.getInstance();
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);


    }

    final class JavaScriptInterface {
        JavaScriptInterface () { }
        public String getSomeString() {
            return "string";
        }};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.windoo_api_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();

        jdcWindooManager.addObserver(this);
        jdcWindooManager.enable(this);
    }

    @Override
    public void onPause(){
        super.onPause();

        jdcWindooManager.disable(this);
        jdcWindooManager.deleteObserver(this);
    }

    @Override
    public void update(Observable observable, final Object object) {

        runOnUiThread(new Runnable() {
            public void run() {

                JDCWindooEvent e = (JDCWindooEvent) object;

                // New Status
                if (e.getType() == JDCWindooEvent.JDCWindooAvailable) {
                    Log.d(TAG, "JDCWindooAvailable");
                    log.setText("JDCWindooAvailable");

                    dismissVolumeDialog();
                    dismissPlugDialog();
                    showCalibratingDialog(WindooApiDemo.this);

                } else if (e.getType() == JDCWindooEvent.JDCWindooNotAvailable) {
                    Log.d(TAG, "JDCWindooNotAvailable");
                    log.setText("JDCWindooNotAvailable");

                    showPlugDialog(WindooApiDemo.this);
                    resetValues();

                } else if (e.getType() == JDCWindooEvent.JDCWindooCalibrated) {
                    Log.d(TAG, "JDCWindooCalibrated");
                    log.setText("JDCWindooCalibrated");

                    dismissCalibratingDialog();
                    dismissVolumeDialog();
                    dismissPlugDialog();

                } else if (e.getType() == JDCWindooEvent.JDCWindooVolumeNotAtItsMaximum) {
                    Log.d(TAG, "JDCWindooVolumeNotAtItsMaximum");
                    log.setText("JDCWindooVolumeNotAtItsMaximum");

                    dismissCalibratingDialog();
                    dismissPlugDialog();
                    showVolumeDialog(WindooApiDemo.this);

                    resetValues();

                } else if (e.getType() == JDCWindooEvent.JDCWindooPublishSuccess) {
                    Log.i(TAG, "JDCWindooPublishSuccess");
                    log.setText("JDCWindooPublishSuccess");

                    JSONObject json = (JSONObject)e.getData();
                    // do whatever you want with the measure
                    Toast.makeText(WindooApiDemo.this, "JDCWindooPublishSuccess: " + json.toString(), Toast.LENGTH_LONG).show();

                } else if (e.getType() == JDCWindooEvent.JDCWindooPublishException) {
                    Log.e(TAG, "JDCWindooPublishException: " + e.getData());
                    log.setText("JDCWindooPublishException: " + e.getData());
                    Toast.makeText(WindooApiDemo.this, "JDCWindooPublishException: " + e.getData(), Toast.LENGTH_LONG).show();

                // New Data



                    /////////////        New WIND VALUE         /////////////
                    /////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////


                } else if (e.getType() == JDCWindooEvent.JDCWindooNewWindValue) {
                    Log.d(TAG, "Wind received : " + e.getData());
                    log.setText("JDCWindooNewWindValue");
                    wind.setText(formatter.format(e.getData()));
                    currentMeasure.setWind((Double)e.getData());



                    ////// my part
                    JSONObject jsonObj = new JSONObject();
                    try {
                        jsonObj.put("id","android");
                        jsonObj.put("type","text");
                        jsonObj.put("windSpeed",formatter.format(e.getData()));
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }

                   String jsonString = jsonObj.toString();

                    Log.i(TAG, "sended wind  toooooooooooooooooo server : " + jsonString);

                    mWebSocketClient.send(jsonString);



                } else if (e.getType() == JDCWindooEvent.JDCWindooNewTemperatureValue) {
                    Log.d(TAG, "Temperature received : " + e.getData());
                    log.setText("JDCWindooNewTemperatureValue");
                    temperature.setText(formatter.format(e.getData()));
                    currentMeasure.setTemperature((Double)e.getData());
                } else if (e.getType() == JDCWindooEvent.JDCWindooNewHumidityValue) {
                    Log.d(TAG, "Humidity received : " + e.getData());
                    log.setText("JDCWindooNewHumidityValue");
                    humidity.setText(formatter.format(e.getData()));
                    currentMeasure.setHumidity((Double)e.getData());
                } else if (e.getType() == JDCWindooEvent.JDCWindooNewPressureValue) {
                    Log.d(TAG, "Pressure received : " + e.getData());
                    log.setText("JDCWindooNewPressureValue");
                    pressure.setText(formatter.format(e.getData()));
                    currentMeasure.setPressure((Double)e.getData());
                }
            }
        });
    }

    private void showPlugDialog(Context context) {
        if (plugDialog == null || !plugDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getApplicationContext().getString(R.string.plug_title));
            builder.setMessage(context.getApplicationContext().getString(R.string.plug_message));
            plugDialog = builder.create();
            plugDialog.setCanceledOnTouchOutside(false);
            plugDialog.show();
        }
    }

    public void dismissPlugDialog() {
        if (plugDialog != null && plugDialog.isShowing()) {
            plugDialog.cancel();
        }
    }

    private void showVolumeDialog(Context context) {
        if (volumeDialog == null || !volumeDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getApplicationContext().getString(R.string.volume_up_title));
            builder.setMessage(context.getApplicationContext().getString(R.string.volume_up_message));
            volumeDialog = builder.create();
            volumeDialog.setCanceledOnTouchOutside(false);
            volumeDialog.show();
        }
    }

    public void dismissVolumeDialog() {
        if (volumeDialog != null && volumeDialog.isShowing()) {
            volumeDialog.cancel();
        }
    }

    private void showCalibratingDialog(Context context){
        if (calibratingDialog == null || !calibratingDialog.isShowing()) {
            calibratingDialog = ProgressDialog.show(context,
                    getString(R.string.calibrating_title),
                    getString(R.string.calibrating_message), true);
            calibratingDialog.show();
        }
    }

    public void dismissCalibratingDialog() {
        if (calibratingDialog != null && calibratingDialog.isShowing()) {
            calibratingDialog.cancel();
        }
    }

    private void resetValues(){
        wind.setText(R.string.na);
        temperature.setText(R.string.na);
        humidity.setText(R.string.na);
        pressure.setText(R.string.na);
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    public void fullfillFakeMeasure() {
        // Identification
        currentMeasure.setNickname("ApiUser");
        currentMeasure.setEmail("apiuser@windoo.ch");
        currentMeasure.setPicture(null);
        currentMeasure.setPictureGuid(null);
        currentMeasure.setCreatedAt(new Date());
        currentMeasure.setUpdatedAt(new Date());

        // Localisation and orientation
        currentMeasure.setLatitude(0.0);
        currentMeasure.setLongitude(0.0);
        currentMeasure.setAccuracy((float)0.0);
        currentMeasure.setAltitude(0.0);
        currentMeasure.setSpeed((float)0.0);
        currentMeasure.setOrientation((float)0.0);
    }

    // Pictures utils
    private void chooseAPicture() {
        // Start intent to choose a picture from the library
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select picture"), SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_FILE && data != null) {
            String selectedFilePath = getPath(data.getData(), WindooApiDemo.this);
            File picture = new File(selectedFilePath);

            fullfillFakeMeasure();
            currentMeasure.setPicture(picture);
            new SendToWindooTask(WindooApiDemo.this).execute(currentMeasure);
        }
    }

    private String getPath(Uri uri, Activity activity) {
        String[] projection = { MediaStore.MediaColumns.DATA };
        Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

}
