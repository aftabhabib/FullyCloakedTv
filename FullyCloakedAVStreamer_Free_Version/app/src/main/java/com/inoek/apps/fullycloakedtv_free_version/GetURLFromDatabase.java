package com.inoek.apps.fullycloakedtv_free_version;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by harshasanthosh on 26/06/17.
 */

public class GetURLFromDatabase extends AsyncTask<String, Void, String> {

    Context cx;
    private UrlUpdaterInterface<String> callback;
    private ProgressDialog progressDialog;

    public GetURLFromDatabase(Context c, UrlUpdaterInterface<String> cb, ProgressDialog pd) {
        cx = c;
        callback = cb;
        progressDialog = pd;
    }

    private void showMessage(Context cxt) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(cxt);
        dlgAlert.setMessage("It seems that you have not turned ON your Internet");
        dlgAlert.setTitle("Information");
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                cx.startActivity(intent);
                System.exit(0);
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
        progressDialog.setMessage("Connecting...");
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(String result) {
        // TODO Auto-generated method stub
        progressDialog.dismiss();
        callback.OnCompleteUpdate(result);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        // TODO Auto-generated method stub
        super.onProgressUpdate(values);
    }

    @Override
    protected String doInBackground(String... params) {
        // TODO Auto-generated method stub
        String response = "";
        try {
            URL url = new URL(
                    "http://fullyclo-001-site1.dtempurl.com/fullycloaked_av.php");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();

            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                response += (char) data;
                data = isw.read();
            }
            isw.close();
            in.close();
            return response;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
}
