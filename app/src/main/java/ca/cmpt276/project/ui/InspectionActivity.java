package ca.cmpt276.project.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ca.cmpt276.project.R;
import ca.cmpt276.project.model.data.Inspection;
import ca.cmpt276.project.model.data.RestaurantDetails;
import ca.cmpt276.project.model.data.RestaurantManager;
import ca.cmpt276.project.model.data.Violation;
import ca.cmpt276.project.model.viewmodel.HealthViewModel;
import ca.cmpt276.project.model.viewmodel.HealthViewModelFactory;

public class InspectionActivity extends AppCompatActivity {
    String trackingNumber;
    Inspection inspection;
    int position;
    private List<Violation> violations;
    HealthViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewModelProvider.Factory factory = new HealthViewModelFactory(this);
        model = new ViewModelProvider(this, factory).get(HealthViewModel.class);


        // get intent and get necessary extras
        Intent intent = getIntent();
        trackingNumber = intent.getStringExtra("tracking number");
        position = intent.getIntExtra("position", 0);

        model.restaurantDetailsMap.observe(this, restaurantDetailsMap -> {
            populateInspection(restaurantDetailsMap);
            populateViolations(restaurantDetailsMap);
        });

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

    private void populateViolations(Map<String, RestaurantDetails> map) {
        violations = map.get(trackingNumber).inspectionDetailsList.get(position).violations;
        if(violations.isEmpty()){
            TextView empty = findViewById(R.id.txtEmpty);
            empty.setVisibility(View.VISIBLE);
            return;
        }

        ArrayAdapter <Violation> adapter = new MyListAdapter();
        ListView list = findViewById(R.id.listViolations);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // show exact description
                Toast.makeText(getApplicationContext(),
                        violations.get(i).description,
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private class MyListAdapter extends ArrayAdapter<Violation>{
        public MyListAdapter(){
            super(InspectionActivity.this, R.layout.listview_violations, violations);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.listview_violations, parent, false);
            }

            // find the inspection to work with
            Violation currentViolation = violations.get(position);

            // set the violation category image
            ImageView imageView = itemView.findViewById(R.id.imgCategory);
            TextView txt = itemView.findViewById(R.id.txtCategory);
            if (currentViolation.category.toString().equals("Food")){
                imageView.setImageResource(R.drawable.violation_food);
                txt.setText("Food");
            }
            else if (currentViolation.category.toString().equals("Pest")){
                imageView.setImageResource(R.drawable.violation_pest);
                txt.setText("Pest");
            }

            else if (currentViolation.category.toString().equals("Equipment")){
                imageView.setImageResource(R.drawable.violation_equipment);
                txt.setText("Equipment");
            }

            else if (currentViolation.category.toString().equals("Hygiene")){
                imageView.setImageResource(R.drawable.violation_hygiene);
                txt.setText("Hygiene");
            }

            else if (currentViolation.category.toString().equals("Training")){
                imageView.setImageResource(R.drawable.violation_training);
                txt.setText("Training");
            }

            else if (currentViolation.category.toString().equals("Logistics")){
                imageView.setImageResource(R.drawable.violation_logistics);
                txt.setText("Logistics");
            }

            String description = currentViolation.description;
            TextView txtDescription = itemView.findViewById(R.id.txtDescription);
            txtDescription.setText(description);


            boolean critical = currentViolation.isCritical;
            ImageView imgCategory = itemView.findViewById(R.id.imageView3);
            TextView txtCritical = itemView.findViewById(R.id.txtCriticalExtent);
            if(critical){
                imgCategory.setImageResource(R.drawable.icon_critical);
                txtCritical.setText("Critical");
                txtCritical.setTextColor(Color.RED);
            }
            else{
                imgCategory.setImageResource(R.drawable.icon_noncritical);
                txtCritical.setText("Non-critical");
                txtCritical.setTextColor(Color.rgb(255,165,0));
            }

            return itemView;

        }
    }

    private void populateInspection(Map<String, RestaurantDetails> map) {
        // find single inspection data
        inspection = map.get(trackingNumber).inspectionDetailsList.get(position).inspection;
        String hazardLevel = inspection.hazardRating.toString();
        String inspectionType = inspection.type.toString();
        SimpleDateFormat formatDate = new SimpleDateFormat("MMMM dd, yyyy");
        Date date = Date.from(inspection.date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        int critical = inspection.numCritViolations;
        int nonCritical = inspection.numNonCritViolations;

        //
        ImageView image = findViewById(R.id.imgHazard);
        if (hazardLevel.equals("Low")){
            image.setImageResource(R.drawable.hazard_low);
        }
        else if (hazardLevel.equals("Moderate")){
            image.setImageResource(R.drawable.hazard_medium);
        }
        else if (hazardLevel.equals("High")){
            image.setImageResource(R.drawable.hazard_high);
        }
        else {
            //image.setImageResource(R.drawable.hazard_high);
        }

        TextView txtType = findViewById(R.id.txtName);
        TextView txtDate = findViewById(R.id.txtDate);
        TextView txtCritical = findViewById(R.id.txtCritical);
        TextView txtNonCritical = findViewById(R.id.txtNonCritical);

        txtType.setText(inspectionType);
        txtDate.setText(formatDate.format(date));
        txtCritical.setText("Critical: " + critical);
        txtNonCritical.setText("Non-critical: " + nonCritical);
    }
}
