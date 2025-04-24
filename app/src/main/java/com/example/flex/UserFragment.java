package com.example.flex;

import static android.view.animation.AnimationUtils.loadAnimation;

import static com.example.flex.MainActivity.mMap;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;


    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        recyclerView = view.findViewById(R.id.fragment_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        MainActivity.userAdapter = new UserAdapter(MainActivity.userList, user -> {
            if (mMap != null) {
                mMap.clear();

                LatLng location = new LatLng(user.getLatitude(), user.getLongitude());
                mMap.addMarker(new MarkerOptions().position(location).title(user.getName()));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.5f));

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(user.getName()));

                if (marker != null) {
                    marker.showInfoWindow();  // show title without click
                }

                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.frame_layout, new Fragment());
                transaction.addToBackStack(null); // optional, back press se wapas aana ho to
                transaction.commit();

            }
        });
        recyclerView.setAdapter(MainActivity.userAdapter);


        return view;
    }
}