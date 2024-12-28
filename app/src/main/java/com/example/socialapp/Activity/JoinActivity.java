package com.example.socialapp.Activity;

import android.app.DatePickerDialog;
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
import com.example.socialapp.Entity.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class JoinActivity extends AppCompatActivity {
    private TextView tvBirth;
    private TextView tvTime;
    private TextView tvPassVerifi;
    private TextView tvPassCheck;
    private EditText etEmail;
    private EditText nickName;
    private EditText etPassword;
    private EditText rePassword;
    private EditText etNum;
    private Button btnBirth;
    private Button btnOverlap;
    private Button btnCheck;
    private Button confirmation;
    private LinearLayout llNum;
    private boolean numCheck = false;
    private boolean emailCheck = false;
    private boolean passCheck = false;
    private String verifiNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        nickName = findViewById(R.id.et_nickname);
        tvBirth = findViewById(R.id.tv_birth);
        tvTime = findViewById(R.id.tv_join_time);
        btnBirth = findViewById(R.id.btn_birth);
        etEmail = findViewById(R.id.et_join_email);
        etNum = findViewById(R.id.et_join_number);
        btnOverlap = findViewById(R.id.btn_overlap);
        btnCheck = findViewById(R.id.btn_number_check);
        tvPassVerifi = findViewById(R.id.tv_join_verification);
        tvPassCheck = findViewById(R.id.tv_join_check);
        etPassword = findViewById(R.id.et_join_password);
        rePassword = findViewById(R.id.et_join_repassword);
        confirmation = findViewById(R.id.btn_confirm);
        llNum = findViewById(R.id.ll_number);

        btnBirth.setOnClickListener(v -> showDatePickerDialog());

        etPassword.addTextChangedListener(new TextWatcher() {       // 페스워드 검증(8자 이상, 대소문자, 특수기호)
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {    }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();

                if(password.length() > 7 && password.matches(".*[A-Z].*") && password.matches(".*\\d.*") && password.matches(".*[a-z].*")) {
                    tvPassVerifi.setText("안 전");
                    tvPassVerifi.setTextColor(Color.GREEN);
                } else if(password.length() > 7 && password.matches(".*\\d.*") && password.matches(".*[a-z].*")) {
                    tvPassVerifi.setText("적 정");
                    tvPassVerifi.setTextColor(Color.parseColor("#625F0D"));
                } else if(password.length() > 7 && password.matches(".*\\d.*")) {
                    tvPassVerifi.setText("위 험");
                    tvPassVerifi.setTextColor(Color.RED);
                } else {
                    tvPassVerifi.setText("사용 불가");
                    tvPassVerifi.setTextColor(Color.RED);
                }

                if(password.length() == 0) {
                    tvPassVerifi.setText("");
                }

            }

            @Override
            public void afterTextChanged(Editable s) {  }
        });

        rePassword.addTextChangedListener(new TextWatcher() {       // 비밀번호 재확인
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {    }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String rePass = s.toString();
                String password = etPassword.getText().toString().trim();
                passCheck = false;

                if(rePass.equals(password) && (tvPassVerifi.getText().equals("적 정") || tvPassVerifi.getText().equals("위 험") || tvPassVerifi.getText().equals("안 전"))) {
                    tvPassCheck.setText("일 치");
                    tvPassCheck.setTextColor(Color.GREEN);
                    passCheck = true;
                } else {
                    tvPassCheck.setText("불 일 치");
                    tvPassCheck.setTextColor(Color.RED);
                }

                if(password.length() == 0) {
                    tvPassVerifi.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {  }
        });

        btnOverlap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                if(!email.equals("") && email.contains("@")) {
                    checkEmail(email);
                }else {
                    etEmail.setText("");
                    Toast.makeText(JoinActivity.this, "올바른 이메일 형식을 입력하세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = etNum.getText().toString().trim();
                if(verifiNum.equals(num) && numCheck) {
                    llNum.setVisibility(View.GONE);
                    btnOverlap.setText("인증 확인");
                    btnOverlap.setTextColor(Color.parseColor("#00FC3B"));
                    btnOverlap.setEnabled(false);
                    etEmail.setEnabled(false);
                    emailCheck = true;
                    Toast.makeText(JoinActivity.this, "인증 확인", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(JoinActivity.this, "인증번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        confirmation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nickName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if(name.equals("") || email.equals("") || password.equals("")) {
                    Toast.makeText(JoinActivity.this, "모든 빈칸을 채우세요.", Toast.LENGTH_SHORT).show();
                } else if(passCheck == false || emailCheck == false) {
                    Toast.makeText(JoinActivity.this, "비밀번호와 인증을 확인하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    postUser(name, email, password);
                    finish();
                }
            }
        });
    }

    private void checkEmail(String email) {     // 사용할 수 이메일 확인
        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<ResponseBody> call = retrofitService.getEmail(email);
        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        String getMail = null;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            getMail = jsonObject.optString("email", null);
                        }

                        if (getMail != null && getMail.equals(email)) {
                            etEmail.setText("");
                            Toast.makeText(JoinActivity.this, "중복된 이메일입니다. 다시 입력해주세요.", Toast.LENGTH_SHORT).show();
                        } else {
                            btnOverlap.setText("재전송");
                            llNum.setVisibility(View.VISIBLE);
                            getNum(email);
                            countDown(180);
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e("JoinActivity", "Error parsing JSON", e);
                    }
                } else {
                    Log.e("JoinActivity", "Server responded with error code: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("JoinActivity", "Failed to load email", t);
            }
        });
    }

    private void postUser(String name, String email, String password) {     // 회원가입 성공 시(유저 추가)
        User user = new User(name, email, password);

        RetrofitService retrofitService = RetrofitInstance.getRetrofitInstance().create(RetrofitService.class);
        retrofit2.Call<Void> call = retrofitService.joinUser(user);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("JoinActivity", "User created successfully");
                    finish(); // 액티비티 종료
                } else {
                    Log.e("JoinActivity", "Server responded with error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });

    }

    private void getNum(String email) {     // 인증번호 전송
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
                    Log.e("JoinActivity", "Failed to send email");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("JoinActivity", "Failed to send email");
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

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                JoinActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = year1 + "년" + (monthOfYear + 1) + "월" + dayOfMonth + "일";
                    tvBirth.setText(selectedDate);
                },
                year, month, day);

        datePickerDialog.show();
    }
}
