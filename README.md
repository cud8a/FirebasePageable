# FirebasePageable

Adds paging support to large Firebase nodes. The paging is based on a unique double value. An example for this value is the createdAt child in each list element which should be initialized with ServerValue.TIMESTAMP.

## Usage
This will create a pageable adapter on the node 'feed' with a page size of 20 ordered by the child node 'createdAt'

    public class Feed implements FirebasePageableRecyclerAdapter.Pageable {
        private double createdAt;

        @Override
        public double getKey() {
            return createdAt;
        }
    }

    public class MyRecyclerAdapter extends FirebasePageableRecyclerAdapter
    {
        public MyRecyclerAdapter(RecyclerView recyclerView, FirebasePageableRecyclerListener listener) {
            super(recyclerView, listener, "feed", 20, "createdAt");
        }
        
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((FeedViewHolder) holder).updateData((Feed) pageables.get(position));
        }
        
        @Override
        public Pageable getFromSnapshot(DataSnapshot snapshot) {
            return snapshot.getValue(Feed.class);
        }
    }
    
    
