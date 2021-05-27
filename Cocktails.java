package com.tiee.etienne;

public class Cocktails
{
    private String cocktailName;
    private int cocktailID;
    private String imgName;
    private String informations;

    public Cocktails(String cocktailName, int cocktailID, String imgName, String informations)
    {
        this.cocktailName = cocktailName;
        this.imgName = imgName;
        this.informations = informations;
        this.cocktailID = cocktailID;
    }

    public String getCocktailName()
    {
        return this.cocktailName;
    }

    public String getInformations()
    {
        return this.informations;
    }

    public String getImgName()
    {
        return this.imgName;
    }

    public int getCocktailID()
    {
        return this.cocktailID;
    }
}
