# FirebasePageable

Adds paging support to large Firebase nodes. The paging is based on a unique double value. An example for this value is the createdAt child in each list element which should be initialized with ServerValue.TIMESTAMP.

## Usage
This will create a pageable adapter on the node 'feed' with a page size of 20 ordered by the child node 'createdAt'

    public class MyRecyclerAdapter extends FirebasePageableRecyclerAdapter
    {
        public MyRecyclerAdapter(RecyclerView recyclerView, FirebasePageableRecyclerListener listener) {
            super(recyclerView, listener, "feed", 20, "createdAt");
        }
    }
