package com.tiee.etienne;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class Processing_Activity extends AppCompatActivity
{
    BluetoothSocket btSocket = Command_Activity.btSocket;
    TextView stattext = null;

    private class Processing extends AsyncTask<Void, Void, Void>//
    {

        @Override
        protected Void doInBackground(Void... voids)
        {
            try
            {
                while (btSocket.getInputStream().available() < 3) {
                }
                byte[] packetBytes = new byte[btSocket.getInputStream().available()];
                int bytes = btSocket.getInputStream().read(packetBytes);
                String readMessage = new String(packetBytes, 0, bytes, "US-ASCII");
                Log.i("ReceivedV2", readMessage);
                if (readMessage.equals("SAE"))
                {
                    stattext.setText("Commande Terminée !");
                }
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(), "Erreur !", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing_);
        stattext = (TextView) findViewById(R.id.textView);

        new Processing().execute();
    }
}