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

import java.text.NumberFormat;

public class EditorActivity extends AppCompatActivity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private TextView tvFillInFields;
    private EditText etEditQuantity;
    private EditText etTitle;
    private EditText etAuthor;
    private EditText etPrice;
    private EditText etSupplier;
    private EditText etPhoneNumber;
    private Button btnDecrease;
    private Button btnIncrease;
    private Button btnOrder;


    // Value for the number of books.
    private int quantity;

    // String for the supplier's phone number.
    private String supplierPhoneNumber;

    // Content URI for the existing book (null if it's a new book).
    private Uri currentBookUri;

    // Identifier for the book loader.
    private static final int CURRENT_BOOK_LOADER_ID = 1;

    // Holds the quantity for screen rotation.
    private static String BOOK_QUANTITY = "book_quantity";

    // Keeps track of whether the book has changed or not.
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Identify the views.
        identifyViews();

        // Set the appropriate listeners for the views.
        setViewListeners();

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

            // Hide the fill in fields TextView since they are already filled in.
            tvFillInFields.setVisibility(View.GONE);
        }
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
        etEditQuantity.setText(String.valueOf(quantity));
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
                        // User clicked "Discard" button, exit the current activity.
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

        // Set the message.
        builder.setMessage(R.string.unsaved_changes_dialog_msg);

        // Handle the button clicks.
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
        // Get the text from the quantity EditText.
        String enteredQuantity = etEditQuantity.getText().toString().trim();

        // Check to see if quantity has been entered and set it as the quantity if it has.
        if (!enteredQuantity.equals("")) {
            quantity = Integer.parseInt(enteredQuantity);
        }

        // Set the quantity to zero if quantity has been entered and then removed,
        // so that the buttons work properly.
        if (enteredQuantity.equals("")) {
            quantity = 0;
        }

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
                etEditQuantity.setText(String.valueOf(quantity));
                break;

            // Action to perform when the increase button is clicked.
            case R.id.btn_increase:
                // Increase the quantity and set the value in the quantity TextView.
                quantity++;
                etEditQuantity.setText(String.valueOf(quantity));
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
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new book, hide the "Delete" menu item.
        if (currentBookUri == null) {
            MenuItem delete = menu.findItem(R.id.action_delete);
            delete.setVisible(false);
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
        String quantity = etEditQuantity.getText().toString().trim();
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
            Toast.makeText(this, R.string.enter_title, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the author is entered.
        if (TextUtils.isEmpty(author)) {
            Toast.makeText(this, R.string.enter_author, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the price is entered.
        if (TextUtils.isEmpty(price)) {
            Toast.makeText(this, R.string.enter_price, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the quantity is entered if it is a new book.
        // Can be zero when editing book, in case the book is out of stock and user wants to change
        // other information, but hasn't received any new inventory yet.
        if (currentBookUri == null && (quantity.equals(getString(R.string.zero)) ||
                quantity.equals(""))) {
            Toast.makeText(this, R.string.enter_quantity, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the supplier is entered.
        if (TextUtils.isEmpty(supplier)) {
            Toast.makeText(this, R.string.enter_supplier, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the supplier's phone number is entered.
        if (TextUtils.isEmpty(supplierPhoneNumber)) {
            Toast.makeText(this, R.string.enter_suppliers_phone_number,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the price to a double.
        double bookPrice = Double.parseDouble(price);

        // Convert the quantity to an int, if there is a quantity entered, if not set it to zero.
        int bookQuantity = 0;
        if (!quantity.equals("")) {
            bookQuantity = Integer.parseInt(quantity);
        }

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

        // Set the message.
        builder.setMessage(R.string.delete_dialog_msg);

        // Handle the button clicks.
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteBook();
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
    private void deleteBook() {
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
        // Exit the activity.
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

            // Format the price to two decimal places so that it displays as "7.50" not "7.5"
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            nf.setMinimumFractionDigits(2);
            String formatPrice = nf.format(price);

            // Remove commas from large numbers so that it displays as "258963.99" not "258,963.99",
            // to keep the app from crashing when the save button is clicked.
            String newFormatPrice = formatPrice.replace(",", "");

            // Update the views on the screen with the values from the database.
            etTitle.setText(productName);
            etAuthor.setText(author);
            etPrice.setText(newFormatPrice);
            etEditQuantity.setText(String.valueOf(quantity));
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
        etEditQuantity.setText("");
        etSupplier.setText("");
        etPhoneNumber.setText("");
    }

    // Identify the views.
    private void identifyViews() {
        // Identify the views.
        tvFillInFields = findViewById(R.id.tv_fill_in_fields);
        etEditQuantity = findViewById(R.id.et_edit_quantity);
        etTitle = findViewById(R.id.et_title);
        etAuthor = findViewById(R.id.et_author);
        etPrice = findViewById(R.id.et_price);
        etSupplier = findViewById(R.id.et_supplier);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        btnDecrease = findViewById(R.id.btn_decrease);
        btnIncrease = findViewById(R.id.btn_increase);
        btnOrder = findViewById(R.id.btn_order);
    }

    // Set the appropriate listeners for the views.
    private void setViewListeners() {
        // Set an OnClickListener for each button.
        btnDecrease.setOnClickListener(this);
        btnIncrease.setOnClickListener(this);
        btnOrder.setOnClickListener(this);

        // Set an OnTouchListener for each view.
        btnDecrease.setOnTouchListener(touchListener);
        btnIncrease.setOnTouchListener(touchListener);
        etTitle.setOnTouchListener(touchListener);
        etAuthor.setOnTouchListener(touchListener);
        etPrice.setOnTouchListener(touchListener);
        etEditQuantity.setOnTouchListener(touchListener);
        etSupplier.setOnTouchListener(touchListener);
        etPhoneNumber.setOnTouchListener(touchListener);

        /* Format the phone number as the user types it.
           Reference: https://stackoverflow.com/a/15647444
           Date: 8/1/18
         */
        etPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        // End referenced code.
    }
}
