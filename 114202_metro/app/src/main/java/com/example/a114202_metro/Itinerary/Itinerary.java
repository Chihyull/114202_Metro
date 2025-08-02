package com.example.a114202_metro.Itinerary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a114202_metro.MainActivity;
import com.example.a114202_metro.R;

public class Itinerary extends AppCompatActivity {

    Button btn_create;
    CardView cardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_create = findViewById(R.id.btn_create);
        cardView = findViewById(R.id.cardView);

        btn_create.setOnClickListener(v -> {
            Intent intent = new Intent(Itinerary.this, ItinerarySetting.class);
            startActivity(intent);
        });

        boolean showCard = getIntent().getBooleanExtra("showCardView", false);

        if (showCard) {
            cardView.setVisibility(View.VISIBLE);
            findViewById(R.id.bag).setVisibility(View.GONE);
            findViewById(R.id.content2).setVisibility(View.GONE);
            findViewById(R.id.content3).setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            findViewById(R.id.bag).setVisibility(View.VISIBLE);
            findViewById(R.id.content2).setVisibility(View.VISIBLE);
            findViewById(R.id.content3).setVisibility(View.VISIBLE);
        }


    }

}
