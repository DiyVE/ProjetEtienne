package com.tiee.etienne;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Command_Activity extends AppCompatActivity
{
    public static BluetoothSocket btSocket = MainActivity.btSocket;

    public void sendCommand(Cocktails cocktail, boolean add_Ice)//Cette fonction permet d'envoyer, avec un système d'acknowledge une commande prise par l'utilisateur
    {
        boolean arduino_ack = false;
        while (arduino_ack == false)// Tant que la arduino n'a pas correctement reçu le message
        {
            try
            {
                btSocket.getOutputStream().write(("SL" + String.format("%02d", cocktail.getCocktailID()) + "G" + add_Ice + "E").getBytes()); //On envoi le message à l'appareil
                while (btSocket.getInputStream().available()<3) {} //On attend de recevoir au moins un message de 3 octets
                byte[] packetBytes = new byte[btSocket.getInputStream().available()];
                int bytes = btSocket.getInputStream().read(packetBytes);
                String readMessage = new String(packetBytes, 0, bytes, "US-ASCII");
                Log.i("ReceivedV2", readMessage);
                if (readMessage.equals("SAE")) //Si on reçoit un acknowledge de la part de la arduino
                {
                    arduino_ack = true;//On fait en sorte de stopper la boucle pour passer à la suite
                }
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(), "Erreur !", Toast.LENGTH_SHORT).show();
            }
        }
        Intent i = new Intent(Command_Activity.this, Processing_Activity.class);
        startActivity(i); //On bascule sur la prochaine activitée, c'est-à-dire Processing_Activity
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) //S'execute lors de la création de l'activitée
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_);

        List<Cocktails> image_details = getListData();
        final ListView listView = (ListView) findViewById(R.id.listviewcocktails);
        listView.setAdapter(new CocktailsListAdapter(this, image_details));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id)
            {
                Object o = listView.getItemAtPosition(position);
                Cocktails cocktail = (Cocktails) o;
                Toast.makeText(Command_Activity.this, "Selected :" + " " + cocktail.getCocktailID(), Toast.LENGTH_LONG).show();
                if (btSocket!=null)
                {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Command_Activity.this);
                        alertDialogBuilder.setTitle("Ajouter des glaçons");
                        alertDialogBuilder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int j)
                            {
                                sendCommand(cocktail, true);
                            }
                        });
                        alertDialogBuilder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int j)
                            {
                                sendCommand(cocktail, false);
                            }
                        });
                        alertDialogBuilder.create();
                        alertDialogBuilder.show();
                }
            }
        });

    }

    private List<Cocktails> getListData() //Permet de générer la liste des cocktails disponibles à partir de la classe Cocktails
    {
        List<Cocktails> list = new ArrayList<Cocktails>();
        Cocktails cocktail_rio = new Cocktails("Cocktail Rio", 1, "ckt_rio", "Jus d’orange, Limonade, Grenadine");
        Cocktails diab_gren = new Cocktails("Diabolo grenadine", 2, "diab_gren", "Grenadine, Limonade");
        Cocktails diab_ment = new Cocktails("Diabolo Menthe", 3, "diab_ment", "Limonade, Sirop de Menthe");
        Cocktails cocktails_lemonmint = new Cocktails("Lemon mint", 4, "ckt_lemint", "Sirop de Menthe, Sirop de Sucre de Canne, Jus d'Orange");
        Cocktails cocktails_blcbec = new Cocktails("Blanc Bec Menthe", 5, "ckt_blcbec", "Sirop de Menthe, Jus de Citron");
        Cocktails cocktails_abricotsprk = new Cocktails("Apricot Sparkler", 6, "ckt_aprsprk", "Jus d'Abricot, Jus de Citron, Limonade");
        Cocktails eau_ment = new Cocktails("Menthe à l'Eau", 7, "eau_ment", "Sirop de Menthe, Eau");
        Cocktails eau_gren = new Cocktails("Sirop de Grenadine", 8, "eau_gren", "Grenadine, Eau");
        Cocktails limonade = new Cocktails("Limonade", 9, "limo", "...");
        Cocktails eau = new Cocktails("Eau", 9, "eau", "...");

        list.add(cocktail_rio);
        list.add(diab_gren);
        list.add(diab_ment);
        list.add(cocktails_lemonmint);
        list.add(cocktails_blcbec);
        list.add(cocktails_abricotsprk);
        list.add(eau_ment);
        list.add(eau_gren);
        list.add(limonade);
        list.add(eau);


        return list;
    }
}