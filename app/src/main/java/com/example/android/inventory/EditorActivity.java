/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventory.data.ProductContract.ProductEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static java.lang.Integer.parseInt;
/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

     // Identifier for the product data loader
    private static final int EXISTING_PRODUCT_LOADER = 1;

     // Content URI for the existing product (null if it's a new product)
    private Uri mCurrentProductUri;

    public static final int IMAGE_GALLERY_REQUEST = 20;
    int quantity = 1;
    private boolean saveFlag;
    private boolean mProductHasChanged = false;
    public Uri pictureUri;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    /**
     * EditText fields to enter the product info
     */
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private EditText mSupplierEditText;
    private ImageView imgPicture;

    private Intent mRequestFileIntent;
    private ParcelFileDescriptor mInputPFD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);


        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        Button contactProvider = (Button) findViewById(R.id.contact_supplier);

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {

            setTitle(getString(R.string.editor_activity_title_new_item));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();

            contactProvider.setVisibility(View.GONE);
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Item"
            setTitle(getString(R.string.editor_activity_title_edit_item));
            Button addImage = (Button) findViewById(R.id.insert_image_button);
            addImage.setText(R.string.change_image);
            contactProvider.setVisibility(View.VISIBLE);
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);

        mQuantityEditText.setText(String.valueOf(quantity));

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);

        // get access to the image view.
        imgPicture = (ImageView) findViewById(R.id.insert_image_view);
    }

    /**
     * This method will be invoked when the user clicks insert image button
     *
     * @param v
     */
    public void openGallery(View v) {
        // invoke the image gallery using an implict intent.
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);

        // where do we want to find the data?
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // finally, get a URI representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // set the data and type.  Get all image types.
        photoPickerIntent.setDataAndType(data, "image/*");

        // we will invoke this activity, and get something back from it.
        startActivityForResult(photoPickerIntent, IMAGE_GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Did the user choose OK?  If so, the code inside these curly braces will execute.
        if (resultCode == RESULT_OK) {
            // if we are here, everything processed successfully.
            if (requestCode == IMAGE_GALLERY_REQUEST) {
                // if we are here, we are hearing back from the image gallery.
                    // the address of the image on the SD Card.
                    pictureUri = data.getData();

               // Log.v("pictureUri 2", pictureUri.toString());
                    // declare a stream to read the image data from the SD Card.
                    InputStream inputStream;

                    // we are getting an input stream, based on the URI of the image.
                    try {
                        inputStream = getContentResolver().openInputStream(pictureUri);

                        // get a bitmap from the stream.
                        Bitmap image = BitmapFactory.decodeStream(inputStream);
                        // show the image to the user
                        imgPicture.setImageBitmap(image);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        // show a message to the user indictating that the image is unavailable.
                        Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

    private void saveProduct() {

        saveFlag = false;
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String picturePathString;
        if (pictureUri != null){
            picturePathString = pictureUri.toString();
        } else {
            picturePathString = "0";
        }

        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(supplierString) && TextUtils.isEmpty(quantityString)) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);

        // If the price is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = parseInt(priceString);
        }

        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);

        if (!TextUtils.isEmpty(quantityString)) {
            quantity = parseInt(quantityString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierString);
        values.put(ProductEntry.COLUMN_PRODUCT_PICTURE, picturePathString);

        // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {

            if (TextUtils.isEmpty(nameString)) {
                Toast.makeText(this, R.string.name_validator, Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(priceString)) {
                Toast.makeText(this, R.string.price_validator, Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(quantityString)) {
                Toast.makeText(this, R.string.quantity_validator, Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(supplierString)) {
                Toast.makeText(this, R.string.supplier_validator, Toast.LENGTH_SHORT).show();
            } else if (picturePathString.contentEquals("0")) {
                Toast.makeText(this, R.string.image_validator, Toast.LENGTH_SHORT).show();
            } else {
                saveFlag = true;
                // Insert a new product into the provider, returning the content URI for the new product.
                Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

                // Show a toast message depending on whether or not the insertion was successful.
                if (newUri == null) {
                    // If the new content URI is null, then there was an error with insertion.
                    Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the insertion was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (TextUtils.isEmpty(nameString)) {
                Toast.makeText(this, R.string.name_validator, Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(priceString)) {
                Toast.makeText(this, R.string.price_validator, Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(quantityString)) {
                Toast.makeText(this, R.string.quantity_validator, Toast.LENGTH_SHORT).show();
           // } else if (TextUtils.isEmpty(supplierString)) {
                //Toast.makeText(this, R.string.supplier_validator, Toast.LENGTH_SHORT).show();
            } else {
                saveFlag = true;
                // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
                // and pass in the new ContentValues. Pass in null for the selection and selection args
                // because mCurrentProductUri will already identify the correct row in the database that
                // we want to modify.
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, getString(R.string.editor_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_update_product_successful),
                            Toast.LENGTH_SHORT).show();
                }
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

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                //Save product to database
                saveProduct();
                if (saveFlag) {
                    //Exit activity
                    finish();
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
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

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_PICTURE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            quantity = cursor.getInt(quantityColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            String uriString = cursor.getString(pictureColumnIndex);
            pictureUri = Uri.parse(uriString);

            // declare a stream to read the image data from the SD Card.
            InputStream inputStream;

            // we are getting an input stream, based on the URI of the image.
            try {
                inputStream = getContentResolver().openInputStream(pictureUri);

                // get a bitmap from the stream.
                Bitmap image = BitmapFactory.decodeStream(inputStream);

                // show the image to the user
                imgPicture.setImageBitmap(image);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // show a message to the user indictating that the image is unavailable.
                Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
            }

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mPriceEditText.setText(String.valueOf(price));
            mQuantityEditText.setText(String.valueOf(quantity));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mSupplierEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
    }

    public void displayQuantity(int number) {
        mQuantityEditText.setText("" + number);
    }

    public void increaseQuantityAmount(View view) {
        quantity += 1;
        displayQuantity(quantity);
    }

    public void decreaseQuantityAmount(View view) {
        if (quantity == 0) {
            Toast.makeText(this, R.string.quantity_warning, Toast.LENGTH_SHORT).show();
            return;
        }
        quantity -= 1;
        displayQuantity(quantity);
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
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    public void contactSupplier (View view){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}