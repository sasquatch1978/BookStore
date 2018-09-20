package com.example.android.bookstore.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class BookContract {

    // Empty constructor to prevent the contract class from being instantiated.
    private BookContract() {
    }

    // The "Content authority" is a name for the entire content provider.
    public static final String CONTENT_AUTHORITY = "com.example.android.bookstore";

    //Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    //the content provider.
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible path (appended to base content URI for possible URI's).
    public static final String PATH_BOOKSTORE = "bookstore";

    // Inner class that defines the table contents of the books table.
    public static final class BookEntry implements BaseColumns {

        // The content URI to access the book data in the provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI,
                PATH_BOOKSTORE);

        // The MIME type of the {@link #CONTENT_URI} for a list of books.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_BOOKSTORE;

        // The MIME type of the {@link #CONTENT_URI} for a single book.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_BOOKSTORE;

        // Table name for the books database.
        public static final String TABLE_NAME = "books";

        // Column names for the books table.
        public static final String COLUMN_BOOK_PRODUCT_NAME = "product_name";
        public static final String COLUMN_BOOK_AUTHOR = "author";
        public static final String COLUMN_BOOK_PRICE = "price";
        public static final String COLUMN_BOOK_QUANTITY = "quantity";
        public static final String COLUMN_BOOK_SUPPLIER = "supplier";
        public static final String COLUMN_BOOK_SUPPLIER_PHONE_NUMBER = "supplier_phone_number";
    }
}
