package com.example.android.bookstore.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.bookstore.data.BookContract.BookEntry;

import java.util.Objects;

public class BookProvider extends ContentProvider {

    // Database helper object.
    private BookDbHelper dbHelper;

    // Tag for the log messages.
    private static final String LOG_TAG = BookProvider.class.getSimpleName();

    // URI matcher code for the content URI for the books table.
    private static final int BOOK = 100;

    // URI matcher code for the content URI for a single book in the books table.
    private static final int BOOK_ID = 101;

    // UriMatcher object to match a content URI to a corresponding code.
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // Sets the integer value for multiple rows in the books table.
        uriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKSTORE, BOOK);

        // Sets the integer value to a single row in the books table.
        uriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKSTORE
                + "/#", BOOK_ID);
    }

    // Initialize the provider and the database helper object.
    @Override
    public boolean onCreate() {
        dbHelper = new BookDbHelper(getContext());
        return true;
    }

    // Perform the query for the given URI.
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // This cursor will hold the result of the query.
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code.
        int match = uriMatcher.match(uri);
        switch (match) {
            case BOOK:
                // For the BOOK code, query the books table directly.
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the books table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor, so that if the data changes the cursor can be updated.
        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);

        // Return the cursor.
        return cursor;
    }

    // Returns the MIME type of data for the content URI.
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case BOOK:
                return BookEntry.CONTENT_LIST_TYPE;
            case BOOK_ID:
                return BookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    // Insert new data into the provider with the given ContentValues.
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case BOOK:
                assert contentValues != null;
                return insertBook(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    // Insert a book into the database with the given content values. Return the new content URI
    // for that specific row in the database.
    private Uri insertBook(Uri uri, ContentValues values) {
        // Check that the product_name is not null.
        String productName = values.getAsString(BookEntry.COLUMN_BOOK_PRODUCT_NAME);
        if (productName == null) {
            throw new IllegalArgumentException("Book requires a title.");
        }

        // Check that the author is not null.
        String author = values.getAsString(BookEntry.COLUMN_BOOK_AUTHOR);
        if (author == null) {
            throw new IllegalArgumentException("Book requires an author");
        }

        // Check that the price is greater than or equal to 0.
        Double price = values.getAsDouble(BookEntry.COLUMN_BOOK_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Book requires valid price.");
        }

        // Check that the quantity is greater than or equal to 0.
        Integer quantity = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Book requires valid quantity.");
        }

        // Check that the supplier is not null.
        String supplier = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER);
        if (supplier == null) {
            throw new IllegalArgumentException("Book requires a supplier.");
        }

        // Check that the supplier_phone_number is not null.
        String supplierPhoneNumber = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER);
        if (supplierPhoneNumber == null) {
            throw new IllegalArgumentException("Book requires the supplier's phone number.");
        }

        // Gets the data repository in write mode.
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Insert the new row, with the given values.
        long id = database.insert(BookEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the book content URI.
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);

        // Once the ID of the new row in the table is known,
        // return the new URI with the ID appended to the end of it.
        return ContentUris.withAppendedId(uri, id);
    }

    // Delete the data at the given selection and selection arguments.
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Track the number of rows that were deleted.
        int rowsDeleted;

        final int match = uriMatcher.match(uri);
        switch (match) {
            case BOOK:
                // Delete all rows that match the selection and selection args.
                rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case BOOK_ID:
                // Delete a single row given by the ID in the URI.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed.
        if (rowsDeleted != 0) {
            Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted.
        return rowsDeleted;
    }

    // Updates the data at the given selection and selection arguments, with the new ContentValues.
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case BOOK:
                assert contentValues != null;
                return updateBook(uri, contentValues, selection, selectionArgs);
            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI,
                // so the proper row to can be updated.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                assert contentValues != null;
                return updateBook(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    // Update books in the database with the given content values. Apply the changes to the rows
    // specified in the selection and selection arguments (which could be 0 or 1 or more books).
    // Return the number of rows that were successfully updated.
    private int updateBook(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link BookEntry#COLUMN_BOOK_PRODUCT_NAME} key is present,
        // check that the product_name value is not null.
        if (values.containsKey(BookEntry.COLUMN_BOOK_PRODUCT_NAME)) {
            String productName = values.getAsString(BookEntry.COLUMN_BOOK_PRODUCT_NAME);
            if (productName == null) {
                throw new IllegalArgumentException("Book requires a title.");
            }
        }

        // If the {@link BookEntry#COLUMN_BOOK_AUTHOR} key is present,
        // check that the author value is valid.
        if (values.containsKey(BookEntry.COLUMN_BOOK_AUTHOR)) {
            String author = values.getAsString(BookEntry.COLUMN_BOOK_AUTHOR);
            if (author == null) {
                throw new IllegalArgumentException("Book requires an author");
            }
        }

        // If the {@link BookEntry#COLUMN_BOOK_PRICE} key is present,
        // check that the price value is valid.
        if (values.containsKey(BookEntry.COLUMN_BOOK_PRICE)) {
            Integer price = values.getAsInteger(BookEntry.COLUMN_BOOK_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Book requires valid price.");
            }
        }

        // If the {@link BookEntry#COLUMN_BOOK_QUANTITY} key is present,
        // check that the quantity value is valid.
        if (values.containsKey(BookEntry.COLUMN_BOOK_QUANTITY)) {
            Integer quantity = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Book requires valid quantity.");
            }
        }

        // If the {@link BookEntry#COLUMN_BOOK_SUPPLIER} key is present,
        // check that the supplier value is not null.
        if (values.containsKey(BookEntry.COLUMN_BOOK_SUPPLIER)) {
            String supplier = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER);
            if (supplier == null) {
                throw new IllegalArgumentException("Book requires a supplier.");
            }
        }

        // If the {@link BookEntry#COLUMN_BOOK_SUPPLIER_PHONE_NUMBER} key is present,
        // check that the supplier_phone_number value is not null.
        if (values.containsKey(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER)) {
            String supplierPhoneNumber = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER);
            if (supplierPhoneNumber == null) {
                throw new IllegalArgumentException("Book requires the supplier's phone number.");
            }
        }

        // If there are no values to update, then don't try to update the database.
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writable database to update the data.
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected.
        int rowsUpdated = database.update(BookEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed.
        if (rowsUpdated != 0) {
            Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated.
        return rowsUpdated;
    }
}
