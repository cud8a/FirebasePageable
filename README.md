# FirebasePageable

Adds paging support to large Firebase nodes. The paging is based on a unique double value. An example for this value is the createdAt child in each list element which should be initialized with ServerValue.TIMESTAMP.

## Usage

    public class MyRecyclerAdapter extends FirebasePageableRecyclerAdapter implements FirebasePageableRecyclerListener
    {
      public MyRecyclerAdapter(RecyclerView recyclerView) {
        super(recyclerView, this, "feed", 20, "createdAt");
      }
    }
