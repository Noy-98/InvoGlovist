package com.itech.invoglovist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GuestScanProductsAdapter(
    private val context: Context,
    private var productList: MutableList<GuestScannedProductDBStructure>
) : RecyclerView.Adapter<GuestScanProductsAdapter.ViewHolder>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("GuestScanProductTbl")

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val product_name: TextView = itemView.findViewById(R.id.product_name)
        val product_quantity: TextView = itemView.findViewById(R.id.product_quantity)
        val price: TextView = itemView.findViewById(R.id.price)
        val delete_bttn: ImageView = itemView.findViewById(R.id.delete_bttn)
        val increase_quantity: ImageView = itemView.findViewById(R.id.increase)
        val decrease_quantity: ImageView = itemView.findViewById(R.id.decrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.products_guest_scan_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.product_name.text = product.product_name
        holder.product_quantity.text = product.product_quantity
        holder.price.text = product.product_price // Displaying the total price

        holder.delete_bttn.setOnClickListener {
            deleteProductFromFirebase(product, position)
        }

        holder.increase_quantity.setOnClickListener {
            val newQuantity = product.product_quantity.toInt() + 1
            updateProductQuantityAndPrice(product, newQuantity)
        }

        holder.decrease_quantity.setOnClickListener {
            val currentQuantity = product.product_quantity.toInt()
            if (currentQuantity > 1) {
                val newQuantity = currentQuantity - 1
                updateProductQuantityAndPrice(product, newQuantity)
            }
        }
    }

    private fun deleteProductFromFirebase(product: GuestScannedProductDBStructure, position: Int) {
        val userId = auth.currentUser?.uid
        if (userId != null && position >= 0 && position < productList.size) {
            GuestProductDelete().deleteProduct(userId, product.product_name) { isSuccess ->
                if (isSuccess) {
                    // Remove the product safely from the list
                    if (position < productList.size) {
                        productList.removeAt(position)
                        notifyItemRemoved(position)
                        // Only update the range if there are still items left
                        if (productList.isNotEmpty()) {
                            notifyItemRangeChanged(position, productList.size)
                        }
                    }
                } else {
                    // Handle deletion failure (optional)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    // Function to update product quantity and price in Firebase
    private fun updateProductQuantityAndPrice(product: GuestScannedProductDBStructure, newQuantity: Int) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val productRef = database.child(userId).child(product.product_name) // Assuming product_name is unique

            // Calculate the new total price based on the unit price
            val pricePerUnit = product.product_price.toDouble() // Assuming product_price is the unit price
            val newTotalPrice = pricePerUnit * newQuantity // Calculate the new total price

            val updates = mapOf(
                "product_quantity" to newQuantity.toString(),
                "total_price" to newTotalPrice.toString()
            )

            productRef.updateChildren(updates)
                .addOnSuccessListener {
                    // Update the product locally in the list
                    productList.find { it.product_name == product.product_name }?.let {
                        it.product_quantity = newQuantity.toString()
                        it.total_price = newTotalPrice.toString() // Update total_price, not product_price
                    }
                    notifyDataSetChanged() // Notify adapter of the update
                }
                .addOnFailureListener {
                    // Optionally handle failure, like showing a message to the user
                }
        }
    }
}
