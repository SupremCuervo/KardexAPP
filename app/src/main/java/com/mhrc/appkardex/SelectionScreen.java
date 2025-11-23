package com.mhrc.appkardex;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SelectionScreen extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selection);

		Button btnRegistered = findViewById(R.id.btnRegistered);
		TextView btnRegister = findViewById(R.id.btnRegister);

		// Aplicar subrayado al TextView
		String text = btnRegister.getText().toString();
		SpannableString spannableString = new SpannableString(text);
		spannableString.setSpan(new UnderlineSpan(), 0, text.length(), 0);
		btnRegister.setText(spannableString);

		// Botón "¿Ya estás inscrito?" -> Login
		btnRegistered.setOnClickListener(v -> {
			Intent intent = new Intent(SelectionScreen.this, SignInScreen.class);
			startActivity(intent);
		});

		// Botón "Inscribirse" -> Selección de tipo de usuario
		btnRegister.setOnClickListener(v -> {
			Intent intent = new Intent(SelectionScreen.this, UserTypeSelectionActivity.class);
			startActivity(intent);
		});
	}
}

