package com.itech.invoglovist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val products: List<Product>) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.products_cashier_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount() = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productId: TextView = itemView.findViewById(R.id.product_id)
        private val productName: TextView = itemView.findViewById(R.id.product_name)
        private val productQuantity: TextView = itemView.findViewById(R.id.product_quantity)
        private val price: TextView = itemView.findViewById(R.id.price)
        private val subTotal: TextView = itemView.findViewById(R.id.sub_total)

        fun bind(product: Product) {
            productId.text = product.product_id
            productName.text = product.product_name
            productQuantity.text = product.product_quantity
            price.text = product.product_price
            subTotal.text = product.total_price
        }
    }
}
