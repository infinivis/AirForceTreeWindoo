package ch.sykwatch.windooapidemo.net;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import ch.skywatch.windoo.api.JDCWindooManager;
import ch.skywatch.windoo.api.JDCWindooMeasurement;
import ch.sykwatch.windooapidemo.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Task to send a measure to the Windoo server
 */
public class SendToWindooTask extends AsyncTask<JDCWindooMeasurement, Void, Object> {

    static final String TAG = "SendToWindooTask";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
    private Context context;
    private ProgressDialog dialog;

    public SendToWindooTask(Context context) {
        this.context = context;

        Calendar cal = Calendar.getInstance();
        dateFormat.setTimeZone(cal.getTimeZone());

        // show the loading dialog
        dialog = ProgressDialog.show(context,
                context.getString(R.string.live_save_measures_send_title),
                context.getString(R.string.live_save_measures_send_message), true);
        dialog.show();
    }

    @Override
    protected Object doInBackground(JDCWindooMeasurement... measures) {
        JDCWindooMeasurement measure = measures[0];
        JDCWindooManager.getInstance().publishMeasure(measure);
        return null;
    }

    @Override
    protected void onPostExecute(Object result) {
        dialog.dismiss();
    }
}