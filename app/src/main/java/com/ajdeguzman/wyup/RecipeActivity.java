package com.ajdeguzman.wyup;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {

    RecyclerView recyRecipes;
    private ProgressDialog pDialog;
    private ArrayList<String> mArrRecipeImage = new ArrayList<>();
    private ArrayList<String> mArrRecipeName = new ArrayList<>();
    final String TAG = "Recipe";
    private String strIngredients;
    private RecipeActivity.MenuItemAdapter adapter = new MenuItemAdapter(RecipeActivity.this, feedListContent(mArrRecipeImage), recyRecipes);
    private ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);
        recyRecipes = findViewById(R.id.recyRecipes);
       actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        Bundle extras;
        extras = getIntent().getExtras();

        if (extras != null) {
            strIngredients = extras.getString("str_ingredients");
            loadRecipes(strIngredients);
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void loadRecipes(String strIngredients) {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading Recipes...");
        pDialog.setCancelable(false);
        pDialog.show();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        CustomJsonRequest obreq = new CustomJsonRequest(Request.Method.GET, Credentials.SPOONACULAR.API
                                                        + "?apiKey=" + Credentials.SPOONACULAR.API_KEY
                                                        + "&ingredients=" + strIngredients, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        pDialog.dismiss();
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            JSONArray arrRecipes = response;
                            clearArray();
                            if(arrRecipes.length() == 0){
                                actionBar.setTitle("No recipe found");
                            }else{

                                actionBar.setTitle("Found " + arrRecipes.length() + " recipes");
                            }
                            for (int i = 0; i < arrRecipes.length(); i++) {
                                JSONObject objFields = arrRecipes.getJSONObject(i);
                                mArrRecipeName.add(objFields.getString("title"));
                                mArrRecipeImage.add(objFields.getString("image"));
                            }
                            if (recyRecipes.getAdapter() == null) {
                                adapter = new MenuItemAdapter(getApplicationContext(), feedListContent(mArrRecipeImage), recyRecipes);
                                recyRecipes.setAdapter(adapter);
                            } else {
                                adapter = ((RecipeActivity.MenuItemAdapter) recyRecipes.getAdapter());
                                adapter.resetData(feedListContent(mArrRecipeImage));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        requestQueue.add(obreq);
    }


    private static class RecipeClass implements Serializable {
        String recipe_image;
        String pres_instructions;
        String pres_date;
        String recipe_title;
    }

    private void clearArray() {
        mArrRecipeImage.clear();
        mArrRecipeName.clear();
    }
    private List<RecipeClass> feedListContent(ArrayList mArrayPrescriptionID) {
        List<RecipeActivity.RecipeClass> result = new ArrayList<>();
        for (int i = 0; i < mArrayPrescriptionID.size(); i++) {
            RecipeActivity.RecipeClass ci = new RecipeClass();
            ci.recipe_title = mArrRecipeName.get(i);
            ci.recipe_image = mArrRecipeImage.get(i);
            result.add(ci);
        }
        return result;
    }

    private static class MenuItemAdapter extends RecyclerView.Adapter {
        private final Context mContext;
        private final List<RecipeActivity.RecipeClass> prescriptionList;
        private final RecyclerView mRecyPresc;

        MenuItemAdapter(Context applicationContext, List<RecipeActivity.RecipeClass> prescriptionList, RecyclerView mRecyPrescriptions) {
            this.mContext = applicationContext;
            this.prescriptionList = prescriptionList;
            this.mRecyPresc = mRecyPrescriptions;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_layout, parent, false);
            return new MenuListHolder(rowView);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder hold, int position) {
            final RecipeActivity.RecipeClass ci = prescriptionList.get(position);
            RecipeActivity.MenuItemAdapter.MenuListHolder hldr = ((RecipeActivity.MenuItemAdapter.MenuListHolder) hold);
            hldr.lblRecipe.setText(ci.recipe_title);
            Glide.with(mContext)
                    .load(ci.recipe_image)
                    .centerCrop()
                    .into(hldr.imgRecipe);

        }

        @Override
        public int getItemCount() {
            return prescriptionList.size();
        }

        static class MenuListHolder extends RecyclerView.ViewHolder {
            final ImageView imgRecipe;
            final TextView lblRecipe;
            MenuListHolder(View itemView) {
                super(itemView);
                imgRecipe = itemView.findViewById(R.id.imgRecipe);
                lblRecipe = itemView.findViewById(R.id.recipeTitle);
            }
        }

        void resetData(List<RecipeActivity.RecipeClass> listResto) {
            this.prescriptionList.clear();
            this.prescriptionList.addAll(listResto);
            notifyDataSetChanged();
        }
    }
}