package id.ac.unja.si.ktmscanner;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

/**
 * Created by norman on 2/28/18.
 */

public class BarcodeSender extends AsyncTask<Void,Void,String> {

    @SuppressLint("StaticFieldLeak")
    private Context c;
    private String urlAddress, key;
    @SuppressLint("StaticFieldLeak")
    private ProgressBar progressBar;

    BarcodeSender(Context c, ProgressBar progressBar,  String urlAddress, String key) {
        this.c = c;
        this.progressBar = progressBar;
        this.urlAddress = urlAddress;
        this.key = key;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... params) {
        return this.send();
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);

        if(response == null) {
            validationFailed("Terjadi kesalahan.");
        }
        else {
            progressBar.setVisibility(View.INVISIBLE);
            String res = "";
            String eventTitle = "";
            try {
                JSONObject jObj = new JSONObject(response);
                res = jObj.getString("msg");
                eventTitle = jObj.getString("title");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (res) {
                case "1":
                    setKey(this.key);
                    Intent intent = new Intent(this.c, ActReader.class);
                    intent.putExtra("EVENT_TITLE",eventTitle);
                    this.c.startActivity(intent);
                    break;
                case "404":
                    validationFailed("Registrasi tidak dapat ditemukan.");
                    Log.e("barcode", "404");
                    break;
                default:
                    validationFailed("Terjadi kesalahan");
                    Log.e("barcode", "EERRRROR");
                    break;
            }
        }
    }


    private String send() {

        // Connect
        HttpURLConnection con=Connector.connect(urlAddress);
        if (con == null) return null;

        try {
            OutputStream os=con.getOutputStream();
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
            bw.write(new BarcodeDataPackager(key).packData());
            bw.flush();
            bw.close();
            os.close();

            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br=new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response=new StringBuilder();
                String line;

                while ((line=br.readLine()) != null) response.append(line);

                br.close();
                return response.toString();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void validationFailed(String msg) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this.c);
        alert.setTitle("Terjadi kesalahan");
        alert.setMessage(msg);
        alert.setCancelable(false);
        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) {
                goToBarcodeActivity();
            }
        });
        alert.show();
    }


    // Saves the key to the internal storage
    private void setKey(String key) {
        String filename = "key";
        try {
            FileOutputStream fos = this.c.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(key.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // REDIRECT METHODS //
    private void goToBarcodeActivity() {
        Intent intent = new Intent(this.c, ActBarcode.class);
        this.c.startActivity(intent);
    }

}
