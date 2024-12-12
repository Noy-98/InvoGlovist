package com.itech.invoglovist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InvoiceAdapter(
    private val invoices: List<Invoice>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.invoice_cashier_scan_item, parent, false)
        return InvoiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        val invoice = invoices[position]
        holder.bind(invoice)
    }

    override fun getItemCount() = invoices.size

    inner class InvoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val invoiceId: TextView = itemView.findViewById(R.id.invoice_id)
        private val productList: RecyclerView = itemView.findViewById(R.id.productList)
        private val deleteButton: Button = itemView.findViewById(R.id.delete_button)

        fun bind(invoice: Invoice) {
            invoiceId.text = invoice.invoice_id


            // Set up the product list RecyclerView
            productList.layoutManager = LinearLayoutManager(itemView.context)
            productList.adapter = ProductAdapter(invoice.products) // Pass the product list to ProductAdapter

            deleteButton.setOnClickListener {
                onDeleteClick(invoice.invoice_id)
            }
        }
    }
}
