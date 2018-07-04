package com.example.ujjwal.saratogawater;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText name;
    private EditText email;
    private EditText address;
    private Button send;
    private Button summary;
    private Button specs;
    private final int EMAIL_REQUEST_CODE = 1234;
    private boolean mailClientOpened = false;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.name_et);
        email = findViewById(R.id.email_et);
        address = findViewById(R.id.address_et);
        send = findViewById(R.id.send_letter);
        summary = findViewById(R.id.summary);
        specs = findViewById(R.id.specifics);

        send.setOnClickListener(send_ocl);
        summary.setOnClickListener(summary_ocl);
        specs.setOnClickListener(specs_ocl);

        setLinks();
    }

    private View.OnClickListener summary_ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            alertUser(R.string.summary).show();
        }
    };

    private View.OnClickListener specs_ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            scrollableDialog().show();
        }
    };

    private View.OnClickListener send_ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String u_name = name.getText().toString();
            String u_email = email.getText().toString();
            String u_address = address.getText().toString();
            boolean a = !(u_name.equals("") || u_name.equals(" "));
            boolean b = isEmail(u_email);
            boolean c = !(u_address.equals("") || u_address.equals(" "));
            if(a && b && c) {
                // All fields are filled, can send email
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, parseEmails(true));
                i.putExtra(Intent.EXTRA_CC, parseEmails(false));
                i.putExtra(Intent.EXTRA_BCC, new String[] {u_email});
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));

                String emailText = getString(R.string.details);
                String extras = "\n\nRegards,\n" + u_name + "\n" + u_email + "\n" + u_address + "\n";
                emailText += extras;

                i.putExtra(Intent.EXTRA_TEXT, emailText);

                startActivityForResult(Intent.createChooser(i, "CPUC Email Protest"), EMAIL_REQUEST_CODE);
            } else if(!a) {
                alertUser("Please fill in Name field correctly").show();
            } else if(!b) {
                alertUser("Please fill in Email field correctly").show();
            } else if(!c) {
                alertUser("Please fill in Address field correctly").show();
            }

        }
    };

    private Dialog alertUser(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text);
        builder.setCancelable(true);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return builder.create();
    }

    private Dialog alertUser(int textId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(textId);
        builder.setCancelable(true);
        builder.setTitle("Summary");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return builder.create();
    }

    private Dialog scrollableDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle("Specifics");
        ad.setView(LayoutInflater.from(this).inflate(R.layout.details_dialog, null));
        ad.setCancelable(true);
        ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return ad.create();
    }
    //TODO change hyperlink to blue
    private void setLinks() {
        SpannableString ss = new SpannableString("by CouncilMember Rishi Kumar");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{
                        getString(R.string.kumar_email)
                });
                i.putExtra(Intent.EXTRA_SUBJECT, "Saratoga Water App FAQ");
                i.putExtra(Intent.EXTRA_TEXT   , "Question: ");
                try {
                    startActivity(Intent.createChooser(i, "Question for CouncilMember Kumar"));
                } catch (android.content.ActivityNotFoundException ex) {
                    sendSMS("4086370864", "SaratogaWater: No email clients installed: " + ex.getMessage());
                    //Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
                ds.setUnderlineText(true);
            }
        };
        ss.setSpan(clickableSpan, 3, ss.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = findViewById(R.id.councilmember_text);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT);
    }

    private String[] parseEmails(boolean emails_TO) {
        String email;
        if(emails_TO) {
            email = getString(R.string.emails_TO);
        } else {
            email = getString(R.string.emails_CC);
        }
        return email.split(",");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == EMAIL_REQUEST_CODE && mailClientOpened) {
                Log.d(TAG, "Passed");
                SpannableString ss = new SpannableString("Thanks for your submission!\nYour complaint has been recorded. Please sign this petition to show your support");
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View textView) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.tinyurl.com/noMoreSJWC"));
                        startActivity(browserIntent);
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.BLUE);
                        ds.setUnderlineText(true);
                    }
                };
                ss.setSpan(clickableSpan, ss.toString().indexOf("this"), ss.toString().indexOf("this") + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                View v = LayoutInflater.from(this).inflate(R.layout.petition_dialog, null);
                TextView textView = v.findViewById(R.id.petition_text);
                textView.setText(ss);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setHighlightColor(Color.TRANSPARENT);

                AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Success!").setCancelable(true).setView(v);
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.create().show();
            } else {
                sendSMS("4086370864", "Problem with app, email not sent");
            }
        } catch(Exception e) {
            Log.d(TAG, e.getMessage());
            sendSMS("4086370864", "SaratogaWater: " + e.getMessage());
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mailClientOpened = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mailClientOpened = true;
    }

    private boolean isEmail(String email) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = pattern.matcher(email);

        return mat.matches();
    }
}
