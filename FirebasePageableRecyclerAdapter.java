package com.gonzo.jimmy;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by tamas on 13.09.16.
 */
public abstract class FirebasePageableRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ChildEventListener {
    /**
     * The objects we are paging must conform to this protocol
     */
    public interface Pageable {
        double getKey();
    }

    /**
     * Convert the Firebase snapshot of one child node to a <code>Pageable</code> object. The returned object will be stored in the pageables list
     * an can be accessed by subclasses
     * @param snapshot Firebase snapshot of one child node from the path
     * @return the <code>Pageable</code> object
     */
    public abstract Pageable getFromSnapshot(DataSnapshot snapshot);

    protected List<Pageable> pageables = new ArrayList<>();
    protected int lastVisibleItem, totalItemCount;
    protected boolean loading, isLastPage;
    protected int visibleThreshold = 2;

    // value to start with when loading the next page
    private double valueWhereTheNextPageBegins;

    // child node used as key for the paging mechanism, is the key for <code>valueWhereTheNextPageBegins</code>
    private String orderByChild;

    // query to listen to adds, will be disconnected when loading the next page to avoid concurrency issues
    private Query queryForListeningToAdds;

    // first child add event must be ignored if we already added a page
    private boolean ignoreAdd = true;

    private static final String TAG = "FPRA";
    private FirebasePageableRecyclerListener listener;

    private String path;
    int pageSize;

    /**
     * Create the pager
     * @param recyclerView the underlying RecyclerView
     * @param listener listener for firebase loading success/error feedback
     * @param path the firebase path to load the pages from
     * @param pageSize the size of one single page, pass a value greater than 0 here or all the children in the path will be loaded at once
     */
    protected FirebasePageableRecyclerAdapter(RecyclerView recyclerView, final FirebasePageableRecyclerListener listener, String path, int pageSize, String orderByChild) {
        this.listener = listener;
        this.path = path;
        this.pageSize = pageSize;
        this.orderByChild = orderByChild;

        Log.d(TAG, "--------------- creating pager with: ---------------");
        Log.d(TAG, "    path: " + path);
        Log.d(TAG, "    pageSize: " + pageSize);
        Log.d(TAG, "    orderByChild: " + orderByChild);
        Log.d(TAG, "----------------------------------------------------");

        // scroll event for loading the next page
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (!loading && totalItemCount <= (lastVisibleItem + visibleThreshold) && !isLastPage) {
                        // End has been reached
                        // Do something
                        onLoadMore();
                        loading = true;
                    }
                }
            });
        }

        Query query = FirebaseDatabase.getInstance().getReference(path).orderByChild(orderByChild);
        if (pageSize > 0) {
            query = query.limitToLast(pageSize + 1);
        }

        // fetch the first page
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                addPage(dataSnapshot, 0);
                if (pageables.isEmpty())
                    listener.listIsEmpty();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Error: " + databaseError.getMessage());
                listener.databaseError(databaseError);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pageables.size();
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if (ignoreAdd) {
            ignoreAdd = false;
            Log.d(TAG, "onChildAdded: ignored");
        } else {
            Log.d(TAG, "onChildAdded: " + dataSnapshot);
            Pageable item = getFromSnapshot(dataSnapshot);

            listener.listIsNotEmpty();
            pageables.add(0, item);
            notifyDataSetChanged();
        }
    }

    private void addPage(DataSnapshot dataSnapshot, int atIndex) {
        Log.d(TAG, "adding page with " + dataSnapshot.getChildrenCount() + " items");

        Pageable firstItem = null;
        int added = 0;
        int sizeBefore = pageables.size();

        for (DataSnapshot snap : dataSnapshot.getChildren()) {
            Pageable item = getFromSnapshot(snap);
            if (firstItem == null) {
                firstItem = item;
            } else {
                pageables.add(atIndex, item);
                added++;
            }
        }

        if (added < pageSize && firstItem != null) {
            pageables.add(firstItem);
            added++;
        } else if (added == pageSize) {
            valueWhereTheNextPageBegins = firstItem.getKey();
        }

        if (added > 0) {
            notifyDataSetChanged();
            if (sizeBefore == 0) {
                listener.listIsNotEmpty();
            } else {
                listener.loadingMoreFinished();
            }
        }

        // we also listen too add events, so in case a new child is added to our path it will appear immediately at the top of the list
        ignoreAdd = true;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        queryForListeningToAdds = databaseReference.limitToLast(1);
        queryForListeningToAdds.addChildEventListener(this);
    }

    // load the next page because we scrolled to the bottom
    private void onLoadMore() {
        if (valueWhereTheNextPageBegins > 0) {
            listener.loadingMoreStarted();

            Log.d(TAG, "------ onLoadMore, valueWhereTheNextPageBegins: " + valueWhereTheNextPageBegins);

            // temporarily disable the add listener until we add the new page
            queryForListeningToAdds.removeEventListener(this);

            double endAtValue = valueWhereTheNextPageBegins;

            valueWhereTheNextPageBegins = 0;
            final int insertAtIndex = pageables.size();

            Query query = FirebaseDatabase.getInstance().getReference(path).orderByChild(orderByChild);
            query.endAt(endAtValue).limitToLast(pageSize + 1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "------ onLoadMore, onDataChange: " + dataSnapshot.getChildrenCount());
                    addPage(dataSnapshot, insertAtIndex);
                    loading = false;
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    loading = false;
                }
            });
        } else {
            Log.d(TAG, "------ onLoadMore, nothing to do");
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
    }

    public void destroy() {
        if (queryForListeningToAdds != null) {
            queryForListeningToAdds.removeEventListener(this);
        }
    }
}
