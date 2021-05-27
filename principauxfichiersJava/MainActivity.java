package com.tiee.etienne;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener {
    String address = null;
    private ProgressDialog progress; //Barre de progression pour la connection au périphérique
    BluetoothAdapter myBluetooth = null;
    public static BluetoothSocket btSocket = null;
    private boolean isBtConnected = false; //Indique si on est connecté ou non à un appareil Bluetooth
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // Classe permettant d'executer une tâche en arrière plan
    {
        private boolean ConnectSuccess = true;
        @Override
        protected void onPreExecute() //Cette action se passe en premier (On crée la fenêtre de chargement)
        {
            progress = ProgressDialog.show(MainActivity.this, "Connection...", "Veuillez Patienter!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) //Cette action est faite en arrière-plan (On se connecte à l'appareil)
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//On récupère l'objet Bluetooth Adapter associé à l'appareil sélectionné
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//On essaye de se connecter à l'adresse MAC de l'appareil
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//On crée un objet socket associé à l'appareil
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//On démarre la connection
                    btSocket.getOutputStream().write(("RCON").getBytes()); //On envoi la séquence RCON à l'appareil afin d'initialiser la connection
                    while (btSocket.getInputStream().available()<2) //On attend de recevoir un message de plus de 2 octets
                    {
                        Log.i("XXXX", String.valueOf(btSocket.getInputStream().available()));
                    }
                    byte[] packetBytes = new byte[btSocket.getInputStream().available()];
                    int bytes = btSocket.getInputStream().read(packetBytes); //On récupère ce message
                    String readMessage = new String(packetBytes, 0, bytes, "US-ASCII");
                    Log.i("Received", readMessage);
                    int h = Integer.valueOf(readMessage);
                    if (h != 1)//Si la version du protocole de l'appareil n'est pas la même que celle utilisée par le téléphone
                    {
                        ConnectSuccess = false;
                        btSocket.close();
                    }
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)//Cette action se passe une fois la tâche en arrière plan terminée
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getApplicationContext(), "Connection échouée", Toast.LENGTH_SHORT).show();
                finish();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Connecté", Toast.LENGTH_SHORT).show();
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.Commander: //Si le bouton Commander à été pressé
                Intent i = new Intent(MainActivity.this, Command_Activity.class);
                startActivity(i);
                break;

            case R.id.Deconnecter: //Si le bouton Déconnecté à été pressé
                if (btSocket!=null)//Si l'appareil est bien connecté
                {
                    try
                    {
                        Toast.makeText(getApplicationContext(), "Déconnecté !", Toast.LENGTH_SHORT).show();
                        btSocket.close();
                    }
                    catch (IOException e)
                    {
                        Toast.makeText(getApplicationContext(), "Erreur !", Toast.LENGTH_SHORT).show();
                    }
                    finish(); //On retourne à l'activitée précédente
                }
        }
    }

    Button bDeconnect = null;
    Button bCommand = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) //S'execute lors de la création de l'activitée
    {
        super.onCreate(savedInstanceState);
        Intent newint = getIntent();
        address = newint.getStringExtra(Connection_Activity.EXTRA_ADDRESS);
        new ConnectBT().execute(); //Call the class to connect
        setContentView(R.layout.activity_main);
        bCommand = (Button) findViewById(R.id.Commander);
        bDeconnect = (Button) findViewById(R.id.Deconnecter);

        bDeconnect.setOnClickListener(this);
        bCommand.setOnClickListener(this);
    }
}