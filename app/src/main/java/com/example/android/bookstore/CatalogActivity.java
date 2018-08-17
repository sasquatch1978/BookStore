package com.example.android.bookstore;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.example.android.bookstore.data.BookContract.BookEntry;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private BookCursorAdapter adapter;
    private static final int BOOK_LOADER_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Identify the views.
        ListView bookList = findViewById(R.id.book_list);
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
                startActivity(editBook);
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define the projection that specifies which columns from the database
        // will be used in the query.
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_PRODUCT_NAME,
                BookEntry.COLUMN_BOOK_AUTHOR,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed, to make sure it is no longer being used.
        adapter.swapCursor(null);
    }
}
