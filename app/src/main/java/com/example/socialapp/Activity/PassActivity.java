package com.example.socialapp.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.socialapp.Config.RetrofitInstance;
import com.example.socialapp.Config.RetrofitService;
import com.example.socialapp.R;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassActivity extends AppCompatActivity {
    private TextView tvVerification;
    private TextView tvCheck;
    private TextView tvTime;
    private EditText etEmail;
    private EditText etNum;
    private EditText etPassword;
    private EditText rePassword;
    private Button btnSend;
    private Button btnCheck;
    private Button btnConfirm;
    private LinearLayout llNum;
    private LinearLayout llPass;
    private LinearLayout llRepass;
    private boolean passCheck = false;
    private boolean emailCheck = false;
    private boolean numCheck = false;
    private String verifiNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        etEmail = findViewById(R.id.et_pass_email);
        etNum = findViewById(R.id.et_pass_number);
        etPassword = findViewById(R.id.et_pass_password);
        rePassword = findViewById(R.id.et_pass_repassword);
        tvVerification = findViewById(R.id.tv_pass_verification);
        tvCheck = findViewById(R.id.tv_pass_check);
        tvTime = findViewById(R.id.tv_pass_time);
        btnSend = findViewById(R.id.btn_pass_overlap);
        btnCheck = findViewById(R.id.btn_pass_check);
        btnConfirm = findViewById(R.id.btn_pass_confirm);
        llNum = findViewById(R.id.ll_pass_number);
        llPass = findViewById(R.id.ll_pass_pass);
        llRepass = findViewById(R.id.ll_pass_repass);


        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {    }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();

                if(password.length() > 7 && password.matches(".*[A-Z].*") && password.matches(".*\\d.*") && password.matches(".*[a-z].*")) {
                    tvVerification.setText("안 전");
                    tvVerification.setTextColor(Color.GREEN);
                } else if(password.length() > 7 && password.matches(".*\\d.*") && password.matches(".*[a-z].*")) {
                    tvVerification.setText("적 정");
                    tvVerification.setTextColor(Color.parseColor("#625F0D"));
                } else if(password.length() > 7 && password.matches(".*\\d.*")) {
                    tvVerification.setText("위 험");
                    tvVerification.setTextColor(Color.RED);
                } else {
                    tvVerification.setText("사용 불가");
                    tvVerification.setTextColor(Color.RED);
                }

                if(password.length() == 0) {
                    tvVerification.setText("");
                }

            }

            @Override
            public void afterTextChanged(Editable s) {  }
        });

        rePassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {    }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String rePass = s.toString();
                String password = etPassword.getText().toString().trim();
                passCheck = false;

                if(rePass.equals(password) && (tvVerification.getText().equals("적 정") || tvVerification.getText().equals("위 험") || tvVerification.getText().equals("안 전"))) {
                    tvCheck.setText("일 치");
                    tvCheck.setTextColor(Color.GREEN);
                    passCheck = true;
                } else {
                    tvCheck.setText("불 일 치");
                    tvCheck.setTextColor(Color.RED);
                }

                if(password.length() == 0) {
                    tvVerification.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {  }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                if(!email.equals("") && email.contains("@")) {
                    btnSend.setText("재전송");
                    llNum.setVisibility(View.VISIBLE);
                    getNum(email);
                    countDown(180);
                }else {
                    etEmail.setText("");
                    Toast.makeText(PassActivity.this, "올바른 이메일 형식을 입력하세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = etNum.getText().toString().trim();
                if(verifiNum.equals(num) && numCheck) {
                    llNum.setVisibility(View.GONE);
                    llPass.setVisibility(View.VISIBLE);
                    llRepass.setVisibility(View.VISIBLE);

                    btnSend.setText("인증 확인");
                    btnSend.setTextColor(Color.parseColor("#00FC3B"));
                    btnSend.setEnabled(false);
                    etEmail.setEnabled(false);
                    emailCheck = true;
                    Toast.makeText(PassActivity.this, "인증 확인", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PassActivity.this, "인증번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if( email.equals("") || password.equals("")) {
                    Toast.makeText(PassActivity.this, "모든 빈칸을 채우세요.", Toast.LENGTH_SHORT).show();
                } else if(passCheck == false || emailCheck == false) {
                    Toast.makeText(PassActivity.this, "비밀번호와 인증을 확인하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    putPassword(email, password);
                    finish();
                }
            }
        });
    }

    private void putPassword(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        Call<Void> call = retrofitService.putPass(body);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()){
                    Toast.makeText(PassActivity.this, "변경 완료", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("PassActivity", "Failed to change password");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("PassActivity", "Failed to send email");
            }
        });
    }

    private void getNum(String email) {
        int num = (int) (100000 + Math.random() * 900000);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("number", String.valueOf(num));

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        Call<ResponseBody> call = retrofitService.sendEmail(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    verifiNum = String.valueOf(num);
                } else {
                    Log.e("PassActivity", "Failed to send email");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("PassActivity", "Failed to send email");
            }
        });
    }

    private void countDown(int second) {
        long duration = second * 1000;
        new CountDownTimer(duration, 1000) {

            public void onTick(long millisUntilFinished) {
                numCheck = true;
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;

                String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
                tvTime.setText(timeLeftFormatted);
            }

            public void onFinish() {
                tvTime.setText("시간 만료");
                numCheck = false;
            }
        }.start();
    }
}
