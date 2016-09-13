package com.gonzo.jimmy;

import com.google.firebase.database.DatabaseError;

/**
 * Created by tamas on 13.09.16.
 */
public interface FirebasePageableRecyclerListener {
    /**
     * called only once after init to signal that the list is empty
     */
    void listIsEmpty();

    /**
     * called only once after init to signal that the list is not empty
     */
    void listIsNotEmpty();

    /**
     * when something goes wrong like Permission Error from Firebase
     * @param databaseError
     */
    void databaseError(DatabaseError databaseError);

    /**
     * called always when a new page gets loaded
     */
    void loadingMoreStarted();

    /**
     * called always when a new page was loaded
     */
    void loadingMoreFinished();
}
