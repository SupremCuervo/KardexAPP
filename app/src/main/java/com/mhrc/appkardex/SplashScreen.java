package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

	private static final int SPLASH_DURATION = 3000; // 3 segundos

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		// Iniciar animaciones
		startAnimations();

		// Esperar 3 segundos y luego ir a SelectionScreen
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			Intent intent = new Intent(SplashScreen.this, SelectionScreen.class);
			startActivity(intent);
			finish();
		}, SPLASH_DURATION);
	}

	private void startAnimations() {
		// Animación del título
		android.widget.TextView tvTitle = findViewById(R.id.tvSplashTitle);
		if (tvTitle != null) {
			Animation titleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
			tvTitle.startAnimation(titleAnim);
		}

		// Obtener referencias a los puntos
		ImageView[] dots = {
			findViewById(R.id.ivDot1),
			findViewById(R.id.ivDot2),
			findViewById(R.id.ivDot3)
		};

		// Aplicar animaciones con retrasos escalonados
		for (int i = 0; i < dots.length; i++) {
			if (dots[i] != null) {
				Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
				pulseAnim.setStartOffset(i * 200); // 200ms de retraso entre cada punto
				pulseAnim.setRepeatCount(Animation.INFINITE);
				dots[i].startAnimation(pulseAnim);
			}
		}
	}
}
