package minasedrak.shushme;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.places.PlaceBuffer;

/**
 * Created by MinaSedrak on 7/27/2017.
 */

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    private Context mContext;
    private PlaceBuffer mPlaces;


    public PlaceAdapter(Context mContext, PlaceBuffer mPlaces){
        this.mContext = mContext;
        this.mPlaces = mPlaces;
    }


    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootView = inflater.inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(rootView);
    }


    @Override
    public void onBindViewHolder(PlaceViewHolder holder, int position) {

        String placeName = mPlaces.get(position).getName().toString();
        String placeAddress = mPlaces.get(position).getAddress().toString();

        holder.mNameTv.setText(placeName);
        holder.mAddressTv.setText(placeAddress);


    }


    @Override
    public int getItemCount() {
        if(mPlaces == null){
            return 0;}

        return mPlaces.getCount();
    }


    public void swapPlaces(PlaceBuffer newPlaces) {
        mPlaces = newPlaces;

        if(mPlaces != null){
            // notify RecyclerView that data has been changed
            this.notifyDataSetChanged();
        }
    }


    class PlaceViewHolder extends RecyclerView.ViewHolder{

        TextView mNameTv;
        TextView mAddressTv;

        public PlaceViewHolder(View rootView) {
            super(rootView);
            mNameTv = (TextView) rootView.findViewById(R.id.place_name_TV);
            mAddressTv = (TextView) rootView.findViewById(R.id.place_address_TV);
        }
    }
}
