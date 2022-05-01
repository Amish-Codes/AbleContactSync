package com.example.ablecontactsync;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private String[] PERMISSIONS;
    Button button1;
    Button button2;
    FirebaseDatabase rootNode;
    DatabaseReference reference;
    TextView textView2;
    static int count = 0;



    public void addListenerOnButton1() {
        button1 = (Button) findViewById(R.id.button);

        PERMISSIONS = new String[] {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        };

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                if (!hasPermissions(MainActivity.this,PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this,PERMISSIONS,1);
                } else if (hasPermissions(MainActivity.this, PERMISSIONS)) {
                    createHashMapOfContacts();
                }
            }
        });
    }


    public void createHashMapOfContacts() {
        textView2 = (TextView) findViewById(R.id.textView2);
        HashMap<String, String> contacts = new HashMap<String, String>();

        ContentResolver cr = getContentResolver();

        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { contactId }, null);

                if (phoneCursor.moveToFirst()) {
                    @SuppressLint("Range") String phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.put(name, phone);
                }
                phoneCursor.close();
            } while (cursor.moveToNext());
        }
        cursor.close();

        rootNode = FirebaseDatabase.getInstance();
        reference = rootNode.getReference("contacts");
        SimpleDateFormat sd = new SimpleDateFormat(
                "dd.MM.yyyy 'at' HH:mm:ss"
        );
        Date date = new Date();
        String d = sd.format(date);
        System.out.println(d);
        for (Map.Entry<String, String> entry : contacts.entrySet()) {

            String name = entry.getKey();
            String phone = entry.getValue();

            UserHelper helperClass = new UserHelper( name, phone );

            String child_name = Integer.toString(count)+ " " + normalizeString(name);
            reference.child(normalizeString(d)).child(child_name).setValue(helperClass);
            count++;
            textView2.setText(Integer.toString(count));

        }
    }

    public String normalizeString(String s) {
        s = s.replaceAll("[^a-zA-Z0-9]", "");
        return s;
    }

    public long TimeOver(String endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        Date current = null;
        Date end = null;
        Date date = new Date();
        String currentDate = sdf.format(date);
        try {
            current = sdf.parse(currentDate);
            end = sdf.parse(endDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long diff = end.getTime() - current.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        return Math.abs(days);
    }

//    contact delete
    public void deleteContact() {
        rootNode = FirebaseDatabase.getInstance();
        reference = rootNode.getReference("contacts");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, String> map = (HashMap<String, String>) snapshot.getValue();
                for (String key: map.keySet()) {
                    System.out.println("key : "+ key);
                    if(TimeOver(key.substring(0, 8)) >= 30) {
                        reference.child(key).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Failed");
            }
        });
    }

    public void addListenerOnButton2() {
        button2 = (Button) findViewById(R.id.button2);
        button2.setBackgroundColor(getResources().getColor(R.color.teal_200));
        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                        "Successfully Deleted!", Toast.LENGTH_SHORT).show();
                deleteContact();
            }
        });
    }



    private boolean hasPermissions(Context context, String... PERMISSIONS) {

        if (context != null && PERMISSIONS != null) {

            for (String permission: PERMISSIONS){

                if (ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Contact Read Permission is granted", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Contact Read Permission is denied", Toast.LENGTH_SHORT).show();
            }

            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Contact Write Permission is granted", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Contact Write Permission is denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addListenerOnButton1();
        addListenerOnButton2();
    }
}