package com.example.nativewebrtcexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ServiceProfileAdapter extends RecyclerView.Adapter<ServiceProfileAdapter.ViewHolder> {

    private List<MyService> services =null;
    private static final String TAG = "ServiceProfileAdapter";

    public ServiceProfileAdapter(List<MyService> profiles) {
        this.services = profiles;
    }

    public interface OnItemClickListener {
        void onItemClicked(int position,MyService profile);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener (OnItemClickListener listener) {
        itemClickListener = listener;
    }

    // 아이템 뷰를 저장하는 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        // ViewHolder 에 필요한 데이터들을 적음.
        private TextView socketID;
        //        private TextView nickname;
        private TextView description;
        private Button button;
        private MyService service;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 아이템 뷰에 필요한 View
            socketID = itemView.findViewById(R.id.item_profile_socketID);
//            nickname = itemView.findViewById(R.id.item_profile_nickname);
            description = itemView.findViewById(R.id.item_profile_description);
//            button = itemView.findViewById(R.id.item_profile_button);

            //여기서 recycler view의 click listener 설정
            //https://parkho79.tistory.com/152

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ViewHodler 객체를 생성 후 리턴한다.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.service_profile, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                if(position != RecyclerView.NO_POSITION) {
                    MyService service = services.get(position);
                    itemClickListener.onItemClicked(position, service);
                }
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // ViewHolder 가 재활용 될 때 사용되는 메소드
        holder.service = services.get(position);
        holder.socketID.setText(holder.service.getSocketID());
//        holder.nickname.setText(profile.getNickname());
        holder.description.setText(holder.service.getDescription());

//        holder.socketID.setText("socketid");
//        holder.nickname.setText("nickname");
//        holder.description.setText("Description");
    }

    @Override
    public int getItemCount() {
        // 전체 데이터의 개수 조회
        return services.size();
    }


}

