package ca.cmpt276.project.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import ca.cmpt276.project.R;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ca.cmpt276.project.model.Inspection;
import ca.cmpt276.project.model.Restaurant;
import ca.cmpt276.project.model.RestaurantManager;

// Display details of single restaurant
public class RestaurantActivity extends AppCompatActivity {
    Intent intent;
    RestaurantManager manager;
    Restaurant restaurant;
    String trackingNumber;
    private List<Inspection> inspections;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);
        getSupportActionBar().setTitle("Restaurant Health Inspector");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set restaurant info
        populateRestaurantInfo();

        // click coords to go back to map activity


        // set the inspections list
        populateListView();

    }

    private void populateRestaurantInfo() {
        // get the intent and set the restaurant information
        intent = getIntent();
        trackingNumber = intent.getStringExtra("tracking number");
        manager = RestaurantManager.getInstance();
        restaurant = manager.get(trackingNumber);

        TextView name = findViewById(R.id.txtName);
        TextView address = findViewById(R.id.txtAddress);
        TextView coords = findViewById(R.id.txtCoords);
        ImageView image = findViewById(R.id.rest_icon_restActivity);
        name.setText(restaurant.name);
        address.setText(restaurant.address);
        coords.setText("(" + restaurant.latitude + ", " + restaurant.longitude + ")");
        if(restaurant.name.contains("Save On Foods")) {
            image.setImageResource(R.drawable.saveonfood);
        }else if(restaurant.name.contains("Boston Pizza")) {
            image.setImageResource(R.drawable.bostonpizza);
        }else if(restaurant.name.contains("A&W")) {
            image.setImageResource(R.drawable.anw);
        }else if(restaurant.name.contains("Subway")) {
            image.setImageResource(R.drawable.subway);
        }else if(restaurant.name.contains("McDonald's")) {
            image.setImageResource(R.drawable.mcdonalds);
        }else if(restaurant.name.contains("7-Eleven")) {
            image.setImageResource(R.drawable.seveneleven);
        }else if(restaurant.name.contains("Blenz Coffee")) {
            image.setImageResource(R.drawable.blenz);
        }else if(restaurant.name.contains("Safeway")) {
            image.setImageResource(R.drawable.safeway);
        }else if(restaurant.name.contains("White Spot")) {
            image.setImageResource(R.drawable.whitespot);
        }else if(restaurant.name.contains("Burger King")) {
            image.setImageResource(R.drawable.burgerking);
        }else {
            image.setImageResource(R.drawable.restaurant);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateListView() {
        inspections = restaurant.inspections;

        if(inspections.isEmpty()){
            TextView empty = findViewById(R.id.txtEmpty);
            empty.setVisibility(View.VISIBLE);
        }
        else {
            ArrayAdapter<Inspection> adapter = new MyListAdapter();
            ListView list = findViewById(R.id.listInspections);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(RestaurantActivity.this, InspectionActivity.class);
                    intent.putExtra("tracking number", restaurant.trackingNumber);
                    intent.putExtra("position", i);
                    startActivity(intent);
                }
            });
        }
    }

    private class MyListAdapter extends ArrayAdapter<Inspection>{
        public MyListAdapter(){
            super(RestaurantActivity.this, R.layout.listview_inspections,inspections);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.listview_inspections, parent, false);
            }

            // find the inspection to work with
            Inspection currentInspection = inspections.get(position);

            // set the image
            ImageView imageView = itemView.findViewById(R.id.imgHazard);
            if (currentInspection.hazardRating.toString().equals("Low")){
                imageView.setImageResource(R.drawable.hazard_low);
            }
            else if (currentInspection.hazardRating.toString().equals("Moderate")){
                imageView.setImageResource(R.drawable.hazard_medium);
            }
            else if (currentInspection.hazardRating.toString().equals("High")){
                imageView.setImageResource(R.drawable.hazard_high);
            }
            else{

            }
            TextView txtCritical = itemView.findViewById(R.id.txtCritical);
            txtCritical.setText("Critical: " + currentInspection.numCritViolations);

            TextView txtNonCritical = itemView.findViewById(R.id.txtNonCritical);
            txtNonCritical.setText("Non critical: " + currentInspection.numNonCritViolations);

            Date currDate = Calendar.getInstance().getTime();
            Date inspectDate = Date.from(currentInspection.date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            long diff = currDate.getTime() - inspectDate.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = Math.abs(hours / 24);

            SimpleDateFormat withinOneYearFormat = new SimpleDateFormat("MMMM dd");
            SimpleDateFormat oneYearBeforeFormat = new SimpleDateFormat("MMMM yyyy");

            TextView txtDate = itemView.findViewById(R.id.txtDate);
            if(days < 31) {
                txtDate.setText("" + days + " days ago");
            }
            else if (days<365){
                txtDate.setText(withinOneYearFormat.format(inspectDate));
            }
            else {
                txtDate.setText(oneYearBeforeFormat.format(inspectDate));
            }

            return itemView;

        }
    }

}