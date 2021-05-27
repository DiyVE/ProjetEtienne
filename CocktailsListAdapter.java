package com.tiee.etienne;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class CocktailsListAdapter  extends BaseAdapter {

    private List<Cocktails> listData;
    private LayoutInflater layoutInflater;
    private Context context;

    public CocktailsListAdapter(Context aContext,  List<Cocktails> listData) {
        this.context = aContext;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.cocktails_item_layout, null);
            holder = new ViewHolder();
            holder.imgView = (ImageView) convertView.findViewById(R.id.cocktailImg);
            holder.cocktailNameView = (TextView) convertView.findViewById(R.id.cocktailNameTextView);
            holder.infosNameView = (TextView) convertView.findViewById(R.id.cocktailInfosTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Cocktails country = this.listData.get(position);
        holder.cocktailNameView.setText(country.getCocktailName());
        holder.infosNameView.setText("Informations: " + country.getInformations());

        int imageId = this.getMipmapResIdByName(country.getImgName());

        holder.imgView.setImageResource(imageId);

        return convertView;
    }

    // Find Image ID corresponding to the name of the image (in the directory mipmap).
    public int getMipmapResIdByName(String resName)  {
        String pkgName = context.getPackageName();
        // Return 0 if not found.
        int resID = context.getResources().getIdentifier(resName , "mipmap", pkgName);
        Log.i("CustomListView", "Res Name: "+ resName+"==> Res ID = "+ resID);
        return resID;
    }

    static class ViewHolder {
        ImageView imgView;
        TextView cocktailNameView;
        TextView infosNameView;
    }

}
