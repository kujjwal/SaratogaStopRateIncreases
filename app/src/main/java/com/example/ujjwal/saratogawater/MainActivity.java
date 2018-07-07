package com.example.ujjwal.saratogawater;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText name;
    private EditText email;
    private EditText address;
    private final int EMAIL_REQUEST_CODE = 1234;
    private boolean mailClientOpened = false;
    private static final String PHONE_NUMBER = "+14086370864";
    private static final String EMAIL_URL_TXT = "https://docs.google.com/feeds/download/documents/export/Export?id=1vPdvH2dQYoayrS0xgfxToIBsBS0cTAUAp-Hj1M2TMAw&exportFormat=txt";
    private static String details = "";
    private static String summary_docs = "";
    private static String success_docs = "";

    private static Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.name_et);
        email = findViewById(R.id.email_et);
        address = findViewById(R.id.address_et);
        Button send = findViewById(R.id.send_letter);
        Button summary = findViewById(R.id.summary);
        Button specs = findViewById(R.id.specifics);

        send.setOnClickListener(send_ocl);
        summary.setOnClickListener(summary_ocl);
        specs.setOnClickListener(specs_ocl);

        setLinks();
        new DownloadTask(this).execute();
    }

    private View.OnClickListener summary_ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(summary_docs);
            builder.setTitle("Summary");
            builder.setCancelable(true);
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.create().show();
        }
    };

    private View.OnClickListener specs_ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dialog.show();
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

                String emailText = details;
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

    private void setLinks() {
        SpannableString ss = new SpannableString("by Councilmember Rishi Kumar");
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
                    startActivity(Intent.createChooser(i, "Question for Councilmember Kumar"));
                } catch (android.content.ActivityNotFoundException ex) {
                    sendSMS("SaratogaWater: No email clients installed: " + ex.getMessage());
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
                View v = LayoutInflater.from(this).inflate(R.layout.petition_dialog, null);
                TextView textView = v.findViewById(R.id.petition_text);
                textView.setText(success_docs);
                Linkify.addLinks(textView, Linkify.WEB_URLS);
                textView.setLinkTextColor(Color.BLUE);
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
                sendSMS("Problem with app, email not sent");
            }
        } catch(Exception e) {
            sendSMS("SaratogaWater: " + e.getMessage());
        }
    }

    private static void sendSMS(String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(PHONE_NUMBER, null, message, null, null);
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


    private static class DownloadTask extends AsyncTask<Void, Void, String> {

        private WeakReference<Context> contextRef;

        public DownloadTask(Context context) {
            contextRef = new WeakReference<>(context);
        }
        //TODO: Format numbered list
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(EMAIL_URL_TXT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(20000);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String total = "";
                String str;
                while ((str = in.readLine()) != null) {
                    if(str.trim().length()==0){
                        total += "\n\n";
                    } else if(str.trim().contains("CASE STUDY")) {
                        total += "\n";
                    }
                    if(str.trim().contains("*")) {
                        str = str.substring(0, str.indexOf("*")) + "\n\t" + str.substring(str.indexOf("*"));
                    }
                    if(str.trim().contains("·")) {
                        str = str.substring(0, str.indexOf("·")) + "\n\t" + str.substring(str.indexOf("·"));
                    }/*
                    if(str.trim().contains(":")) {
                        str = str.substring(0, str.indexOf(":")) + "\n" + str.substring(str.indexOf(":"));
                    }*/
                    /*if(str.trim().contains("."))*/ //TODO: Find regex for this
                    total += str;
                }

                summary_docs = total.substring(total.indexOf("SUMMARY") + 17, total.indexOf("SUCCESS")).trim();
                success_docs = total.substring(total.indexOf("SUCCESS") + 7, total.indexOf("DETAILS TO COPY, PASTE")).trim();
                // Now do processing
                total = total.substring(total.indexOf("Dear PUC"), total.indexOf("cumulative rate changes.")) + total.substring(total.indexOf("AL 510: WE WANT MORE"), total.indexOf("gouging of the consumer.")) + total.substring(total.indexOf("In my opinion, SJWC"), total.indexOf("blatant gouging by San Jose Water Company."));

                in.close();
                return total;
            } catch (IOException ioe) {
                sendSMS("SaratogaWater: " + ioe.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            details = s;
            scrollableDialog(s);
        }

        private void scrollableDialog(String message) {
            AlertDialog.Builder ad = new AlertDialog.Builder(contextRef.get());
            ad.setTitle("Details");
            View v = LayoutInflater.from(contextRef.get()).inflate(R.layout.details_dialog, null);
            TextView text = v.findViewById(R.id.detailsText);
            text.setText(message);
            Linkify.addLinks(text, Linkify.WEB_URLS);
            text.setMovementMethod(LinkMovementMethod.getInstance());
            text.setLinksClickable(true);
            text.setLinkTextColor(Color.BLUE);
            ad.setView(v);
            ad.setCancelable(true);
            ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog = ad.create();
        }
    }
}
