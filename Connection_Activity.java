package com.tiee.etienne;

import android.app.Activity;
import java.util.ArrayList;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Connection_Activity extends Activity implements View.OnClickListener
{
    private final static int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
    private Set<BluetoothDevice> pairedDevices; //Liste des Appareils appairés
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static String EXTRA_ADDRESS = "device_address";


    public void onClick(@org.jetbrains.annotations.NotNull View v)
    {
        switch (v.getId())
        {
            case R.id.connectEtienne: //Si le bouton Connecter Etienne à été pressé
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) //Si il n'y a pas de module Bluetooth sur le téléphone
                {
                    Toast.makeText(getApplicationContext(), "Bluetooth non activé !", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (!bluetoothAdapter.isEnabled()) //On essaye d'activer le Bluetooth si il est désactivé
                    {
                        Toast.makeText(getApplicationContext(), "Bluetooth non activé !", Toast.LENGTH_SHORT).show();
                        Intent activeBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(activeBlueTooth, REQUEST_CODE_ENABLE_BLUETOOTH);
                    }
                    else
                    {
                        pairedDevices = bluetoothAdapter.getBondedDevices(); //Et si il est activé on essaye de récuperer la listes des appareils appairés
                        ArrayList list = new ArrayList();

                        if (pairedDevices.size()>0)
                        {

                            for(BluetoothDevice bt : pairedDevices)
                            {
                                list.add(bt.getName() + "\n" + bt.getAddress());
                            }
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
                        }
                        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
                        devicelist.setAdapter(adapter);


                    }
                }
                break;
        }
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView av, View v, int arg2, long arg3) //Si on clique sur l'appareil auquel on souhaite se connecter
        {
            //Récupère l'adresse MAC de l'appareil auquel on souhaite se connecter
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            //On se prépare à démarrer l'activité MainActivity
            Intent i = new Intent(Connection_Activity.this, MainActivity.class);
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }
    };

    Button b1 = null;
    ListView devicelist;

    @Override
    protected void onCreate(Bundle savedInstanceState) //S'execute lors de la création de l'activitée
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_);

        b1 = (Button)  findViewById(R.id.connectEtienne);
        devicelist = (ListView) findViewById(R.id.listview);
        devicelist.setOnItemClickListener(myListClickListener);
        b1.setOnClickListener(this);
    }


}