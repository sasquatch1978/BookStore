package com.example.android.bookstore;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.bookstore.data.BookContract.BookEntry;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private BookCursorAdapter adapter;
    private static final int BOOK_LOADER_ID = 1;

    private ListView bookList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Have the app bar say "Inventory".
        setTitle(getString(R.string.catalog_activity_title));

        // Identify the views.
        bookList = findViewById(R.id.book_list);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        bookList.setEmptyView(emptyView);

        // Set the adapter.
        adapter = new BookCursorAdapter(this, null);
        bookList.setAdapter(adapter);

        // Prepare the loader.
        getLoaderManager().initLoader(BOOK_LOADER_ID, null, this);

        // Set a listener for the list that opens the editor for the book that was clicked.
        bookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create the intent to open the editor for the book.
                Intent editBook = new Intent(CatalogActivity.this, EditorActivity.class);
                // Form the content URI that represents the specific book that was clicked on.
                Uri currentBookUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, id);
                // Set the URI on the data field of the intent
                editBook.setData(currentBookUri);
                /* Make a parent to child activity transition.
                   Reference: https://stackoverflow.com/a/43748907
                   Date: 6/13/18
                 */
                Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(
                        view, 0, 0, view.getWidth(), view.getHeight()).toBundle();
                ActivityCompat.startActivity(CatalogActivity.this, editBook, options);
                // End referenced code.
            }
        });

        // Start the EditorActivity.
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addBook = new Intent(CatalogActivity.this,
                        EditorActivity.class);
                startActivity(addBook);
            }
        });
    }

    // Prompt the user to confirm that they want to delete all books in the database.
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the message and title.
        builder.setMessage(R.string.delete_all_dialog_msg)
                .setTitle(R.string.delete_all_dialog_msg_title);

        // Handle the button clicks.
        builder.setPositiveButton(R.string.delete_all_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Yes" button, so delete all books.
                deleteAllBooks();
            }
        });
        builder.setNegativeButton(R.string.delete_all_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "No" button, so dismiss the dialog.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Delete all books in the database.
    private void deleteAllBooks() {
        int rowsDeleted = getContentResolver().delete(BookEntry.CONTENT_URI, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0) {
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, "Error deleting books.",
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful, display a toast.
            Toast.makeText(this, "All books deleted.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Determine which item is selected and take the appropriate action.
        int id = item.getItemId();

        // Respond to a click on the "Delete All Books" menu option.
        if (id == R.id.action_delete_all_entries) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Get the number of books in the list.
        int numberOfBooks = adapter.getCount();

        // If the book list is empty, hide the "Delete All Books" menu option.
        if (numberOfBooks == 0) {
            MenuItem deleteAllEntries = menu.findItem(R.id.action_delete_all_entries);
            deleteAllEntries.setVisible(false);
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define the projection that specifies which columns from the database
        // will be used in the query.
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_PRODUCT_NAME,
                BookEntry.COLUMN_BOOK_AUTHOR,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY,
                BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER
        };

        // This loader will execute the ContentProvider's query on the background thread.
        return new CursorLoader(this,
                BookEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Swap the new cursor in.
        adapter.swapCursor(cursor);

        // Invalidate the options menu, so the "Delete All Books" menu option can be hidden.
        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed, to make sure it is no longer being used.
        adapter.swapCursor(null);
    }
}
