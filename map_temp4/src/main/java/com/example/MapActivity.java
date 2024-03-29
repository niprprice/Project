package com.example;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlacesClient placeClient;
    private List<AutocompletePrediction> predictionList;

    private Location mLastKnowLocation;
    private LocationCallback locationCallback;

    private MaterialSearchBar materialSearchBar;
    private View mapView;
    private Button btnFilter;

    private final float DEFAULT_ZOOM = 18;

   /**/
    public DatabaseAccess access;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        materialSearchBar = findViewById(R.id.searchBar);
        btnFilter = findViewById(R.id.btn_filter);

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mapView = mapFragment.getView();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);
        Places.initialize(MapActivity.this, "AIzaSyC23Nfih07UvEoosLJ7f6k148YtxSReE-4");
        placeClient = Places.createClient(this);
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        ImageView floatingCloseButton = findViewById(R.id.mt_clear);
        floatingCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if(id == R.id.mt_clear){
                    if(materialSearchBar.isSuggestionsVisible()){
                        materialSearchBar.clearSuggestions();
                    }
                    if(materialSearchBar.isSearchEnabled()){
                        materialSearchBar.disableSearch();
                    }
                    Toast.makeText(MapActivity.this, "the close button is clicked", Toast.LENGTH_SHORT).show();
                }

            }
        });
        getAccess();

        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //change this to search inside app
                startSearch(text.toString(), true, null, true);
                Toast.makeText(MapActivity.this, "search confirmed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if(buttonCode == MaterialSearchBar.BUTTON_NAVIGATION){
                    //opening or closing a navigation drawer
                }else if(buttonCode == MaterialSearchBar.BUTTON_BACK){
                    materialSearchBar.disableSearch();
//                    materialSearchBar.clearSuggestions();
                }
            }
        });

        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        .setCountry("AU")
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(token)
                        .setQuery(s.toString())
                        .build();
                placeClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                        if(task.isSuccessful()){
                            FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                            if(predictionsResponse != null){
                                predictionList = predictionsResponse.getAutocompletePredictions();
                                List<String> suggestionsList = new ArrayList<>();
                                for(int i=0; i<predictionList.size(); i++){
                                    AutocompletePrediction prediction = predictionList.get(i);
                                    suggestionsList.add(prediction.getFullText(null).toString());
                                }
                                materialSearchBar.updateLastSuggestions(suggestionsList);
                                if(!materialSearchBar.isSuggestionsVisible()){
                                    materialSearchBar.showSuggestionsList();
                                }
                            }
                        }else{
                            Log.i("mytag", "prediction fetching task unsuccessful");
                        }
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        materialSearchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                if(position >= predictionList.size()){
                    return;
                }
                AutocompletePrediction selectedPrediction = predictionList.get(position);
                final String suggestion = materialSearchBar.getLastSuggestions().get(position).toString();
                materialSearchBar.setText(suggestion);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        materialSearchBar.clearSuggestions();
                    }
                }, 1000);


                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                if(imm != null){
                    imm.hideSoftInputFromWindow(materialSearchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                String placeId = selectedPrediction.getPlaceId();
                List<Place.Field> placeField = Arrays.asList(Place.Field.LAT_LNG);

                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeField).build();
                placeClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        Place place = fetchPlaceResponse.getPlace();
                        Log.i("mytag", "Place found: " + place.getName());
                        LatLng latLngOfPlace = place.getLatLng();
                        if(latLngOfPlace != null){
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOfPlace, DEFAULT_ZOOM));
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(e instanceof ApiException){
                            ApiException apiException = (ApiException) e;
                            apiException.printStackTrace();
                            int statusCode = apiException.getStatusCode();
                            Log.i("mytag", "Place not found: " + e.getMessage());
                            Log.i("mytag", "status code: " + statusCode);
                        }
                    }
                });
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {
            }

        });

    }


    public String[] getSa2() {
        String[] result = new String[28];
        result[0] = "sa2_main16";
        result[1] = "sa2_name16";
        result[2] = "homeless_pr100";
        result[3] ="rent_pr100";
        result[4] ="internet_access_from_dwelling";
        result[5] ="job_pr100";
        result[6] ="median_income";
        result[7] ="citizen_population";
        result[8] ="christian_pr100";
        result[9] ="buddhism_pr100";
        result[10] ="hinduism_pr100";
        result[11] = "judaism_pr100 ";
        result[12] ="islam_pr100";
        result[13] ="other_religion_pr100";
        result[14] ="immi_not_citizen";
        result[15] = "h_sold_price";
        result[16] = "h_sale_price";
        result[17] = "h_rent_Discount";
        result[18] = "h_rent_price";
        result[19] = "u_sold_price";
        result[20] =  "u_sale_price";
        result[21] = "u_rent_price";
        result[22] = "u_rent_discount";
        result[23] = "vehicle_num";
        result[24] = "business_num";
        result[25] = "comm_skill_pr100";
        result[26] = "health_mental";
        result[27] = "health_physical";
        return result;
    }

    public void getAccess(){
        access = DatabaseAccess.getInstance(getApplicationContext());
        access.open();
    }
    public String[] genColumn(int[] input){
        String[] columns = getSa2();
        int i; int num = 2;
        for(i=2;i<=27;i++){
            if (input[i] != 0){
                num++;
            }
        }
        String[] result = new String[num];
        result[0] = "sa2_main16";
        result[1] = "sa2_name16";
        int j = 2;
        for(i=2;i<=27;i++){
            if (input[i] != 0){
               result[j] = columns[i];
               j++;
            }
        }
        return result;
    }
    public String genWhere(int[] input){
        String result = "";
        if(input[2] != 0){
            int i = input[2];
            if (i>0) result = result + " AND homeless_pr100 >=" + i;
            else result = result + " AND homeless_pr100 <=" + i;
        }
        if(input[3] != 0){
            int i = input[3];
            if (i>0) result = result + " AND rent_pr100 >=" + i;
            else result = result + " AND rent_pr100 <=" + i;
        }
        if(input[4] != 0){
            int i = input[4];
            if (i>0) result = result + " AND internet_access_from_dwelling >=" + i;
            else result = result + " AND internet_access_from_dwelling <=" + i;
        }
        if(input[5] != 0){
            int i = input[5];
            if (i>0) result = result + " AND job_pr100 >=" + i;
            else result = result + " AND job_pr100 <=" + i;

        }
        if(input[6] != 0){
            int i = input[6];
            if (i>0) result = result + " AND median_income >=" + i;
            else result = result + " AND median_income <=" + i;

        }
        if(input[7] != 0){
            int i = input[7];
            if (i>0) result = result + " AND citizen_population >=" + i;
            else result = result + " AND citizen_population <=" + i;

        }
        if(input[8] != 0){
            int i = input[8];
            if (i>0) result = result + " AND christian_pr100 >=" + i;
            else result = result + " AND christian_pr100 <=" + i;

        }
        if(input[9] != 0){
            int i = input[9];
            if (i>0) result = result + "AND buddhism_pr100 >=" + i;
            else result = result + "AND buddhism_pr100 <=" + i;

        }
        if(input[10] != 0){
            int i = input[10];
            if (i>0) result = result + " AND hinduism_pr100 >=" + i;
            else result = result + " AND hinduism_pr100 <=" + i;

        }
        if(input[11] != 0){
            int i = input[11];
            if (i>0) result = result + " AND judaism_pr100 >=" + i;
            else result = result + " AND judaism_pr100 <=" + i;

        }
        if(input[12] != 0){
            int i = input[12];
            if (i>0) result = result + " AND islam_pr100 >=" + i;
            else result = result + " AND islam_pr100 <=" + i;

        }
        if(input[13] != 0){
            int i = input[13];
            if (i>0) result = result + " AND other_religion_pr100 >=" + i;
            else result = result + " AND other_religion_pr100 <=" + i;

        }
        if(input[14] != 0){
            int i = input[14];
            if (i>0) result = result + " AND immi_not_citizen >=" + i;
            else result = result + " AND immi_not_citizen <=" + i;

        }
        if(input[15] != 0){
            int i = input[15];
            if (i>0) result = result + " AND h_sold_price >=" + i;
            else result = result + " AND h_sold_price <=" + i;

        }
        if(input[16] != 0){
            int i = input[16];
            if (i>0) result = result + " AND h_sale_price >=" + i;
            else result = result + " AND h_sale_price <=" + i;

        }
        if(input[17] != 0){
            int i = input[17];
            if (i>0) result = result + " AND h_rent_Discount >=" + i;
            else result = result + " AND h_rent_Discount <=" + i;

        }
        if(input[18] != 0){
            int i = input[18];
            if (i>0) result = result + " AND h_rent_price >=" + i;
            else result = result + " AND h_rent_price <=" + i;

        }
        if(input[19] != 0){
            int i = input[19];
            if (i>0) result = result + " AND u_sold_price >=" + i;
            else result = result + " AND u_sold_price <=" + i;

        }
        if(input[20] != 0){
            int i = input[20];
            if (i>0) result = result + " AND u_sale_price >=" + i;
            else result = result + " AND u_sale_price <=" + i;

        }
        if(input[21] != 0){
            int i = input[21];
            if (i>0) result = result + " AND u_rent_price >=" + i;
            else result = result + " AND u_rent_price <=" + i;

        }
        if(input[22] != 0){
            int i = input[22];
            if (i>0) result = result + " AND u_rent_discount >=" + i;
            else result = result + " AND u_rent_discount <=" + i;

        }
        if(input[23] != 0){
            int i = input[23];
            if (i>0) result = result + " AND vehicle_num >=" + i;
            else result = result + " AND vehicle_num <=" + i;

        }
        if(input[24] != 0){
            int i = input[24];
            if (i>0) result = result + " AND business_num >=" + i;
            else result = result + " AND business_num <=" + i;

        }
        if(input[25] != 0){
            int i = input[25];
            if (i>0) result = result + " AND comm_skill_pr100 >=" + i;
            else result = result + " AND comm_skill_pr100 <=" + i;

        }
        if(input[26] != 0){
            int i = input[26];
            if (i>0) result = result + " AND health_mental >=" + i;
            else result = result + " AND health_mental <=" + i;

        }
        if(input[27] != 0){
            int i = input[27];
            if (i>0) result = result + " AND health_physical >=" + i;
            else result = result + " AND health_physical <=" + i;

        }
        return result;
    }
    public Cursor query(String[] columns, String where){
        return access.query(columns, where);
    }
    public String[][] returnResult(Cursor cursor){
        String[][] result = new String[cursor.getCount()][cursor.getColumnCount()];
        if(cursor!=null){
            cursor.moveToFirst();
        }
        int i =0;
        do{
            for(int j =0; j<= (cursor.getColumnCount()-1);j++){
                result[i][j] = cursor.getString(j);
            }
            i++;
        }while(cursor.moveToNext());
        return result;
    }








    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);  //enable the location button
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        //change the position of location button
        if(mapView != null && mapView.findViewById(Integer.parseInt("1")) != null){
            View locationButton = ((View)mapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)locationButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 40, 180);

        }

        //check if gps is enabled or not then request user to enable it
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(MapActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });

        task.addOnFailureListener(MapActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof ResolvableApiException){
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    try {
                        resolvable.startResolutionForResult(MapActivity.this, 51);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if(materialSearchBar.isSuggestionsVisible()){
                    materialSearchBar.clearSuggestions();
                }
                if(materialSearchBar.isSearchEnabled()){
                    materialSearchBar.disableSearch();
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 51){
            if(resultCode == RESULT_OK){
                getDeviceLocation();
            }
        }
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()){
                    mLastKnowLocation = task.getResult();
                    if(mLastKnowLocation != null){
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastKnowLocation.getLatitude(), mLastKnowLocation.getLongitude()),
                                DEFAULT_ZOOM));
                    }else{
                        LocationRequest locationRequest = LocationRequest.create();
                        locationRequest.setInterval(10000);
                        locationRequest.setFastestInterval(5000);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        locationCallback = new LocationCallback(){
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                if(locationResult == null){
                                    return;
                                }
                                mLastKnowLocation = locationResult.getLastLocation();
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnowLocation.getLatitude(), mLastKnowLocation.getLongitude()),
                                        DEFAULT_ZOOM));
                                mFusedLocationProviderClient.removeLocationUpdates(locationCallback); //don't know its use, maybe it makes app get location only once
                            }
                        };
                        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                }else{
                    Toast.makeText(MapActivity.this, "unalbe to get last location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}
