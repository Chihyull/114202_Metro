package com.example.a114202_metro;
//修改中
public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {
    private List<Favorite> favorites;
    private Context context;

    public FavoriteAdapter(List<Favorite> favorites, Context context) {
        this.favorites = favorites;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Favorite f = favorites.get(position);
        holder.name.setText(f.getName());
        holder.address.setText(f.getAddress());

        // Google Map 跳轉
        holder.address.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(f.getAddress()));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            context.startActivity(mapIntent);
        });

        // 收藏愛心
        holder.heart.setImageResource(f.isLiked() ? R.drawable.B : R.drawable.A);
        holder.heart.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("取消收藏")
                    .setMessage("確定要取消收藏嗎？")
                    .setPositiveButton("是", (dialog, which) -> {
                        f.setLiked(false);
                        holder.heart.setImageResource(R.drawable.A);
                        FavoriteManager.removeFavorite(context, f.getName());
                        // ❌ 不馬上刪掉 RecyclerView item，等使用者離開再刷新
                    })
                    .setNegativeButton("否", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;
        ImageView heart;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_name);
            address = itemView.findViewById(R.id.tv_address);
            heart = itemView.findViewById(R.id.iv_heart);
        }
    }
}

