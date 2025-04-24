package com.example.flex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private final List<User> userList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public UserAdapter(List<User> userList, OnItemClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.user_name.setText(user.getName());
        holder.recycler_Lat.setText(String.valueOf(user.getLatitude()));
        holder.recycler_Lon.setText(String.valueOf(user.getLongitude()));
        holder.user_image.setImageResource(R.drawable.baseline_assistant_navigation_24);
        holder.user_status.setImageResource(R.drawable.offline);
        if (user.isOnline()) {
            holder.user_status.setImageResource(R.drawable.online);
        } else {
            holder.user_status.setImageResource(R.drawable.offline);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView user_name;
        TextView recycler_Lat;
        TextView recycler_Lon;
        ImageView user_image;
        ImageView user_status;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            user_name = itemView.findViewById(R.id.user_name);
            recycler_Lat = itemView.findViewById(R.id.recyclerLat);
            recycler_Lon = itemView.findViewById(R.id.recyclerLon);
            user_image = itemView.findViewById(R.id.user_image);
            user_status = itemView.findViewById(R.id.user_status);
        }
    }

    public void updateList(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();  // 🟢 RecyclerView ko update karne ke liye
    }
}


