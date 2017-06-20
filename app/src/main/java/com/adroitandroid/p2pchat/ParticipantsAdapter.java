package com.adroitandroid.p2pchat;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.adroitandroid.near.model.Host;
import com.adroitandroid.p2pchat.databinding.RowParticipantsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by pv on 20/06/17.
 */

class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantVH> {
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_ITEM = 2;
    private List<Host> mParticipants;

    ParticipantsAdapter() {
        mParticipants = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    @Override
    public ParticipantVH onCreateViewHolder(ViewGroup parent, int viewType) {
        RowParticipantsBinding binding = RowParticipantsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ParticipantVH(viewType, binding);
    }

    @Override
    public void onBindViewHolder(ParticipantVH holder, int position) {
        if (TYPE_HEADER == holder.mViewType) {
            holder.mBinding.nameTv.setText("Participants Found:");
        } else {
            holder.mBinding.nameTv.setText(mParticipants.get(position - 1).getName());
            holder.mBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    TODO: implement
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mParticipants.size() + 1;
    }

    public void setData(Set<Host> hosts) {
        mParticipants = new ArrayList<>(hosts);
        notifyDataSetChanged();
    }

    class ParticipantVH extends RecyclerView.ViewHolder {
        private final RowParticipantsBinding mBinding;
        private final int mViewType;

        ParticipantVH(int viewType, RowParticipantsBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
            this.mViewType = viewType;
        }
    }
}
