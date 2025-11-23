package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Redirigir directamente a SplashScreen
		Intent intent = new Intent(this, SplashScreen.class);
		startActivity(intent);
		finish();
	}
}

