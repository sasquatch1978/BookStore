package com.example.android.bookstore;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bookstore.data.BookContract.BookEntry;

public class EditorActivity extends AppCompatActivity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private int quantity;
    private TextView tvEditQuantity;
    private Button btnDecrease;
    private Button btnIncrease;
    private Button btnOrder;
    private EditText etTitle;
    private EditText etAuthor;
    private EditText etPrice;
    private EditText etSupplier;
    private EditText etPhoneNumber;
    private String supplierPhoneNumber;

    // Content URI for the existing book (null if it's a new book).
    private Uri currentBookUri;

    private static final int CURRENT_BOOK_LOADER_ID = 1;
    private boolean bookHasChanged = false;

    // OnTouchListener that listens for any user touches on a View, implying that
    // they are modifying the view.
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            bookHasChanged = true;
            return false;
        }
    };

    private static String BOOK_QUANTITY = "book_quantity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Identify the views.
        tvEditQuantity = findViewById(R.id.tv_edit_quantity);
        btnDecrease = findViewById(R.id.btn_decrease);
        btnDecrease.setOnClickListener(this);
        btnIncrease = findViewById(R.id.btn_increase);
        btnIncrease.setOnClickListener(this);
        btnOrder = findViewById(R.id.btn_order);
        btnOrder.setOnClickListener(this);
        etTitle = findViewById(R.id.et_title);
        etAuthor = findViewById(R.id.et_author);
        etPrice = findViewById(R.id.et_price);
        etSupplier = findViewById(R.id.et_supplier);
        etPhoneNumber = findViewById(R.id.et_phone_number);

        // Set an OnTouchListener for each view.
        btnDecrease.setOnTouchListener(touchListener);
        btnIncrease.setOnTouchListener(touchListener);
        etTitle.setOnTouchListener(touchListener);
        etAuthor.setOnTouchListener(touchListener);
        etPrice.setOnTouchListener(touchListener);
        etSupplier.setOnTouchListener(touchListener);
        etPhoneNumber.setOnTouchListener(touchListener);

        // Examine the intent that was used to launch this activity, in order to figure out
        // if a new book is being created or an existing one is being edited.
        Intent intent = getIntent();
        currentBookUri = intent.getData();

        // If the intent DOES NOT contain a book content URI, then we know that we are
        // creating a new book.
        if (currentBookUri == null) {
            // This is a new book, so change the app bar to say "Add a Book"
            setTitle(getString(R.string.editor_activity_title_new_book));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();

            // Remove the Order Button when adding a new book.
            btnOrder.setVisibility(View.GONE);
        } else {
            // Otherwise this is an existing book, so change app bar to say "Edit Book"
            setTitle(getString(R.string.editor_activity_title_edit_book));

            // Prepare the loader.
            getLoaderManager().initLoader(CURRENT_BOOK_LOADER_ID, null, this);
        }

        /* Format the phone number as the user types it.
           Reference: https://stackoverflow.com/a/15647444
           Date: 8/1/18
         */
        etPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        // End referenced code.
    }

    // Save values for screen rotation.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the values.
        outState.putInt(BOOK_QUANTITY, quantity);
    }

    // Restore values after screen rotation.
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Extract the values.
        quantity = savedInstanceState.getInt(BOOK_QUANTITY);

        // Display the values.
        tvEditQuantity.setText(String.valueOf(quantity));
    }

    @Override
    public void onBackPressed() {
        // If the book hasn't changed, continue with handling back button press.
        if (!bookHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(View view) {
        // Perform action on click.
        switch (view.getId()) {
            // Action to perform when the decrease button is clicked.
            case R.id.btn_decrease:
                // Don't allow quantity to go below zero.
                if (quantity == 0) {
                    Toast.makeText(this, R.string.minimum_quantity,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Decrease the quantity and set the value in the quantity TextView.
                quantity--;
                tvEditQuantity.setText(String.valueOf(quantity));
                break;

            // Action to perform when the increase button is clicked.
            case R.id.btn_increase:
                // Increase the quantity and set the value in the quantity TextView.
                quantity++;
                tvEditQuantity.setText(String.valueOf(quantity));
                break;

            // Action to perform when the order button is clicked.
            case R.id.btn_order:
                // Create an intent to call the supplier.
                Intent callSupplier = new Intent(Intent.ACTION_DIAL);
                callSupplier.setData(Uri.parse("tel:" + supplierPhoneNumber));
                // Make sure an app is installed to complete this action.
                if (callSupplier.resolveActivity(getPackageManager()) != null) {
                    // Start the intent if there is an app installed to handle the intent.
                    startActivity(callSupplier);
                } else {
                    // Show a toast if there isn't an app installed to handle the intent.
                    Toast.makeText(getApplication(), R.string.install_phone_app,
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new book, hide the "Delete" menu item.
        if (currentBookUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Perform action when user clicks on a menu option in the app bar overflow menu.
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save a book to the database.
                saveBook();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the book hasn't changed, continue with navigating up to parent activity.
                if (!bookHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Get user input from the editor and save a new book into the database.
    private void saveBook() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String productName = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String quantity = tvEditQuantity.getText().toString().trim();
        String supplier = etSupplier.getText().toString().trim();
        String supplierPhoneNumber = etPhoneNumber.getText().toString().trim();

        // If this is a new book and all of the fields are blank.
        if (currentBookUri == null &&
                TextUtils.isEmpty(productName) && TextUtils.isEmpty(author) &&
                TextUtils.isEmpty(price) && quantity.equals(getString(R.string.zero)) &&
                TextUtils.isEmpty(supplier) && TextUtils.isEmpty(supplierPhoneNumber)) {
            // Exit the activity without saving a new book.
            finish();
            return;
        }

        // Make sure the book title is entered.
        if (TextUtils.isEmpty(productName)) {
            Toast.makeText(this, "Please enter a title.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the author is entered.
        if (TextUtils.isEmpty(author)) {
            Toast.makeText(this, "Please enter an author.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the price is entered.
        if (TextUtils.isEmpty(price)) {
            Toast.makeText(this, "Please enter a price.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the quantity is entered if it is a new book.
        // Can be zero when editing book, in case the book is out of stock and user wants to change
        // other information, but hasn't received any new inventory yet.
        if (currentBookUri == null && quantity.equals(getString(R.string.zero))) {
            Toast.makeText(this, "Please enter a quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the supplier is entered.
        if (TextUtils.isEmpty(supplier)) {
            Toast.makeText(this, "Please enter a supplier.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the supplier's phone number is entered.
        if (TextUtils.isEmpty(supplierPhoneNumber)) {
            Toast.makeText(this, "Please enter the supplier's phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the price to a double.
        double bookPrice = Double.parseDouble(price);

        // Convert the quantity to an int.
        int bookQuantity = Integer.parseInt(quantity);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(BookEntry.COLUMN_BOOK_PRODUCT_NAME, productName);
        values.put(BookEntry.COLUMN_BOOK_AUTHOR, author);
        values.put(BookEntry.COLUMN_BOOK_PRICE, bookPrice);
        values.put(BookEntry.COLUMN_BOOK_QUANTITY, bookQuantity);
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER, supplier);
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER, supplierPhoneNumber);

        // Check if this is a new or existing book.
        if (currentBookUri == null) {
            // Insert a new book into the provider, returning the content URI for the new book.
            Uri newUri = getContentResolver().insert(BookEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the row ID is null, then there was an error with insertion.
                Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful, display a toast.
                Toast.makeText(this, R.string.save_successful, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Check to see if any updates were made if not, no need to update the database.
            if (!bookHasChanged) {
                // Exit the activity without updating the book.
                finish();
            } else {
                // Update the book.
                int rowsUpdated = getContentResolver().update(currentBookUri, values, null,
                        null);

                // Show a toast message depending on whether or not the update was successful.
                if (rowsUpdated == 0) {
                    // If no rows were updated, then there was an error with the update.
                    Toast.makeText(this, R.string.update_error, Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful, display a toast.
                    Toast.makeText(this, R.string.update_successful,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Exit the activity, called here so the user can enter all of the required fields.
        finish();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the book
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Perform the deletion of the book in the database.
    private void deletePet() {
        // Only perform the delete if this is an existing book.
        if (currentBookUri != null) {
            // Delete an existing book.
            int rowsDeleted = getContentResolver().delete(currentBookUri, null,
                    null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful, display a toast.
                Toast.makeText(this, R.string.delete_successful, Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
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
                BookEntry.COLUMN_BOOK_SUPPLIER,
                BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER
        };

        // This loader will execute the ContentProvider's query on the background thread.
        return new CursorLoader(this,
                currentBookUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Exit early if the cursor is null or there is less than 1 row in the cursor.
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it.
        if (cursor.moveToFirst()) {
            // Find the columns of book attributes that are needed.
            int productNameColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRODUCT_NAME);
            int authorColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_AUTHOR);
            int priceColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_SUPPLIER);
            int supplierPhoneNumberColumnIndex = cursor.getColumnIndex(
                    BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER);

            // Extract out the value from the Cursor for the given column index.
            String productName = cursor.getString(productNameColumnIndex);
            String author = cursor.getString(authorColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            quantity = cursor.getInt(quantityColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            supplierPhoneNumber = cursor.getString(supplierPhoneNumberColumnIndex);

            // Convert the price and quantity back to Strings to be displayed on the views.
            String bookPrice = String.valueOf(price);
            String bookQuantity = String.valueOf(quantity);

            // Update the views on the screen with the values from the database.
            etTitle.setText(productName);
            etAuthor.setText(author);
            etPrice.setText(bookPrice);
            tvEditQuantity.setText(bookQuantity);
            etSupplier.setText(supplier);
            etPhoneNumber.setText(supplierPhoneNumber);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        etTitle.setText("");
        etAuthor.setText("");
        etPrice.setText("");
        tvEditQuantity.setText("");
        etSupplier.setText("");
        etPhoneNumber.setText("");
    }
}
