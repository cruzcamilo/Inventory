package com.example.android.inventory;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.ProductContract.ProductEntry;

import static com.example.android.inventory.R.id.quantity;

public class ProductCursorAdapter extends CursorAdapter {

    private int productQuantity;
    private long productId;

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);

    }

     //Makes a new blank list item view. No data is set (or bound) to the views yet.

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantityTextView = (TextView) view.findViewById(quantity);


        // Find the columns of product attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int idColumnIndex = cursor.getColumnIndex(ProductEntry.PRODUCT_ID);

        // Read the product attributes from the Cursor for the current product
        String productName = cursor.getString(nameColumnIndex);
        String productPrice = cursor.getString(priceColumnIndex);
        productQuantity = cursor.getInt(quantityColumnIndex);
        productId = cursor.getLong(idColumnIndex);

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        String currency = context.getString(R.string.currency);
        priceTextView.setText(String.format(currency, productPrice));
        String available = context.getString(R.string.available_items);

        quantityTextView.setText(String.format(available, productQuantity));


        final Button sellButton = (Button) view.findViewById(R.id.sell_button);
        sellButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (productQuantity >= 1){
                    productQuantity -= 1;

                    ContentResolver resolver = view.getContext().getContentResolver();
                    Uri mCurrentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productId);
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, productQuantity);
                    resolver.update(
                            mCurrentProductUri,
                            values,
                            null,
                            null
                    );
                    context.getContentResolver().notifyChange(mCurrentProductUri, null);
                } else {
                    Toast.makeText(context, R.string.out_of_stock, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}