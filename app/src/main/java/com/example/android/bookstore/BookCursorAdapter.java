package com.example.android.bookstore;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bookstore.data.BookContract.BookEntry;

import java.text.NumberFormat;

public class BookCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link BookCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    BookCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the book data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Identify the views.
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvAuthor = view.findViewById(R.id.tv_author);
        TextView tvPrice = view.findViewById(R.id.tv_price);
        TextView tvQuantity = view.findViewById(R.id.tv_quantity);
        TextView tvQuantityHint = view.findViewById(R.id.tv_quantity_hint);
        Button btnSale = view.findViewById(R.id.btn_sale);

        // Find the columns of book attributes that are needed to populate the list.
        int idColumnIndex = cursor.getColumnIndex(BookEntry._ID);
        int productNameColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRODUCT_NAME);
        int authorColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_AUTHOR);
        int priceColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE);
        final int quantityColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY);

        // Read the book attributes from the Cursor for the current book.
        final int bookRowId = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(productNameColumnIndex);
        String author = cursor.getString(authorColumnIndex);
        double price = cursor.getDouble(priceColumnIndex);
        String quantity = cursor.getString(quantityColumnIndex);

        // Set the the attributes for the current book on the TextViews.
        // Display the book title.
        tvTitle.setText(productName);

        // Display the author.
        tvAuthor.setText(author);

        // Display the formatted the price corresponding to the user's locale.
        tvPrice.setText(String.valueOf(NumberFormat.getCurrencyInstance().format(price)));

        // Display "In stock" or "Out of stock" for the quantity hint.
        if (quantity.equals(context.getString(R.string.zero))) {
            // Make the text bold in the quantity hint TextView if the quantity is zero.
            tvQuantityHint.setTypeface(Typeface.create(tvQuantityHint.getTypeface(), Typeface.BOLD));

            // Change the text color in the quantity hint TextView if the quantity is zero.
            tvQuantityHint.setTextColor(context.getResources().getColor(R.color.colorTextAttention));

            // Set the quantity hint TextView text to "Out of stock:", if the quantity is zero.
            tvQuantityHint.setText(R.string.out_of_stock);
        } else {
            // Make sure the quantity hint TextView reverts to it's normal state,
            // if the quantity was previously zero.
            tvQuantityHint.setTypeface(Typeface.create(tvQuantityHint.getTypeface(), Typeface.NORMAL));
            tvQuantityHint.setTextColor(context.getResources().getColor(R.color.colorTextLight));

            // Set the quantity hint TextView text to "In stock:", if the quantity is more than zero.
            tvQuantityHint.setText(R.string.in_stock);
        }

        // Display the quantity.
        tvQuantity.setText(quantity);

        // Set a listener for the Sale Button.
        btnSale.setOnClickListener(new View.OnClickListener() {

            // Value for the quantity to be updated.
            int bookQuantity = cursor.getInt(quantityColumnIndex);

            @Override
            public void onClick(View view) {
                // Don't allow the quantity to go below zero.
                if (bookQuantity == 0) {
                    // Display a toast, that informs the user the book is out of stock and to reorder.
                    Toast toast = Toast.makeText(context, R.string.contact_supplier,
                            Toast.LENGTH_SHORT);
                    /* Identify the toast TextView so it can be centered, since it's two lines.
                       Reference: https://stackoverflow.com/a/13492794
                       Date: 8/14/18
                     */
                    TextView toastText = toast.getView().findViewById(android.R.id.message);
                    toastText.setGravity(Gravity.CENTER);
                    // End referenced code.
                    toast.show();
                } else {
                    // Form the content URI that represents the specific book that was clicked on.
                    Uri currentBookUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, bookRowId);

                    // Decrease the quantity by one.
                    bookQuantity--;

                    // Create a new map of values.
                    ContentValues values = new ContentValues();
                    values.put(BookEntry.COLUMN_BOOK_QUANTITY, bookQuantity);

                    // Update an existing book quantity.
                    int rowsUpdated = context.getContentResolver().update(currentBookUri, values,
                            null, null);

                    // Show a toast message depending on whether the quantity update was successful.
                    if (rowsUpdated == 0) {
                        // If no rows were updated, then there was an error with the quantity update.
                        Toast.makeText(context, R.string.quantity_not_updated,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Otherwise, the quantity update was successful.
                        Toast.makeText(context, R.string.quantity_updated,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
