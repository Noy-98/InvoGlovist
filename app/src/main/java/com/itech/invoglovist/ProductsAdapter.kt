package com.itech.invoglovist

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductsAdapter(private val context: Context, private var productList: List<ProductsDBStructure>) :
    RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val product_name: TextView = itemView.findViewById(R.id.product_name)
        val viewDetailsBttn: AppCompatButton = itemView.findViewById(R.id.viewDetails_bttn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.products_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val products = productList[position]

        holder.product_name.text = products.product_name

        // Handle click on view details button
        holder.viewDetailsBttn.setOnClickListener {
            // Start a new activity to display patient details
            val intent = Intent(context, CashierProductsViewDetailsDashboard::class.java)
            intent.putExtra("product_id", products.product_id)
            intent.putExtra("productsData", products)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    fun filterList(filteredList: List<ProductsDBStructure>) {
        productList = filteredList.toMutableList()
        notifyDataSetChanged()
    }
}