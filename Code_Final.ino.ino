//*********************************Ajout des bibliothèques*********************************\\

#include <Braccio.h> //Ajout de la bibliothèque Braccio
#include <Servo.h> //Ajout de la bibliothèque Servo
#include <SoftwareSerial.h> //Ajout de la bibliothèque 
#include "pitches.h" //Ajout des correspondances entre notes et fréquences pour la mélodie
#include <LiquidCrystal_I2C.h> // Ajout de la bibliothèque de l'écran LCD avec le module I2C

struct Array3 {
  int array[3];
};
//*******************************Déclaration des constantes*********************************\\

int positions_ingredients[] = {0, 45, 90, 135, 180, 45, 90, 135, 0}; //Associe chaque numéro de verre "(index)" à un angle pour le moteur M1
int ChantOM[] = {
  NOTE_G3,  NOTE_G3, 0, NOTE_G3,  NOTE_G3, 0, NOTE_G3,  NOTE_G3, NOTE_G3
};
int noteDurations[] = {
  5, 5, 3, 5, 5, 3, 3, 3, 3
};
const int boutoninterruptionPin = 2; //Pin réservé pour le bouton d'interruption
const int bluetooth1Pin = 4; //Pin réservé pour le module Bluetooth
const int bluetooth2Pin = A0; //Pin réservé pour le module Bluetooth
const int relaipompePin = 7; // /Pin réservé pour le relais pompe
const int hautparleurPin = 8; //Pin réservé pour le haut-parleur
const int trigPin = 9; //Pin réservé pour le capteur ultrasons
const int echoPin = 12; //Pin réservé pour le capteur ultrasons

long duration;
int temps;
int distanceCm; //Distance en cm entre le capteur ultrasons et le liquide présent dans le bidon
bool interruptTriggered = false;
Array3 donnees_bt;
SoftwareSerial hc06(bluetooth1Pin, bluetooth2Pin);
LiquidCrystal_I2C lcd(0x27, 20, 4); // Donner l'adresse de l'écran LCD, le nombres de caractères et le nombre de lignes.

//********************************Déclaration des variables********************************\\


Servo base;
Servo shoulder;
Servo elbow;
Servo wrist_rot;
Servo wrist_ver;
Servo gripper;
int req_ID;
int add_ice; //Ajout de glace si égal à 1
String cmd = "";

void jouer_chant_OM()
// Cette fonction sert à jouer un chant de supporter de l'OM, au démarrage du système
{
  for (int thisNote = 0; thisNote < 9; thisNote++)
  {
    // Pour calculer la durée d'une note, on divise 1 seconde (en ms) par le type de note
    // par exemple, durée d'une quarte (4 tons) = 1000/4 , durée d'une octave (8 tons) = 1000/8, etc
    int noteDuration = 1000 / noteDurations[thisNote];
    tone(8, ChantOM[thisNote], noteDuration);

    // Pour distinguer les notes, on définit un temps minimum entre elles
    // La durée de la note + 30% marche bien
    int pauseBetweenNotes = noteDuration * 1.30;
    delay(pauseBetweenNotes);

    // on arrête de jouer la note :
    noTone(8);
  }
}

Array3 recuperation_donnees_bluetooth()
{
  /*
    Cette fonction permet de récupérer les informations envoyées par le téléphone
    Le premier élément du tableau indique le type de donnée reçu,
    ce qui permet d'en déduire la nature des cases suivantes:  - donnees[0] = 0 --> donnees[1]:"ID du cocktail commandé"
                                                                              --> donnees[2]:"Ajout de glace"

                                                             - donnees[0] = 1 --> donnees[1]:"<à completer>"
                                                                              --> donnees[2]:"<à completer>"
    Elle retourne la valeur -1 sur au moins la première case de son tableau si aucune donnée n'a été reçue
  */
  Array3 donnees = { -1, -1, -1};
  String message_rec = "";
  bool dataAreValid = true;
  int pos = 5;
  bool ice;
  if (hc06.available())
  {
    //On récupère le message reçu par le module HC-06
    while (hc06.available() > 0)
    {
      message_rec += (char)hc06.read();
      delay(10);
    }

    //On s'assure que le message est présent en intégralité
    if ((message_rec[0] == 'S') && (message_rec[message_rec.length() - 1] == 'E'))
    {
      switch (message_rec[1])
      {
        case 'L':
          donnees.array[0] = 0;
          donnees.array[1] = ((String)message_rec[2] + (String)message_rec[3]).toInt();
          donnees.array[2] = message_rec[5];
          break;
        case 'P':
          donnees.array[0] = 1;
          break;
        default:
          dataAreValid = false;
          break;
      }
    }
    else
    {
      dataAreValid = false;
    }
    if (!dataAreValid)
    {
      Serial.println("Invalid Data Received : " + message_rec);
      hc06.write("SNE");//On signale au téléphone un non Acknowledge afin de recevoir un nouveau message
    }
    else
    {
      hc06.write("SAE");//On signale la bonne réception du message au téléphone
    }
  }
  delay(10);

  return donnees;
}

void ajouter_ingredient(int numero_ingredient, float qt_ingredient)
//Permet d'ajouter un ingrédient spécifié grâce à son numéro dans le verre principal.
//On doit également indiquer la quantité d'ingrédient que l'on souhaite verser ainsi que le type (glace pilée, sirop, liquides assez fluides comme les jus)
{
  if (numero_ingredient != 9)
  {
    if (numero_ingredient <= 4)//CHANGENOTE : Changer le sens de l'égalité pour le 180 en fonction des résultats des mesures
    {
      recuperer_et_deplacer_hemidroite(numero_ingredient);
    }
    else
    {
      recuperer_et_deplacer_hemigauche(numero_ingredient);
    }
    verser(numero_ingredient, qt_ingredient);
    reposer_ingredient(numero_ingredient);
  }
  else
  {
    Braccio.ServoMovement(40,           positions_ingredients[0], 90, 0, 120, 60, 70);
    verser(9, qt_ingredient);
  }

}

void recuperer_et_deplacer_hemidroite(int numero_ingredient)
//Attrape le conteneur associé au type d'ingrédient et le déplace à la position associée à son numéro. Ces déplacements sont valides uniquement dans l'hemisphère de droite, cad dans la partie contenant les ingrédients numérotés de 0 à 4
{

  int angle_consigne = positions_ingredients[numero_ingredient];

  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 180, 60, 10);
  //se penche vers le verre
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 180, 60, 10);
  //rectifie position de saisie
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 155, 60, 10);
  //saisie du verre
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 155, 60, 60);
  Serial.println("Pos Droite");  //Attend 1 seconde
  //revient en position droite
  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 120, 60, 60);
  Braccio.ServoMovement(40,           positions_ingredients[0], 90, 0, 120, 60, 60);



}

void recuperer_et_deplacer_hemigauche(int numero_ingredient)
//Attrape le conteneur associé au type d'ingrédient et le déplace à la position associée à son numéro ces déplacements sont valides uniquement dans l'hémisphère de gauche,
//cad dans la partie contenant les ingrédients numérotés de 5 à 7, à savoir jus abricot, glaçons, eau
{

  int angle_consigne = positions_ingredients[numero_ingredient];

  Braccio.ServoMovement(40,           positions_ingredients[0] , 90, 180, 90, 60, 73);
  //déplacement vers le verre contenant l'ingrédient voulu,rotation M1
  Braccio.ServoMovement(40,           angle_consigne, 90, 180, 90, 60, 73);
  //positionnement central, passage par le centre
  Braccio.ServoMovement(40,           angle_consigne, 90, 90, 90, 60, 73);
  //passage dans l'autre hémisphère
  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 90, 60, 73);
  //position de saisie droite
  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 180, 60, 73);
  //se penche vers le verre
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 180, 60, 73);
  //rectifie position de saisie
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 155, 60, 10);
  //saisie du verre
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 155, 60, 73);
  //Attend 1 seconde
  delay(1000);
  //revient en position droite
  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 120, 60, 73);
  //Attend 1 seconde
  delay(1000);
  //rotation M1 pour aller au verre principal
  Braccio.ServoMovement(40,           positions_ingredients[0], 90, 0, 120, 60, 73);

}

void reposer_ingredient(int numero_ingredient)
//Une fois l'ingrédient versé, permet de le reposer dans son emplacement d'origine
{
  int angle_consigne = positions_ingredients[numero_ingredient];
  Serial.println(angle_consigne);
  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 120, 60, 73);
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 155, 60, 73);
  Braccio.ServoMovement(40,           angle_consigne, 45, 0, 155, 60, 10);
  Braccio.ServoMovement(40,           angle_consigne, 90, 0, 180, 60, 10);

}

void activationpompe(int temps)
{
  // Fonction permettant d'activer la pompe
  digitalWrite(relaipompePin, LOW); //relais on
  delay(temps);//délai à estimer selon le volume à remplir
  digitalWrite(relaipompePin, HIGH); //relais off
  delay(2000);// délai pour assurer la vidange du tuyau
}

void verser(int numero_ingredient, int qt_ingredient)
{
  switch (numero_ingredient)
  {
    case 9:
      temps = qt_ingredient * 1000 / 1.7;
      activationpompe(temps);
      break;
    default:
      temps = qt_ingredient * 1000 / 2 ;
      //verser
      Braccio.ServoMovement(40, 30, 90, 0, 120, 180, 73);
      //Attend (temps) millisecondes
      delay(temps);
      //redresse bouteille
      Braccio.ServoMovement(40, 30, 90, 0, 120, 60, 73);
      //Attend 1 seconde
      delay(1000);
      break;
  }
}

void interpreter_commande(int ID, bool ice)
//Interprète la commande utiisateur reçue par bluetooth
{
  Serial.println(ID);
  switch (ID)
  {

    case 1 : //cocktail RIO

      ajouter_ingredient(3, 3); // sirop de grenadine
      ajouter_ingredient(9, 11); //limonade
      ajouter_ingredient(4, 11); //jus d'orange
      break;

    case 2 : // Diabolo grenadine

      ajouter_ingredient(3, 3); // sirop de grenadine
      ajouter_ingredient(9, 22); //limonade
      break;

    case 3 : // Diabolo menthe

      ajouter_ingredient(2, 3); // sirop de menthe
      ajouter_ingredient(9, 22); // limonade
      break;

    case 4 : // Lemon mint

      ajouter_ingredient(2, 4); // sirop de menthe
      ajouter_ingredient(4, 5);//jus d'orange
      ajouter_ingredient(1, 4); //jus de citron
      break;

    case 5 : // Blanc Bec Menthe

      ajouter_ingredient(2, 4); // sirop de menthe
      ajouter_ingredient(1, 3); //jus de citron
      break;

    case 6 : // APRICOT SPARKLER

      ajouter_ingredient(5, 6); //jus d'abricot
      ajouter_ingredient(1, 2); //jus de citron
      ajouter_ingredient(9, 17); // limonade
      break;

    case 7 : //menthe à l'eau

      ajouter_ingredient(2, 4); // sirop de menthe
      ajouter_ingredient(7, 21); // eau
      break;

    case 8 : // Grenadine

      ajouter_ingredient(3, 4); // sirop de grenadine
      ajouter_ingredient(7, 21); // eau
      break;

    case 9 : // Verre de limonade

      ajouter_ingredient(9, 25); //limonade
      break;

    case 10 : // Verre d'eau

      ajouter_ingredient(7, 25); // eau
      break;
  }
}

int distance_ultrason()
{
  // Renvoie la distance mesurée par le capteur ultrason HC-SR04 dans le réservoir

  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  duration = pulseIn(echoPin, HIGH);
  distanceCm = duration * 0.034 / 2;
  return (distanceCm);
}

bool distance_ok()
{ // Permet de savoir si on peut lancer des commandes avec la pompe
  distanceCm = distance_ultrason();
  if (distanceCm <= 15) {
    lcd.setCursor(0, 2); // Sets the location at which subsequent text written to the LCD will be displayed
    lcd.print("Bouteille remplie   "); // Prints string "Distance" on the LCD
    lcd.setCursor(0, 3);
    lcd.print("                    ");
    return (true);
  }
  else if (distanceCm <= 21) {
    lcd.setCursor(0, 2);
    lcd.print("Bouteille           ");
    lcd.setCursor(0, 3);
    lcd.print("presque vide        ");
    return (true);
  }
  else if (distanceCm > 21)
  {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Bouteille vide      ");
    lcd.setCursor(0, 1);
    lcd.print("Veuillez recharger  ");
    lcd.setCursor(0, 2);
    lcd.print("en eau dans le      ");
    lcd.setCursor(0, 3);
    lcd.print("reservoir svp       ");
    return (false);
  }
  else
  {
    lcd.clear();
    lcd.print("Erreur distance     ");
  }
}

void buttonInterupt()
{
  // Fonction d'interruption ( il faut réduire le nombre de commandes)
  interruptTriggered = true;
  digitalWrite(5, HIGH); //Eteindre la pompe
}

void setup()
{
  // initialisation de l'écran LCD
  lcd.init(); // Initializes the interface to the LCD screen, and specifies the dimensions (width and height) of the display
  lcd.backlight();


  // initialisation Capteur ultrasons
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  // initialisation du Bouton
  pinMode(2, INPUT);
  attachInterrupt(0, buttonInterupt, RISING);

  pinMode(relaipompePin, OUTPUT); // déclaration du relais
  digitalWrite(relaipompePin, HIGH);

  // initialisation du Module bluetooth
  Serial.begin(9600);
  Serial.println("Waiting Bluetooth Connection...");
  lcd.setCursor(1, 2);
  lcd.print("Attente de Conn BT");
  hc06.begin(9600);

  while (true) //Tant que la connection Bluetooth n'a pas été établie
  {
    while (hc06.available() > 0) //On attend de recevoir au moins un octet de donnée
    {
      cmd += (char)hc06.read();
      delay(10);
    }
    if (cmd == "RCON")
    {
      hc06.write("01");
      delay(200);
      cmd = "";
      break;
    }
    delay(10);
  }
  lcd.setCursor(1, 2);
  lcd.print("Connection etablie !");
  jouer_chant_OM();
  Braccio.begin();
}



void loop()
{
  if (interruptTriggered)
  {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("EMERGENCY");
    lcd.setCursor(0, 1);
    lcd.print("STOP");
    lcd.setCursor(0, 2);
    lcd.print("EMERGENCY");
    lcd.setCursor(0, 3);
    lcd.print("STOP");

    while (1); // Bloque la arduino
  }

  donnees_bt = recuperation_donnees_bluetooth();
  switch (donnees_bt.array[0])
  {
    case 0:
      Serial.println("Cocktail Command Received !!");
      if (distance_ok())
      {
        interpreter_commande(donnees_bt.array[1], donnees_bt.array[2]);
      }
      break;
    case 1:
      Serial.println("New Settings Received !!");
      delay(2000);
      break;
    default:
      Serial.println("No Data Received");
      break;
  }
}
